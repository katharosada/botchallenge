"""
Example robot which finds trees and gathers wood.
"""
from botchallenge import *
from botchallenge.api.materials_pb2 import Material

USERNAME = "" # Put your minecraft username here
SERVER = "" # Put the address of the minecraft server here

robot = Robot(USERNAME, SERVER)

TARGET = Material.LOG

locations = robot.find_type_nearby(TARGET)
print(locations)

while len(locations) > 0:
    location = locations[0]
    t = robot.get_block_type(robot.get_location().direction(location))
    while t.type != TARGET:
        robot.move(robot.find_path(location))
        robot.turn(robot.get_location().direction(location))
        t = robot.get_block_type(robot.get_location().direction(location))
    robot.mine(robot.get_location().direction(location))
    locations = robot.find_type_nearby(TARGET)




