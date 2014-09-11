"""
Setup script for the minecraft robominions module.
"""

from setuptools import setup, find_packages

DESC = """\
A Python API for interacting with a Minecraft server plugin to control a bot."""


setup(
    name="botchallenge",
    version="0.1",
    packages=find_packages(),
    install_requires=['greenlet'],
    author="Katie Bell",
    author_email="katie@katharos.id.au",
    description=DESC,
    license="MIT",
    keywords="minecraft bot",
    url="https://github.com/katharosada/botchallenge",
)
