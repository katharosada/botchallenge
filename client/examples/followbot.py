
from botchallenge import *

USERNAME = "" # Put your minecraft username here
SERVER = "" # Put the address of the minecraft server here

robot = Robot(USERNAME, SERVER)

while True:
    me = robot.getLocation()
    owner = robot.getOwnerLocation()

    print(me.distance(owner))
    if me.distance(owner) > 4:
        d = robot.findPath(owner)
        robot.turn(d)
        robot.move(d)

