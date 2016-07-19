=======================================================
Apache Sling Authentication Requirement and Login Paths
=======================================================

Extension to Sling Authentication that populates and updates 
`org.apache.sling.auth.core.AuthenticationRequirement` based on information
stored in a JCR content repository.

This module defines a new JCR mixin type (`sling:AuthenticationRequired`),
which is used to identify subtrees in the JCR repository that require 
authentication in order to be accessible each being optionally associated
with a login path. The optional login path will be excluded from authentication 
requirement.

Public API
----------

- `org.apache.sling.auth.requirement.LoginPathProvider`: provides a login 
   path for a given `HttpServletRequest`. The default implementation tries
   to find the best matching path based on the optional `sling:loginPath`
   properties.
   
    
Internal API
------------
    
- `org.apache.sling.auth.requirement.impl.RequirementHandler`: receives (content) 
   modifications related to authentication requirement and is in charge
   of notifying the `org.apache.sling.auth.core.AuthenticationRequirement`
   and keeping track of the complete set of login paths.
   

Configuration
-------------

The `org.apache.sling.auth.requirement.impl.DefaultRequirementHandler` requires
a set of supported paths, where the `sling:AuthenticationRequired` mixin type
will be respected.


Implementation Details
----------------------

The default implementation registers an Apache Jackrabbit Oak 
`org.apache.jackrabbit.oak.spi.commit.Observer` that keeps track of all
modifications related to `sling:AuthenticationRequired` mixin type and
notifies the configured `org.apache.sling.auth.requirement.RequirementHandler`.

The `DefaultRequirementHandler` service will search for existing (valid) 
requirements upon activation. It is informed about subsequent changes 
to the auth requirement by the `Observer`, filters them based on the given
supported paths and updates the Sling Authentication using the API provided
by `org.apache.sling.auth.core.AuthenticationRequirement`.


System Requirements
-------------------

- org.apache.sling.auth.core bundle 1.3.18 or higher
- JCR content repository based on Apache Jackrabbit Oak 1.4
- Repo-Init with a dedicated service user that has read access at the configured paths
- Oak Property Index to optimize query searching for existing auth requirements

TODO
----

- repo-init containing service user
- dedicated index for sling:AuthenticationRequired


Getting Started
---------------

This component uses an Apache Maven (http://maven.apache.org/) build
environment. It requires a Java 7 JDK (or higher) and Maven (http://maven.apache.org/)
3.2.3 or later. We recommend to use the latest Maven version.

If you have Maven installed, you can build and package the jar using the following command:

    mvn clean install

See the Maven documentation for other build features.

The latest source code for this component is available in the
Subversion (http://subversion.tigris.org/) source repository of
the Apache Software Foundation. If you have Subversion installed,
you can checkout the latest source using the following command:

    svn checkout http://svn.apache.org/repos/asf/sling/trunk/contrib/auth/requirement

See the Subversion documentation for other source control features.
