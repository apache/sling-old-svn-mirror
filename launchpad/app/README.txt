How to run the Sling launcher/app module
----------------------------------------

1) Build Sling using 

	mvn clean install
	
in the top-level directory of the Sling source code.

2) Come back to this directory and build using 

	mvn -P full clean install

3) Start the generated jar with

	 java -jar target/org.apache.sling.launcher.app-2.0.0-incubator-SNAPSHOT-full.jar 
	 
Use the correct version number instead of 2.0.0-incubator-SNAPSHOT, if needed.