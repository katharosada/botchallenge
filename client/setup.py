"""
Setup script for the minecraft robominions module.
"""

from setuptools import setup, find_packages

DESC = """\
A Python API for interacting with a Minecraft server plugin to control a bot."""


LONG_DESC = """\
Described at the url listed is a Minecraft server plugin for adding a network
interface for each player to control their own bot figure in the Minecraft
world.

This is the Python API for that network interface, it allows a user to script
their own Minecraft bots which can move around independently (even when the
player is disconnected), search for blocks of specific types, mine blocks and
place blocks.
"""

setup(
    name="botchallenge",
    version="1.1",
    packages=find_packages(),
    install_requires=['greenlet'],
    author="Katie Bell",
    author_email="katie@katharos.id.au",
    description=DESC,
    long_description=LONG_DESC,
    license="MIT",
    keywords="minecraft bot",
    url="https://github.com/katharosada/botchallenge",
    classifiers=[
        'Development Status :: 3 - Alpha',
        'Intended Audience :: Developers',
        'Intended Audience :: Education',
        'License :: OSI Approved :: MIT License',
        'Natural Language :: English',
        'Programming Language :: Python :: 3',
        'Programming Language :: Python :: 3.3',
        'Programming Language :: Python :: 3.4',
        'Topic :: Games/Entertainment',
    ],


)
