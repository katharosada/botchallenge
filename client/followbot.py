
from client import *

robot = Robot()

while True:
  me = robot.getLocation()
  owner = robot.getOwnerLocation()

  print me.distance(owner)
  if me.distance(owner) > 4:
    d = robot.findPath(owner)
    robot.turn(d)
    robot.move(d)

