<!--
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
-->

<!-- 
    Configuration script to setup the JSP scripting engine
    for testing: sets development mode and interval 0 for
    checking script updates.
 -->

<%@ page import="
    org.osgi.service.cm.ConfigurationAdmin,
    org.osgi.service.cm.Configuration,
    java.util.Dictionary,
    java.util.Hashtable
"%>
<%@page session="false"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0"%>
<sling:defineObjects/>

<%
out.println("<pre>");
final String JSPConfig = "org.apache.sling.scripting.jsp.JspScriptEngineFactory";
ConfigurationAdmin ca = sling.getService(ConfigurationAdmin.class);
out.println("ca=" + ca);
final Configuration c = ca.getConfiguration(JSPConfig);
out.println("c=" + c);

Dictionary d = c.getProperties();
if(d == null) {
    d = new Hashtable();
}

final String interval = "jasper.modificationTestInterval";
out.println(interval + " was " + d.get(interval));
d.put(interval, new Integer(0));
out.println(interval + " is now " + d.get(interval));

final String dev = "jasper.development";
out.println(dev + " was " + d.get(dev));
d.put(dev, new Boolean(true));
out.println(dev + " is now " + d.get(dev));

c.setBundleLocation(null);
c.update(d);
out.println("Configuration updated: " + JSPConfig);

out.println("</pre>");
%>