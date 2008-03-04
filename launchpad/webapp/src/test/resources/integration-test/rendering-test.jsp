<!-- simple JSP rendering test -->
<%@page session="false"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0"%>
<sling:defineObjects/>

<h1>JSP rendering result</h1>
<p>
	text value using resource.adaptTo:<%= resource.adaptTo(javax.jcr.Node.class).getProperty("text").getValue().getString() %>
</p>
<p>
	text value using currentNode:<%= currentNode.getProperty("text").getValue().getString() %>
</p>
