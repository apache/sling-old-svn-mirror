SLING_SCRIPTS=./scripts
GENERATE=${SLING_SCRIPTS}/generate_crankfile.sh
BUNDLES=${SLING_SCRIPTS}/bundles.rb
CRANKSTART=${SLING_SCRIPTS}/crankstart.sh

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
	$(CRANKSTART) target/crank.txt

start-s3: target/crank-s3.txt contrib/crankstart.jar
	$(CRANKSTART) target/crank-s3.txt

start-mongo: target/crank-mongo.txt contrib/crankstart.jar
	$(CRANKSTART) target/crank-mongo.txt

start-s3-mongo: target/crank-s3-mongo.txt contrib/crankstart.jar
	$(CRANKSTART) target/crank-s3-mongo.txt

contrib/crankstart.jar:
	mkdir -p contrib
	wget http://central.maven.org/maven2/org/apache/sling/org.apache.sling.crankstart.launcher/1.0.0/org.apache.sling.crankstart.launcher-1.0.0.jar -O contrib/crankstart.jar

update-bundles:
	rm -f crank.d/*-provisioning-*.txt
	$(BUNDLES)

install-deps: contrib/crankstart.jar
	${SLING_SCRIPTS}/download_dependencies.rb *.d/*

clean:
	rm -rf target sling
