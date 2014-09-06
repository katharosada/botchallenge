#!/bin/sh
protoc --python_out=../client/src/api *.proto
2to3 -w ../client/src/api/*.py
