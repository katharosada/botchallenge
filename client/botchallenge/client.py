from .api import robotapi_pb2
from .api import materials_pb2

import struct
import sys
import logging
import random
import math
import asyncio
from greenlet import greenlet

logging.basicConfig(level=logging.DEBUG)

loop = asyncio.get_event_loop()

class RobotClientProtocol(asyncio.Protocol):

  def __init__(self, contextHandler):
    self.contextHandler = contextHandler
    self.timeout = None
    self.lastSentRequest = None
    self.connectionTimeout = None
    self.response_data = None

  def data_received(self, data):
    if not self.response_data:
      self.response_data = data
    else:
      self.response_data += data

    expect_len = struct.unpack(b"!L", self.response_data[:4])[0]
    if len(self.response_data) - 4 == expect_len:
      payload = self.response_data[4:]
      self.response_data = None
      self.stringReceived(payload)
  
  def stringReceived(self, msg):
    # We got a response, send it to the robot logic and await another command.
    if self.timeout:
      self.timeout.cancel()
      self.timeout = None
    response = robotapi_pb2.RobotResponse()
    response.ParseFromString(msg)
    logging.debug("Received response:\n<\n%s>", response)
    if self.lastSentRequest and self.lastSentRequest.key == response.key:
      self.contextHandler.handleResponse(response)
    else:
      logging.info("Recieved extra response for a retry. Response was for key: %s but I expected key %s", self.lastSentRequest.key, response.key)

  def handleTimeout(self):
    logging.info("TIMEOUT happened. I'm gonna retry that request.")
    self.sendRequest(self.lastSentRequest)

  def sendString(self, payload):
    size = struct.pack(b'!L', len(payload))
    self.transport.write(size + payload)

  def sendRequest(self, request):
    logging.debug("Sending request:\n<\n%s>", request)
    self.lastSentRequest = request
    self.sendString(request.SerializeToString())
    # TODO: add a timeout.

  def connection_made(self, transport):
    logging.info("Connected to server.")
    self.transport = transport
    self.contextHandler.triggerFirstRequest(self)

  def connection_lost(self, reason):
    logging.info("Connection to server lost. Reason: " + str(reason))


class ContextHandler(object):

  def __init__(self, robot):
    self.robot = robot
    self.network_greenlet = None
    self.robot_greenlet = greenlet.getcurrent()
    self.network_greenlet = greenlet(self.run_event_loop)
    self.network_greenlet.switch()

  def errback(error, extra):
    logging.error('Error setting up the protocol: %s (%s)', error, extra)

  def triggerFirstRequest(self, protocol):
    logging.info("Connected. Waiting for first robot request...")
    self.protocol = protocol
    # Switch to the robot execution context until it returns an request:
    request = self.robot_greenlet.switch(self.robot)
    # We've got our first command:
    self.protocol.sendRequest(request)

  def run_event_loop(self):
    self.protocol = RobotClientProtocol(self)
    asyncio.async(loop.create_connection(
        lambda: self.protocol,
        host="130.211.85.227",
        port=26656))

    # Main asyncio event loop running in the asyncio_greenlet
    loop.run_forever()
    # The loop was terminated, close the things.
    loop.close()

  def sendRequest(self, request):
    if self.network_greenlet.dead:
      sys.exit("Goodbye.")
    response = self.network_greenlet.switch(request)
    return response

  def handleResponse(self, response):
    # give the response to the robot context, and get the next request
    request = self.robot_greenlet.switch(response)
    if request:
      # We were given back a new request, let's send it.
      self.protocol.sendRequest(request)
    else:
      # There are no more commands, kill the event loop.
      logging.INFO("Received null request, ending.")
      loop.stop()

