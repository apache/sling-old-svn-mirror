Apache Sling Launchpad Dockerfile

The Launchpad Dockerfile directory allows building a Dockerfile based on
a previously existing launchpad jar file.

Getting Started
===============

Building the Docker Image required an installation of Docker
(https://www.docker.com/) . It has been tested on version 1.8.2, but it
is expected that older versions will work.

To build an image, first copy a launchpad jar file and rename it to
org.apache.sling.launchpad.jar. Then run the docker command
  
    sudo docker build -t sling .

When running the Docker image it is possible to customise the JVM
parameters, as well as the options passed to Sling, for instance:

   sudo docker run -e SLING_OPTS='-Dsling.run.modes=oak' \
      -e JAVA_OPTS='-Xmx256m' -P -v /data/sling:/opt/sling/sling \
      -d --name sling-8-testing sling

