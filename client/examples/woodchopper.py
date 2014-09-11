"""
Example robot which finds trees and gathers wood.
"""
from botchallenge import *

USERNAME = "" # Put your minecraft username here
SERVER = "" # Put the address of the minecraft server here

robot = Robot(USERNAME, SERVER)

TARGET = BlockType.LOG

locations = robot.find_type_nearby(TARGET)
print(locations)

while len(locations) > 0:
    location = locations[0]
    block_type = robot.get_block_type(robot.get_location().direction(location))
    while block_type != TARGET:
        robot.move(robot.find_path(location))
        robot.turn(robot.get_location().direction(location))
        block_type = robot.get_block_type(robot.get_location().direction(location))
    robot.mine(robot.get_location().direction(location))
    locations = robot.find_type_nearby(TARGET)




