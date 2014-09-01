
from client import *

robot = Robot()

while True:
  me = robot.getLocation()
  owner = robot.getOwnerLocation()

  print me.distance(owner)
  if me.distance(owner) > 4:
    robot.turn(me.direction(owner))
    robot.move(me.direction(owner))

