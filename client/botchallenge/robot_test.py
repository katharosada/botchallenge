"""Unit tests for the robot module."""

import pytest
from .robot import Robot, Dir, Location
from .api import robotapi_pb2
from .blocktypes import BlockType


class MockContextHandler(object):
    """A mock for the context handler, simply returns pre-prepared responses
    straight away."""

    def __init__(self):
        self.expected_requests = []

    def expect(self, request, response):
        self.expected_requests.append((request, response))

    def send_request(self, request):
        (expected_request, response) = self.expected_requests.pop(0)
        request.key = 1
        assert request == expected_request
        return response
    
    def validate(self):
        assert len(self.expected_requests) == 0

class TestRobot(object):
    """Unit tests for the robot class itself."""

    def setup(self):
        self.mock_context = MockContextHandler()
        self.robot = Robot("playername", "host",
                           context_handler=self.mock_context)

    def get_request(self):
        request = robotapi_pb2.RobotRequest()
        request.name = "playername"
        request.key = 1
        return request
    
    def get_response(self):
        response = robotapi_pb2.RobotResponse()
        response.key = 1
        response.success = True
        return response

    def test_move(self):
        request = self.get_request()
        request.action_request.move_direction = Dir.LEFT.value
        response = self.get_response()
        self.mock_context.expect(request, response)
        result = self.robot.move(Dir.LEFT)
        assert result == True
        self.mock_context.validate()

    def test_turn(self):
        request = self.get_request()
        request.action_request.turn_direction = Dir.RIGHT.value
        response = self.get_response()
        self.mock_context.expect(request, response)
        result = self.robot.turn(Dir.RIGHT)
        assert result == True
        self.mock_context.validate()

    def test_place(self):
        request = self.get_request()
        request.action_request.place_direction = Dir.RIGHT.value
        request.action_request.place_material.type = BlockType.COBBLESTONE.value
        response = self.get_response()
        self.mock_context.expect(request, response)
        result = self.robot.place(Dir.RIGHT, BlockType.COBBLESTONE)
        assert result == True
        self.mock_context.validate()

    def test_mine(self):
        request = self.get_request()
        request.action_request.mine_direction = Dir.SOUTH.value
        response = self.get_response()
        self.mock_context.expect(request, response)
        result = self.robot.mine(Dir.SOUTH)
        assert result == True
        self.mock_context.validate()

    def test_get_block_type(self):
        request = self.get_request()
        request.read_request.identify_material.direction = Dir.EAST.value
        response = self.get_response()
        response.material_response.type = BlockType.STONE.value
        self.mock_context.expect(request, response)
        result = self.robot.get_block_type(Dir.EAST)
        assert result == BlockType.STONE
        self.mock_context.validate()

    def test_is_block_solid(self):
        request = self.get_request()
        request.read_request.is_solid.direction = Dir.FORWARD.value
        response = self.get_response()
        response.boolean_response = False
        self.mock_context.expect(request, response)
        result = self.robot.is_block_solid(Dir.FORWARD)
        assert result == False
        self.mock_context.validate()

    def test_get_location(self):
        request = self.get_request()
        request.read_request.locate_entity = robotapi_pb2.RobotReadRequest.SELF
        response = self.get_response()
        proto_location = response.location_response.locations.add()
        proto_location.absolute_location.x = 20
        proto_location.absolute_location.y = -30
        proto_location.absolute_location.z = 4
        self.mock_context.expect(request, response)
        result = self.robot.get_location()
        assert result == Location(20, -30, 4)
        self.mock_context.validate()

    def test_get_owner_location(self):
        request = self.get_request()
        request.read_request.locate_entity = (
                robotapi_pb2.RobotReadRequest.OWNER)
        response = self.get_response()
        proto_location = response.location_response.locations.add()
        proto_location.absolute_location.x = -200
        proto_location.absolute_location.y = 46
        proto_location.absolute_location.z = 10
        self.mock_context.expect(request, response)
        result = self.robot.get_owner_location()
        assert result == Location(-200, 46, 10)
        self.mock_context.validate()

    def test_find_path(self):
        request1 = self.get_request()
        request1.read_request.locate_entity = robotapi_pb2.RobotReadRequest.SELF
        response1 = self.get_response()
        own_loc = response1.location_response.locations.add().absolute_location
        own_loc.x, own_loc.y, own_loc.z = 10, 20, 30
        self.mock_context.expect(request1, response1)

        request2 = self.get_request()
        request2.read_request.locate_nonsolid_nearby = True
        response2 = self.get_response()

        loc1 = response2.location_response.locations.add().absolute_location
        loc1.x, loc1.y, loc1.z = 11, 20, 30

        loc2 = response2.location_response.locations.add().absolute_location
        loc2.x, loc2.y, loc2.z = 10, 21, 30

        loc3 = response2.location_response.locations.add().absolute_location
        loc3.x, loc3.y, loc3.z = 10, 20, 31
        self.mock_context.expect(request2, response2)

        direction = self.robot.find_path(Location(15, 30, 30))
        assert direction == Dir.UP
        self.mock_context.validate()

    def test_get_inventory(self):
        request = self.get_request()
        request.read_request.get_inventory = True
        response = self.get_response()
        response.inventory_response.materials.add().type = BlockType.DIRT.value
        response.inventory_response.materials.add().type = (
                BlockType.COBBLESTONE.value)
        response.inventory_response.materials.add().type = BlockType.WOOL.value
        response.inventory_response.counts.append(64)
        response.inventory_response.counts.append(64)
        response.inventory_response.counts.append(5)

        self.mock_context.expect(request, response)
        inv = self.robot.get_inventory()
        assert (BlockType.DIRT, 64) in inv
        assert (BlockType.COBBLESTONE, 64) in inv
        assert (BlockType.WOOL, 5) in inv
        assert len(inv) == 3
        self.mock_context.validate()


class TestLocation(object):
    """Tests for the Location type."""

    def test_location_distance(self):
        loc1 = Location(23, 45, -128)
        loc2 = Location(23, 45, 12)
        dist = loc1.distance(loc2)
        assert dist == loc2.distance(loc1)
        assert dist == 140

    def test_location_distance(self):
        loc1 = Location(16, -4, 25)
        loc2 = Location(-23, 45, 12)
        dist = loc1.distance(loc2)
        assert dist == loc2.distance(loc1)
        assert abs(dist - 63.96092) < 0.00001

    def test_location_from_proto(self):
        proto = robotapi_pb2.Coordinate()
        proto.x = 5
        proto.y = -18
        proto.z = 89
        loc = Location.from_proto(proto)
        assert loc.x_coord == 5
        assert loc.y_coord == -18
        assert loc.z_coord == 89

    def test_direction_northsouth(self):
        loc1 = Location(10, 20, -30)
        loc2 = Location(-8, 12, 5)
        direction = loc1.direction(loc2)
        assert direction == Dir.SOUTH
        direction = loc2.direction(loc1)
        assert direction == Dir.NORTH

    def test_direction_updown(self):
        loc1 = Location(0, 5, 5)
        loc2 = Location(0, -5, 5)
        direction = loc1.direction(loc2)
        assert direction == Dir.DOWN
        assert loc2.direction(loc1) == Dir.UP

    def test_direction_eastwest(self):
        loc1 = Location(10, 4, 4)
        loc2 = Location(0, -5, 5)
        direction = loc1.direction(loc2)
        assert direction == Dir.WEST
        assert loc2.direction(loc1) == Dir.EAST

class TestDir(object):
    """Tests for the direction enum."""

    def test_directions(self):
        assert Dir.value_map[0] == Dir.UP
        assert Dir.value_map[8] == Dir.NORTH
        assert Dir.value_map[3] == Dir.RIGHT
    









