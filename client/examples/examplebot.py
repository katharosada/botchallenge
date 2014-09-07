
from botchallenge import *

square = [Dir.LEFT, Dir.UP, Dir.RIGHT, Dir.DOWN]

def run(robot):
  while True:
    for dir in square:
      for i in range(4):
        success = robot.move(dir)
        if not success:
          print("Failed to move", dir)


run(Robot())
