MAINTAINER dev@sling.apache.org
FROM java:8
COPY org.apache.sling.launchpad-8-SNAPSHOT.jar /opt/sling/
WORKDIR /opt/sling/
EXPOSE 8080
VOLUME /opt/sling/sling
ENV JAVA_OPTS -Xmx512m
ENV SLING_OPTS ''
CMD exec java $JAVA_OPTS -jar org.apache.sling.launchpad.jar $SLING_OPTS
