
from client import *

robot = Robot()
i = 1

square = [Dir.LEFT, Dir.UP, Dir.RIGHT, Dir.DOWN]

while True:
  for dir in square:
    for i in range(4):
      success = robot.move(dir)
      if not success:
        print("Failed to move", dir)



