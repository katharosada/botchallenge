from client import *

robot = Robot()

block = robot.getType(Dir.DOWN)
print "Underneath me is: %s" % block

block = robot.getType(Dir.UP)
print "Above me is: %s" % block

block = robot.getType(Dir.FORWARD)
print "In front of me is: %s" % block
