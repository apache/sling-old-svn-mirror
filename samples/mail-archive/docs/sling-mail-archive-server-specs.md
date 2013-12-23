Sling Mail Archive Server Specification
=======================================

Overview
--------
The goal of this student project is to create a mail archive server
based on Apache Sling.

The server is meant both as a high capacity archive server for mailing lists,
as well as an example application that demonstrates Sling best practices.

This is an initial specification for the server, we'll refine it as we go.

Use cases
---------
Here's a rough initial list of use cases:

* Import email live from POP/IMAP servers, as well as via Exchange web services
* Mass imports of various mail archive file formats: mbox, Outlook PST, etc.
* Provide time-based and thread-based mail browsing RESTful and web interfaces.
* Provide short permanent URLs for each individual message and each thread.
* Optional content filters to collapse repeated sections in messages (quoting other messages).
* Optional content filters for intelligent linking (to jira etc.)
* Optional content filters for "de-junking" Outlook messages.
* Optional content filters for syntax coloring of code excerpts.
* Optional exclude filters to ignore some messages (Jenkins notifications for instance)
* Tagging of archived messages, for example to flag them as useful or obsolete. 
* Tag-based navigation.
* Full-text and structured search.
* Traffic statistics.

Architecture, performance, scalability etc.
-------------------------------------------
Also a rough list of criteria for now:

* Use Sling for all web interactions, Jackrabbit Oak as the content repository once that becomes available, jQuery etc. for the front-end (or maybe http://createjs.org/)
* OSGi services for everything, some scripting for HTML rendering and user plugins if appropriate
* Kill -9 is fine as a shutdown mechanism, causes no harm
* Handle huge import files, avoid loading the whole thing in memory to stay scalable
* Scale to thousands of lists and millions of messages
* Much less than 2 seconds response time for all common operations
* Optional per-list access control
