GENERATE=./scripts/generate_crankfile.sh
BUNDLES=./scripts/bundles.rb
.PHONY: clean start start-s3 start-mongo start-s3-mongo

all: target/crank.txt target/crank-s3.txt target/crank-mongo.txt target/crank-s3-mongo.txt

target:
	mkdir target

target/crank.txt: target crank.d
	$(GENERATE) > target/crank.txt

target/crank-s3.txt: target crank.d crank-s3.d crank-s3.d/05-credentials.txt
	$(GENERATE) s3 > target/crank-s3.txt

target/crank-mongo.txt: target crank.d crank-mongo.d
	$(GENERATE) mongo > target/crank-mongo.txt

target/crank-s3-mongo.txt: target crank.d crank-mongo.d crank-s3.d crank-s3.d/05-credentials.txt
	$(GENERATE) mongo s3 > target/crank-s3-mongo.txt

start: target/crank.txt contrib/crankstart.jar
	java -jar contrib/crankstart.jar target/crank.txt

start-s3: target/crank-s3.txt contrib/crankstart.jar
	java -jar contrib/crankstart.jar target/crank-s3.txt

start-mongo: target/crank-mongo.txt contrib/crankstart.jar
	java -jar contrib/crankstart.jar target/crank-mongo.txt

start-s3-mongo: target/crank-s3-mongo.txt contrib/crankstart.jar
	java -jar contrib/crankstart.jar target/crank-s3-mongo.txt

install-deps: contrib/oak-s3 contrib/org.apache.sling.launchpad.karaf contrib/crankstart.jar
        mkdir -p contrib; cd contrib
	cd contrib/oak-s3; mvn clean install; cd ..
	cd contrib/org.apache.sling.launchpad.karaf; mvn clean install; cd ..
        cd ..

update-bundles:
	rm crank.d/*-sling-startlevel-*.txt
	$(BUNDLES)

contrib/org.apache.sling.launchpad.karaf:
	cd contrib; svn export https://svn.apache.org/repos/asf/sling/trunk/contrib/launchpad/karaf/org.apache.sling.launchpad.karaf/; cd ..

contrib/oak-s3:
	cd contrib; svn export https://svn.apache.org/repos/asf/sling/trunk/contrib/extensions/oak-s3/; cd ..

contrib/crankstart.jar:
	wget http://central.maven.org/maven2/org/apache/sling/org.apache.sling.crankstart.launcher/1.0.0/org.apache.sling.crankstart.launcher-1.0.0.jar -O contrib/crankstart.jar

clean:
	rm -rf target sling
