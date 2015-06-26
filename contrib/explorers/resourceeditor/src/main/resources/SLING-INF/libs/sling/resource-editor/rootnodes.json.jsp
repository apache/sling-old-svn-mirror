<%--
  Licensed to the Apache Software Foundation (ASF) under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ASF licenses this file
  to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at
 
    http://www.apache.org/licenses/LICENSE-2.0
 
  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
--%>

<%@ page session="false"%>
<%@ page isELIgnored="false"%>
<%@ page import="javax.jcr.*,org.apache.sling.api.resource.Resource"%>
<%@ page import="java.util.Iterator"%>
<%@ page import="java.util.LinkedList, java.util.List"%>

<%@ page language="java" contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling"%>
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