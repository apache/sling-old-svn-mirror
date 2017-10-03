# Apache Sling SLF4J MDC Filter

This module is part of the [Apache Sling](https://sling.apache.org) project.

This filter exposes various request details as part of [MDC][1]. 

Currently it exposes following variables:

1. `req.remoteHost` - Request remote host
2. `req.userAgent` - User Agent Header
3. `req.requestURI` - Request URI
4. `req.queryString` - Query String from request
5. `req.requestURL` -
6. `req.xForwardedFor` -
7. `sling.userId` - UserID associated with the request. Obtained from ResourceResolver
8. `jcr.sessionId` - Session ID of the JCR Session associated with current request.

The filter also allow configuration to extract data from request cookie, header and parameters. Look for
configuration with name 'Apache Sling Logging MDC Inserting Filter' for details on specifying header, cookie,
param names.

[1] http://www.slf4j.org/manual.html#mdc