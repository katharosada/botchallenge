"""
Client code for handling connecting to the Minecraft server and switching
between greenlet execution contexts.
"""

import struct
import sys
import logging
import asyncio
from greenlet import greenlet

from .api import robotapi_pb2


class RobotClientProtocol(asyncio.Protocol):
    """Protocol for sending requests to the Robot control API.

    Includes adding (and checking) the 4-byte length integer at the start of
    every packet and serializing/deserializing the protobuffer request and
    response objects."""

    def __init__(self, context_handler):
        self.context_handler = context_handler
        self.last_sent_request = None
        self.response_data = None
        self.transport = None

    def data_received(self, data):
        """Overrides Protocol method, to handle incoming data."""
        if not self.response_data:
            self.response_data = data
        else:
            self.response_data += data

        # First 4 bytes are the length of the response.
        expect_len = struct.unpack(b"!L", self.response_data[:4])[0]
        if len(self.response_data) - 4 == expect_len:
            payload = self.response_data[4:]
            self.response_data = None
            self.process_payload(payload)

    def process_payload(self, msg):
        """Processes a fully receieved payload proto."""
        # We got a response, send it to the robot logic and
        # await another command.
        response = robotapi_pb2.RobotResponse()
        response.ParseFromString(msg)
        logging.debug("Received response:\n<\n%s>", response)
        if (self.last_sent_request and
                self.last_sent_request.key == response.key):
            self.context_handler.handle_response(response)
        else:
            logging.warn("Recieved extra response for a retry. Response was "
                         "for key: %s but I expected key %s.",
                         response.key, self.last_sent_request.key)

    def send_request(self, request):
        """Serialize and send the given proto request object."""
        logging.debug("Sending request:\n<\n%s>", request)
        self.last_sent_request = request
        payload = request.SerializeToString()
        size = struct.pack(b'!L', len(payload))
        self.transport.write(size + payload)
        # TODO: add a timeout.

    def connection_made(self, transport):
        """Overrides Protocol method, called when the connection to the server
        is established."""
        logging.info("Connected to server.")
        self.transport = transport
        self.context_handler.trigger_first_request(self)

    def connection_lost(self, reason):
        """Overrides Protocol method, called when the connection to the server
        is lost."""
        logging.warn("Connection to server lost. Reason: " + str(reason))


class ContextHandler(object):
    """Class that controls switching between two execution contexts (using
    greenlets).

    When waiting for the server to connect, it starts off in the network
    greenlet, when the server connects it switches back to the robot
    greenlet for it to work out what the first network request will be. When
    it sends a request, we switch back to the network greenlet to send it
    and asynchronously wait for the response. When the response is receieved
    we switch back to the robot greenlet to await the next request."""

    def __init__(self, host, port):
        self.loop = asyncio.get_event_loop()
        self.host = host
        self.port = port
        self.network_greenlet = None
        self.protocol = None

        # Grab the current execution context as the robot greenlet
        self.robot_greenlet = greenlet.getcurrent()
        # Prepare to run the asyncio event loop in the network greenlet
        self.network_greenlet = greenlet(self._run_event_loop)
        # Start the asyncio event loop and switch to it (to connect)
        self.network_greenlet.switch()

    def trigger_first_request(self, protocol):
        """Once the server connection is made, this is called to swtich to the
        robot greenelt to get the first request."""
        logging.debug("Connected. Waiting for first robot request...")
        self.protocol = protocol
        # Switch to the robot execution context until it returns an request:
        request = self.robot_greenlet.switch()
        # We've got our first command:
        self.protocol.send_request(request)

    def _run_event_loop(self):
        """Start the main asyncio event loop (must be in the network greenlet).
        """
        self.protocol = RobotClientProtocol(self)
        # Connect to the server
        asyncio.async(
            self.loop.create_connection(
                lambda: self.protocol,
                host=self.host,
                port=self.port))

        # Main asyncio event loop running in the asyncio_greenlet
        self.loop.run_forever()
        # The loop was terminated, close the things.
        self.loop.close()

    def send_request(self, request):
        """Switch to the network context and send the given proto request
        object."""
        if self.network_greenlet.dead:
            sys.exit("Goodbye.")
        response = self.network_greenlet.switch(request)
        return response

    def handle_response(self, response):
        """Accept the response proto object and switch back to the robot
        greenlet to process it before returning it to the user's code."""
        # give the response to the robot context, and get the next request
        request = self.robot_greenlet.switch(response)
        if request:
            # We were given back a new request, let's send it.
            self.protocol.send_request(request)
        else:
            # There are no more commands, kill the event loop.
            logging.info("Received null request, ending.")
            self.loop.stop()

