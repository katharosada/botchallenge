from botchallenge import *

USERNAME = "" # Put your minecraft username here
SERVER = "" # Put the address of the minecraft server here

robot = Robot(USERNAME, SERVER)

block = robot.getType(Dir.DOWN)
print("Underneath me is: %s" % block)

block = robot.getType(Dir.UP)
print("Above me is: %s" % block)

block = robot.getType(Dir.FORWARD)
print("In front of me is: %s" % block)
