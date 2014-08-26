Apache Sling mail archive server sample app
==========================================

This is a work in progress sample application for Sling, initially
contributed by Igor Bogomolov who wrote it as part of an internship,
and meant to create a useful mail archive server that also serves
as a more complex Sling sample.

To run this, and install the bundles provided by this module in a 
Sling Launchpad (trunk) instance.

Then, start at http://localhost:8080/mailarchiveserver/import.mbox.html
to import a few mbox files, you can find some at 
server/src/test/resources/test-files/mbox/

http://localhost:8080/mailarchiveserver.html is the server's homepage.

To import live mail from a mail server you need a Connector - the SLING-3297
contribution does include one for Exchange, but we haven't included it
in this sample so far.

Stats (even more work in progress) are available at 
http://localhost:8080/content/mailarchiveserver/stats.html
