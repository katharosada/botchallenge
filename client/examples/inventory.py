"""
Builds a simple dirt hut to shelter you from the enemies at night.
"""

from botchallenge import *


USERNAME = "" # Put your minecraft username here
SERVER = "" # Put the address of the minecraft server here

robot = Robot(USERNAME, SERVER)


inventory = robot.get_inventory()
print("My inventory contains:")
for block_type, count in inventory:
    print(count, "of", block_type)



