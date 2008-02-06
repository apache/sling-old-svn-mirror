<!-- simple JSP rendering test -->
<%@page session="false"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0"%>
<sling:defineObjects/>
JSP rendering result:<%= resource.adaptTo(javax.jcr.Node.class).getProperty("text").getValue().getString() %>