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
        host="192.168.0.22",
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

class Robot(object):

  def __init__(self):
    self._contextHandler = ContextHandler(self)
    self.counter = random.randint(1, 2^16) 

  def _action(self, request):
    response = self._contextHandler.sendRequest(request)
    return response

  def _newAction(self):
    request = robotapi_pb2.RobotRequest()
    request.name = "katharosada"
    self.counter += 1
    request.key = self.counter
    return request

  def move(self, direction):
    request = self._newAction()
    request.action_request.move_direction = direction
    return self._action(request)

  def turn(self, direction):
    request = self._newAction()
    request.action_request.turn_direction = direction
    return self._action(request)

  def mine(self, direction):
    request = self._newAction()
    request.action_request.mine_direction = direction
    return self._action(request)

  def place(self, direction, material):
    request = self._newAction()
    request.action_request.place_direction = direction
    request.action_request.place_material.type = material
    return self._action(request)

  def getType(self, direction):
    request = self._newAction()
    request.read_request.identify_material.direction = direction
    return self._action(request).material_response

  def isSolid(self, direction):
    request = self._newAction()
    request.read_request.is_solid.direction = direction
    return self._action(request).boolean_response

  def _locate(self, entity):
    request = self._newAction()
    request.read_request.locate_entity = entity
    loc_proto = self._action(request).location_response.locations[0]
    return Location.fromProto(loc_proto.absolute_location)

  def getLocation(self):
    return self._locate(robotapi_pb2.RobotReadRequest.SELF)

  def getOwnerLocation(self):
    return self._locate(robotapi_pb2.RobotReadRequest.OWNER)

  def findMaterial(self, material):
    request = self._newAction()
    request.read_request.locate_material_nearby.type = material
    loc_proto_list = self._action(request).location_response.locations
    loc_list = [Location.fromProto(l.absolute_location) for l in loc_proto_list]
    return loc_list

  def findPath(self, target_location):
    my_loc = self.getLocation()
    request = self._newAction()
    request.read_request.locate_nonsolid_nearby = True
    loc_proto_list = self._action(request).location_response.locations
    loc_list = [Location.fromProto(l.absolute_location) for l in loc_proto_list]
    
    # Find point which is furthest from our current point and closest to the target
    best = None
    targetdist = target_location.distance(loc_list[0]) + 20
    for loc in loc_list:
      newdist = target_location.distance(loc)
      if newdist < targetdist and my_loc.distance(loc) == 1:
        best = loc
        targetdist = newdist
    if best == None:
      log.error("Follow bot can't move no free blocks!")
      best = loc_list[0]
    return my_loc.direction(best)

  def getInventory(self):
    request = self._newAction()
    request.read_request.get_inventory = True
    inv = self._action(request).inventory_response
    return [(mat.type, count) for mat, count in zip(inv.materials, inv.counts)]
  
class Location(object):

  @classmethod
  def fromProto(cls, location_proto):
    return Location(location_proto.x, location_proto.y, location_proto.z)
  
  def __init__(self, x, y, z):
    self.x = x
    self.y = y
    self.z = z

  def distance(self, other):
    return math.sqrt(
      (self.x - other.x) ** 2 +
      (self.y - other.y) ** 2 +
      (self.z - other.z) ** 2) 

  def direction(self, other):
    loc = [0, 0, 0]
    loc[0] = other.x - self.x
    loc[1] = other.y - self.y
    loc[2] = other.z - self.z
    m = max(list(map(abs, loc)))
    maxDir = 0
    if m in loc:
      maxDir = loc.index(m)
    else:
      maxDir = loc.index(-1 * m)
    # check up/down first
    if maxDir == 1:
      if loc[1] > 0:
        return Dir.UP
      return Dir.DOWN
    if maxDir == 0:
      if loc[0] > 0:
        return Dir.EAST
      return Dir.WEST
    if loc[2] > 0:
      return Dir.SOUTH
    return Dir.NORTH


class Dir:
  UP = robotapi_pb2.WorldLocation.UP
  DOWN = robotapi_pb2.WorldLocation.DOWN
  LEFT = robotapi_pb2.WorldLocation.LEFT
  RIGHT = robotapi_pb2.WorldLocation.RIGHT
  FORWARD = robotapi_pb2.WorldLocation.FORWARD
  BACKWARD = robotapi_pb2.WorldLocation.BACKWARD
  NORTH = robotapi_pb2.WorldLocation.NORTH
  SOUTH = robotapi_pb2.WorldLocation.SOUTH
  EAST = robotapi_pb2.WorldLocation.EAST
  WEST = robotapi_pb2.WorldLocation.WEST

