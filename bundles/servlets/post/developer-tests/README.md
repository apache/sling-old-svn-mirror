# Developer test scripts.

Testing POST behaviour can involve a lot of curl commands or browser work, expecially with file uploads. 
This folder contains test scripts that verify the behaviour of this bundle in a running Sling instance. 
They are not intended as a replacement for unit tests or integration tests, but rather a quick way to allow
a developer to verify behaviour and be in control of the test being run.

## testFileUploads.sh
run in parent as sh developer-tests/testFileUploads.sh <testfile> and it will upload that file using non streamed, 
 streamed, and streaming chunked protocols to the Sling server on localhost:8080 as admin:admin. It will also download 
 the file and diff it against the local copy to ensure no changes. Chunks are 20kb each, so a large file will generate a
 large number of small chunks. Dont try and test a GB file without editing the script to increase the chunk size.