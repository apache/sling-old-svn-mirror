FROM openjdk:8-jre-alpine
MAINTAINER dev@sling.apache.org

# build on top of the released Launchpad
RUN mkdir -p /opt/sling
RUN wget -q http://repo1.maven.org/maven2/org/apache/sling/org.apache.sling.launchpad/9/org.apache.sling.launchpad-9.jar -O /opt/sling/org.apache.sling.launchpad.jar
WORKDIR /opt/sling/
EXPOSE 8080
VOLUME /opt/sling/sling

ENV JAVA_OPTS -Xmx512m
ENV SLING_OPTS ''

CMD exec java $JAVA_OPTS -jar org.apache.sling.launchpad.jar $SLING_OPTS
