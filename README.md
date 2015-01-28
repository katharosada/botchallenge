Bot Challenge
=============

NOTE: These instructions are for contributors, if you want to try it out and use the
Python API, see these docs:
[katharosada.github.io/botchallenge](http://katharosada.github.io/botchallenge)

Botchallenge is a Bukkit plugin which adds (on a separate network port) a programming
interface allowing the creation of a mining bot which can move independently around the
world and mine and place blocks.

This is designed for those who want an easy scriptable programming interface
for Minecraft, but doesn't give total world control to all players. The bots
are limited to similar rules as players. They can only see and affect the parts
of the world to which they have a line of sight with a limited distance.

This creates both an interesting programming challenge and a way to have
multiple people building automation for a shared world without risking
destroying each other's work or the entire world. :)

How to build the server plugin
==============================

**Warning: If you want to run a server you'll need a CraftBukkit or Spigot
server to run it on (or anything that supports Bukkit plugins). Due to
licensing concerns, these are currently hard to come by. I'd recommend getting
a working server before you bother trying to build this plugin.**

These instructions are for Ubuntu/Debian, but they should (in theory) translate
well enough for Mac if you get the Maven, Ant and Protocol Buffer tools
installed with Homebrew or package manager of your choice.

You need to first install Maven and the Google protobuffer compiler (the plugin
scripting api uses protocol buffers for server/client communication so both the
Python client and the server need the protocol buffer libraries and protoc
compiler to generate source.)

Install Maven, the protobuf compiler and openjdk:
```
sudo apt-get install maven ant protobuf-compiler openjdk-7-jdk
```


Checkout this git repo and cd to the top-level directory then run:
mvn install

In the targets directory there's a jar file called RoboMinionsPlugin, copy that
jar into the plugins/ directory of your CraftBukkit server.

If you don't have a CraftBukkit server, follow the instructions at:
[wiki.bukkit.org/Setting_up_a_server](http://wiki.bukkit.org/Setting_up_a_server)


Setting up an Eclipse development environment
=============================================

After following the build instructions above, in the repositories root directory, run:
```
mvn eclipse:eclipse 
```

This generates Eclipse workspace files which you can import into Eclipse.

You need the m2eclipse Maven plugin for Eclipse. Once that's installed, with
Eclipse go _File > Import..._ and select _'Existing Maven Projects'_ and point the
project root directory to the root directory of your git repo.


Building and running the Python client
======================================

Again you need the protocol buffer compiler:
```
sudo apt-get install protobuf-compiler
```

OR (for Macs with homebrew):
```
brew install protobuf
```

Check that it's installed with:
```
which protoc
```

For the Python libraries, you need greenlet, twisted and the protocol buffer
Python lib, you can install these with:
```
pip install greenlet protobuf twisted
```

In the proto directory, generate the Python protobuffer source with:
```
./gen_python.sh
```

Go to the client directory, and the modules are there for you to use (locally importing) if you like.

To install (again, use python3):
```
python setup.py build
python setup.py install
```

