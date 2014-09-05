"""
Example robot which finds trees and gathers wood.
"""
from client import *
from api.materials_pb2 import Material

robot = Robot()

TARGET = Material.LOG
TARGET2 = Material.LOG_2

locations = robot.findMaterial(TARGET)
print locations

while len(locations) > 0:
  location = locations[0]
  t = robot.getType(robot.getLocation().direction(location))
  while t.type != TARGET and t.type != TARGET2:
    robot.move(robot.findPath(location))
    robot.turn(robot.getLocation().direction(location))
    t = robot.getType(robot.getLocation().direction(location))
  robot.mine(robot.getLocation().direction(location))
  locations = robot.findMaterial(TARGET)




