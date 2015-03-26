Apache Sling debian/ubuntu packaging
====================================

This is a work in progress .deb packaging for Sling.
It provides basic init.d start/stop logrotate & sample sling config. The goal is to provide a generic yet configurable sling package to eliminate the need to build sling for in-house deployments.

To run this, and install using dpkg/apt commands, review /etc/default.sling
config and run
	"service sling start"

Components used are:
* [Sling crankstart](https://github.com/apache/sling/tree/trunk/contrib/crankstart)
* [Sling-s3](https://github.com/apache/sling/tree/trunk/contrib/sling-s3)

Supported run modes are:

    crank           - tar files for nodes and data
    crank-s3        - tar files for nodes, s3 for data
    crank-mongo     - mongo for nodes and data
    crank-s3-mongo  - mongo for nodes, s3 for data

Each of these are supported by building crank files using the sling-s3 module to aggregate configuration templates into usable configurations.

* SLING_EXEC=/opt/sling - Location for all binaries & scripts.
* SLING_CFG=/etc/sling - Tree of config file templates from which final.crantstart files are built. Editing any of these config templates triggers a rebuild of the target/crank* files.
* SLING_DEFAULTS=/etc/default/sling - Defaults file for location & path setup and to override any of the crank settings.
* SLING_DATA=/var/lib/sling - Local sling data dir.
* SLING_LOG_DIR=/var/log/sling - Sling log data.

TODO:
* Support clustered configurations.


