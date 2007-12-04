------------------------------------------------------------------------------
microsling standalone server
See http://incubator.apache.org/sling for more info.
------------------------------------------------------------------------------
The contents of this zip file allow microsling to run
standalone, using the Jetty web server's start.jar utility.

To run microsling:
1) Expand this zip file in an empty directory

2) In this directory, run

  java -jar start.jar
  
Using the Java 5 JDK.

3) http://localhost:8080 should display the "microsling homepage", 
   follow the instructions found there.    

Note that microsling creates a number of files and directories in 
the current directory. The jackrabbit-repository directory contains
the repository data, and can be deleted to start fresh, after stopping
microsling.