"""
Mines a set of stairs 2 blocks wide going down in whatever direction the bot
is facing when it starts.
"""

from client import *

robot = Robot()

def mineUntilFree(direction):
  """Keeps mining until the mine action fails."""
  solid = True
  while solid:
    robot.mine(direction)
    solid = robot.isSolid(direction)

for i in range(10):
  # Clear space above and below
  mineUntilFree(Dir.DOWN)
  mineUntilFree(Dir.UP)
  
  # Mine and move right
  mineUntilFree(Dir.RIGHT)
  robot.move(Dir.RIGHT)

  # Clear space above and below
  mineUntilFree(Dir.DOWN)
  mineUntilFree(Dir.UP)
  
  # Move left + down + forward
  mineUntilFree(Dir.LEFT)
  robot.move(Dir.LEFT)
  mineUntilFree(Dir.DOWN)
  robot.move(Dir.DOWN)
  mineUntilFree(Dir.FORWARD)
  robot.move(Dir.FORWARD)



