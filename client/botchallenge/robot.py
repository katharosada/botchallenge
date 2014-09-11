"""
Classes which we expect the user to want to interact with directly, the robot
class handles the direct commands from the user and translates them into
API calls to the server.
"""
import random
import math

from .client import ContextHandler
from .api import robotapi_pb2

class Robot(object):
  """Represents the robot itself, commands are sent to the server and the
     result is returned."""

  def __init__(self, owner_name, host, port=26656):
    self.host = host
    self.owner_name = owner_name
    self.port = port
    self._contextHandler = ContextHandler(self)
    self._counter = random.randint(1, 2^16) 

  def _action(self, request):
    response = self._contextHandler.sendRequest(request)
    return response

  def _newAction(self):
    request = robotapi_pb2.RobotRequest()
    request.name = self.owner_name
    self._counter += 1
    request.key = self._counter
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
    return Location._fromProto(loc_proto.absolute_location)

  def getLocation(self):
    return self._locate(robotapi_pb2.RobotReadRequest.SELF)

  def getOwnerLocation(self):
    return self._locate(robotapi_pb2.RobotReadRequest.OWNER)

  def findMaterial(self, material):
    request = self._newAction()
    request.read_request.locate_material_nearby.type = material
    loc_proto_list = self._action(request).location_response.locations
    loc_list = [Location._fromProto(l.absolute_location) for l in loc_proto_list]
    return loc_list

  def findPath(self, target_location):
    my_loc = self.getLocation()
    request = self._newAction()
    request.read_request.locate_nonsolid_nearby = True
    loc_proto_list = self._action(request).location_response.locations
    loc_list = [Location._fromProto(l.absolute_location) for l in loc_proto_list]
    
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
  """A location in the Minecraft world as a set of 3D coordinates."""

  @classmethod
  def _fromProto(cls, location_proto):
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







