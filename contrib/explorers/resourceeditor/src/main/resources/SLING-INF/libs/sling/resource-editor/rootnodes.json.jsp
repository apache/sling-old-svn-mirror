<%@ page session="false"%>
<%@ page isELIgnored="false"%>
<%@ page import="javax.jcr.*,org.apache.sling.api.resource.Resource"%>
<%@ page import="java.util.Iterator"%>
<%@ page import="java.util.LinkedList, java.util.List"%>

<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0"%>
<sling:defineObjects />
<% response.setContentType("application/json"); %>
[{
	"id" : "root",
	"state" : {"opened":true, "disabled": false, "selected": false},
	"text"	: "<i class=\"jstree-icon node-icon open-icon\"></i><i class=\"jstree-icon node-icon add-icon\"></i> /",
	"li_attr" :{ "nodename" : "${currentNode.name}", "nodetype" :"${currentNode.primaryNodeType.name}" },
	"a_attr" :{ "href" : "<%= request.getContextPath() %>/reseditor/.html" },
	"children" :
		<%@ include file="nodes.json.incl.jsp" %>
}]