#!/bin/sh
protoc --python_out=../client/botchallenge/api *.proto
2to3 -w ../client/botchallenge/api/*.py
