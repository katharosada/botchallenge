
from botchallenge import *

square = [Dir.LEFT, Dir.UP, Dir.RIGHT, Dir.DOWN]

USERNAME = "" # Put your minecraft username here
SERVER = "" # Put the address of the minecraft server here

robot = Robot(USERNAME, SERVER)

while True:
    for dir in square:
        for i in range(4):
            success = robot.move(dir)
            if not success:
                print("Failed to move", dir)


