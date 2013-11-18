Apache Sling Commons Log
========================

The "log" project packages the [Logback][2] library to manage logging
in OSGi environment. It provide some useful extension to the default
Logback feature set to enable better integration with OSGi. The SLF4j
API bundle must be installed along with this bundle to provide full SLF4J
logging support.
  
The Logging bundle should be installed as one of the first modules in
the OSGi framework and - provided the framework supports start levels -
be set to start at start level 1. This ensures the Logging bundle is
loaded as early as possible thus providing services to the framework
and preparing logging.

For more details refer to the [Logging Documentation][1]

Getting Started
===============

You can compile and package the jar using the following command:

    mvn package -Pide,coverage

It would build the module and also produce a test coverage report also
prepare bundle jar which is suitable to be used to run integration test
from within IDE.

[1]: http://sling.apache.org/documentation/development/logging.html
[2]: http://logback.qos.ch/
