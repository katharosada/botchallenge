"""
Builds a simple dirt hut to shelter you from the enemies at night.
"""

from botchallenge import *


USERNAME = "" # Put your minecraft username here
SERVER = "" # Put the address of the minecraft server here

robot = Robot(USERNAME, SERVER)


hut_layout = """
 xxxxx 
x     x
x     x
x     x
x     x
x     x
 xx xx 
"""
hut_layout_2 = """
 xxxxx 
x     x
x     x
x     x
x     x
x     x
 xxxxx 
"""
hut_layout_roof = """
  xxx  
 xxxxx 
xxxxxxx
xxxxxxx
xxxxxxx
 xxxxx 
  xxx  
"""



def build_layer(layer):
    # Move up one so we're placing the blocks downward
    robot.move(Dir.UP)
    lines = layer.split("\n")
    num_lines = len(lines)
    print("There are", num_lines, "lines")
    for line in lines:
        line_len = len(line)
        for char in line:
            if char == "x":
                robot.place(Dir.DOWN, BlockType.COBBLESTONE)
            robot.move(Dir.LEFT)
        # End of the line, go back to the beginning for the next line
        for i in range(line_len):
            robot.move(Dir.RIGHT)
        robot.move(Dir.FORWARD)
    # End of the layer, go back to the start position
    for l in range(num_lines):
        robot.move(Dir.BACKWARD)



for layer in range(2):
    build_layer(hut_layout)

build_layer(hut_layout_2)
build_layer(hut_layout_roof)





