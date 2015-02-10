"""
Classes which we expect the user to want to interact with directly, the robot
class handles the direct commands from the user and translates them into
API calls to the server.
"""
import random
import math

from .client import ContextHandler
from .api import robotapi_pb2
from .blocktypes import BlockType


class Robot(object):
    """Represents the robot itself, commands are sent to the server and the
         result is returned."""

    def __init__(self, owner_name, host, port=26656, context_handler=None):
        self.host = host
        self.owner_name = owner_name
        self.port = port
        self._context_handler = context_handler
        if not context_handler:
            self._context_handler = ContextHandler(host, port)
        self._counter = random.randint(1, 2 ** 16)

    def _action(self, request):
        """Send an action request to the server (via the context handler)."""
        response = self._context_handler.send_request(request)
        return response

    def _new_action(self):
        """Construct a new robot api request with the owner name, and counter
        filled in."""
        request = robotapi_pb2.RobotRequest()
        request.name = self.owner_name
        self._counter += 1
        request.key = self._counter
        return request

    def move(self, direction):
        """Move the robot one block in the given direction."""
        request = self._new_action()
        request.action_request.move_direction = direction.value
        return self._action(request).success

    def turn(self, direction):
        """Turn the robot to face the given direction."""
        request = self._new_action()
        request.action_request.turn_direction = direction.value
        return self._action(request).success

    def mine(self, direction):
        """Mine the adjacent block in the given direction and pick up the
        item that results from destrying that block."""
        request = self._new_action()
        request.action_request.mine_direction = direction.value
        return self._action(request).success

    def place(self, direction, blocktype):
        """Place a block next to the robot in the given direction, with the
        given type."""
        request = self._new_action()
        request.action_request.place_direction = direction.value
        request.action_request.place_material.type = blocktype.value
        return self._action(request).success

    def get_block_type(self, direction):
        """Find the type of the adjacent block in the given direction."""
        request = self._new_action()
        request.read_request.identify_material.direction = direction.value
        material_id = self._action(request).material_response.type
        if material_id in BlockType.value_map:
            return BlockType.value_map[material_id]
        # FIXME: logging does not exists here.
        # logging.warn("Unrecognized block type: %d", material_id)
        return None

    def is_block_solid(self, direction):
        """Check if the adjacent block in the given direction is one that the
        robot can walk through or not (returns a boolean)."""
        request = self._new_action()
        request.read_request.is_solid.direction = direction.value
        return self._action(request).boolean_response

    def _locate(self, entity):
        """Return the location of the entity type specified."""
        request = self._new_action()
        request.read_request.locate_entity = entity
        loc_proto = self._action(request).location_response.locations[0]
        return Location.from_proto(loc_proto.absolute_location)

    def get_location(self):
        """Returns the Location object for the location coordinates of the
        robot itself."""
        return self._locate(robotapi_pb2.RobotReadRequest.SELF)

    def get_owner_location(self):
        """Returns the Location object for the location coordinates of the
        robot's owner player."""
        return self._locate(robotapi_pb2.RobotReadRequest.OWNER)

    def find_type_nearby(self, blocktype):
        """Returns a list of the locations of blocks nearby that match the
        specified block type."""
        request = self._new_action()
        request.read_request.locate_material_nearby.type = blocktype.value
        loc_proto_list = (
            self._action(request).location_response.locations)
        loc_list = [
            Location.from_proto(l.absolute_location) for l in loc_proto_list]
        return loc_list

    def find_path(self, target_location):
        """Returns the direction to move in, to (hopefully) reach the target
        location (or None if the robot is completely stuck).

        This is a very basic pathfinding algorithm, it looks for which empty
        (non-solid) adjacent block is closest to the target location and
        returns the direction for that block."""
        my_loc = self.get_location()
        request = self._new_action()
        request.read_request.locate_nonsolid_nearby = True
        loc_proto_list = self._action(request).location_response.locations
        loc_list = [
            Location.from_proto(l.absolute_location) for l in loc_proto_list]

        # Find point which is furthest from our current point and closest to
        # the target
        best = None
        targetdist = target_location.distance(loc_list[0]) + 20
        for loc in loc_list:
            newdist = target_location.distance(loc)
            if newdist < targetdist and my_loc.distance(loc) == 1:
                best = loc
                targetdist = newdist
        return my_loc.direction(best)

    def get_inventory(self):
        """Returns a list of pairs (blocktype, count) for all the items in the
        robot's inventory."""
        request = self._new_action()
        request.read_request.get_inventory = True
        inv = self._action(request).inventory_response
        return [
            (self._material_to_block(mat), count)
            for mat, count in zip(inv.materials, inv.counts)]

    def _material_to_block(self, material):
        if material.type in BlockType.value_map:
            return BlockType.value_map[material.type]
        return None

    def message_owner(self, msg):
        request = self._new_action()
        request.action_request.chat_message = msg
        request.action_request.is_public_message = False
        return self._action(request).success

    def message_all(self, msg):
        request = self._new_action()
        request.action_request.chat_message = msg
        request.action_request.is_public_message = True
        return self._action(request).success


class Location(object):
    """A location in the Minecraft world as a set of 3D coordinates."""

    @classmethod
    def from_proto(cls, location_proto):
        """Internal use only. Used to convert the wireformat location into a
        more convenient Location object."""
        return Location(location_proto.x, location_proto.y, location_proto.z)

    def __init__(self, x_coord, y_coord, z_coord):
        self.x_coord = x_coord
        self.y_coord = y_coord
        self.z_coord = z_coord

    def __bool__(self):
        return (self.x_coord != 0 or self.y_coord != 0 or self.z_coord != 0)

    def __repr__(self):
        return "Location(x_coord={}, y_coord={}, z_coord={})".format(
            self.x_coord, self.y_coord, self.z_coord)

    def __eq__(self, other):
        if not isinstance(other, Location):
            return NotImplemented
        return (self.x_coord == other.x_coord and
                self.y_coord == other.y_coord and
                self.z_coord == other.z_coord)

    def __ne__(self, other):
        if not isinstance(other, Location):
            return NotImplemented
        return not (self == other)

    def __add__(self, other):
        if not isinstance(other, Location):
            raise TypeError("You can only add a location to an other Location")
        t_loc = self.clone()
        t_loc += other
        return t_loc

    def __iadd__(self, other):
        if not isinstance(other, Location):
            raise TypeError("You can only add a location to an other Location")
        self.x_coord += other.x_coord
        self.y_coord += other.y_coord
        self.z_coord += other.z_coord
        return self

    def __neg__(self):
        return Location(-self.x_coord, -self.y_coord, -self.z_coord)

    def __sub__(self, other):
        if not isinstance(other, Location):
            raise TypeError("You can only substract a location to an other Location")
        return self.__add__(-other)

    def __isub__(self, other):
        if not isinstance(other, Location):
            raise TypeError("You can only substract a location to an other Location")
        return self.__iadd__(-other)

    def __mul__(self, k):
        t_loc = self.clone()
        t_loc *= k
        return t_loc

    def __imul__(self, k):
        if isinstance(k, int):
            # Scalar multiplication
            self.x_coord *= k
            self.y_coord *= k
            self.z_coord *= k
        elif isinstance(k, Location):
            # Cross product
            u1, u2, u3 = self
            v1, v2, v3 = k
            self.x_coord = u2 * v3 - u3 * v2
            self.y_coord = u3 * v1 - u1 * v3
            self.z_coord = u1 * v2 - u2 * v1
        else:
            raise TypeError("Scalar multiplication or cross product, k has to be an integer or a location.")
        return self

    def __iter__(self):
        return iter((self.x_coord, self.y_coord, self.z_coord))

    def __abs__(self):
        return Location(abs(self.x_coord), abs(self.y_coord), abs(self.z_coord))

    def __round__(self, n=2):
        return Location(round(self.x_coord, n),
                        round(self.y_coord, n),
                        round(self.z_coord, n))

    def _vector_length(self):
        return math.sqrt(self.x_coord ** 2 +
                         self.y_coord ** 2 +
                         self.z_coord ** 2)

    def clone(self):
        return Location(self.x_coord, self.y_coord, self.z_coord)

    def distance(self, other):
        """Returns the distance between this location and the given other
        location."""
        if not isinstance(other, Location):
            raise TypeError("You can only get the distance to an other Location")
        return (self - other)._vector_length()

    def direction(self, other):
        """Find the direction (North, South, East or West) of the other
        location from this one."""
        if not isinstance(other, Location):
            raise TypeError("You can only get the direction to an other Location")
        if self == other:
            return None
        loc = other - self
        max_value = max(abs(loc))

        # check up/down first
        if max_value == abs(loc.y_coord):
            if loc.y_coord > 0:
                return Dir.UP
            return Dir.DOWN
        elif max_value == abs(loc.x_coord):
            if loc.x_coord > 0:
                return Dir.EAST
            return Dir.WEST
        else:
            if loc.z_coord > 0:
                return Dir.SOUTH
            return Dir.NORTH


class Dir:
    """A direction enum.

    This includes absolute compass directions, up, down and directions relative
    to the direction that the robot is facing (forward, backward, left, right)
    """

    def __init__(self, name, value):
        self.value = value
        self.name = name

    def __repr__(self):
        return "{} ({})".format(self.name, self.value)

    def __str__(self):
        return self.name

    def __eq__(self, other):
        if not isinstance(other, Dir):
            return NotImplemented
        return self.value == other.value

    def __ne__(self, other):
        if not isinstance(other, Dir):
            return NotImplemented
        return not (self.value == other.value)


def setup_dir():
    """Initalize the Dir enum with proto values."""
    value_map = {}
    for attr, value in robotapi_pb2.WorldLocation.__dict__.items():
        if attr.isupper() and type(value) == int:
            dir_obj = Dir(attr, value)
            setattr(Dir, attr, dir_obj)
            value_map[value] = dir_obj
    Dir.value_map = value_map

setup_dir()
