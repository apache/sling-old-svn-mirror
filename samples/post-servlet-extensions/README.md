# Apache Sling Sample Post Servlet Extensions

This module is part of the [Apache Sling](https://sling.apache.org) project.

This bundle provides sample OSGI services which extend the sling post 
servlet behaviour.

The default POST behavior of Sling can be changed by registering
custom servlets that process POST requests for specific paths and/or
node types.

Another, often simpler, way of customizing the POST behavior is to
register OSGi services for the two extension points that the
SlingPostServlet provides: the SlingPostOperation and the
SlingPostProcessor.

The SlingPostOperation interface is meant to create new operations in
addition to the standard (:operation=delete, :operation=move. etc.)
ones.

The SlingPostProcessor is a called by the SlingPostServlet after the
(standard or custom) SlingPostOperation selected by the request is
called.

These two extension points allow the POST behavior to be customized
easily, with small amounts of code and while reusing most of the
standard behavior.
