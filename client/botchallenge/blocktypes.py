"""Defines the BlockType enum-ish class which contains all the Material types
that the API supports."""

from .api.materials_pb2 import Material

class BlockType(object):
    """The type of a minecraft block or item."""

    def __init__(self, name, value):
        self.value = value
        self.name = name

    def __repr__(self):
        return "{} ({})".format(self.name, self.value)

    def __str__(self):
        return self.name

    def get_value(self):
        """Get the integer id of this item (same as offical Minecraft material
        ids.)"""
        return self.value

    def __eq__(self, other):
        return self.value == other.value


def init():
    """Set up all the blocktypes which are available in the API proto."""
    value_map = {}
    for attr, value in Material.__dict__.items():
        if attr.isupper() and type(value) == int:
            block_type = BlockType(attr, value)
            setattr(BlockType, attr, block_type)
            value_map[value] = block_type
    BlockType.value_map = value_map

init()


