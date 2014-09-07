This module is a fork of the Python module for Google Protocol Buffers
======================================================================
See: https://github.com/google/protobuf/

Why? Because the Google protobuf libraries don't support Python3, but it's
apparently very close. See: https://github.com/google/protobuf/issues/7

Why is this not a direct fork of the github repo? Because it's only a part and
I'm forking the compiled/built version anyway.

I've followed these instructions to build the necessary Python3 libraries.

Steps to reproduce:

 1. Download protobuf 2.6.0 (2.5.0 or earlier WILL NOT WORK)
    https://developers.google.com/protocol-buffers/docs/downloads

 2. Build and install the protoc compiler according to the README

 3. Build (but don't necessarily install) the Python libraries using
    a Python2 interpreter. (I used 2.7)

 4. Go into the google directory and run 2to3 on the following files:

protobuf/descriptor.py
protobuf/internal/cpp_message.py
protobuf/internal/decoder.py
protobuf/internal/python_message.py
protobuf/internal/type_checkers.py
protobuf/internal/message_factory_test.py
protobuf/internal/message_factory_python_test.py
protobuf/internal/message_python_test.py
protobuf/internal/message_test.py
protobuf/internal/reflection_test.py
protobuf/internal/test_util.py
protobuf/internal/text_format_test.py
protobuf/message_factory.py
protobuf/text_encoding.py
protobuf/text_format.py

A note on generating Python code for new protos
==============================================
There's no Python3 output option for the protoc compiler, but the 2.6.0 version
is compatible using 2to3. I've found that it only affects relative imports.
