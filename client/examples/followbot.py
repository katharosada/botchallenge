
from botchallenge import *

USERNAME = "" # Put your minecraft username here
SERVER = "" # Put the address of the minecraft server here

robot = Robot(USERNAME, SERVER)

while True:
    me = robot.get_location()
    owner = robot.get_owner_location()

    print(me.distance(owner))
    if me.distance(owner) > 4:
        d = robot.find_path(owner)
        robot.turn(d)
        robot.move(d)

