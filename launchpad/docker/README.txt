Apache Sling Launchpad - Docker integration

The Launchpad Docker integration project is a helper aimed to make it
simple to build an 'official' Sling docker image.

The Dockerfile expects a org.apache.sling.launchpad.jar file present in the
root directory.

Building a Docker image
---------------------------------------------

$ sudo docker build -t sling .

Running a standalone Sling instance
---------------------------------------------

To launch a docker instance named 'my-sling-container' bound to 
port 8080 on the local machine, and with the /opt/sling/sling volume
mounted at /srv/docker/sling in the local machine, execute:

$ sudo docker run -ti -p 8080:8080 -v /srv/docker/sling:/opt/sling/sling \
    --name my-sling-container sling


Running a Sling instance connected to MongoDB
---------------------------------------------

There is a docker-compose-mongodb.yml file supplied which shows how to run
the Sling launchpad in a docker container linked to a mongo one. To launch
both containers, execute:

$ sudo docker-compose -f docker-compose-mongo.yml up
