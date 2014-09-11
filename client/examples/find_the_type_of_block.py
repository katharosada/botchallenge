from botchallenge import *

USERNAME = "" # Put your minecraft username here
SERVER = "" # Put the address of the minecraft server here

robot = Robot(USERNAME, SERVER)

block = robot.get_block_type(Dir.DOWN)
print("Underneath me is: %s" % block)

block = robot.get_block_type(Dir.UP)
print("Above me is: %s" % block)

block = robot.get_block_type(Dir.FORWARD)
print("In front of me is: %s" % block)
