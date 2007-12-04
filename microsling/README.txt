------------------------------------------------------------------------------
microsling main module
See http://incubator.apache.org/sling for more info.
------------------------------------------------------------------------------

The easiest way of running microsling is to:

1) Build sling by running "mvn clean install" in the parent directory
   of this one.

2) Come back to this directory and build microsling by running
   "mvn clean install" here

2b) (This should not be needed, but see TODO in pom.xml):

   cd microsling-standalone
   mvn clean install
   cd ..

3) Use the zip file that, if all goes well, is generated in
   microsling-standalone/target, expand it into an empty
   directory and see the README.txt there.
