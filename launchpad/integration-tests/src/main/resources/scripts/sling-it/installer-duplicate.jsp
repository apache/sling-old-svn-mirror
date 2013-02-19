<%@page session="false" %><%
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
%><%@page import="java.util.List,
                  org.apache.sling.installer.api.info.InfoProvider,
                  org.apache.sling.installer.api.info.InstallationState,
                  org.apache.sling.installer.api.info.Resource,
                  org.apache.sling.installer.api.info.ResourceGroup"%><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects/><%

    // we don't check for null etc to make the test fail if the service is not available!
    final InfoProvider ip = sling.getService(InfoProvider.class);
    final InstallationState is = ip.getInstallationState();

    String output = "";
    
    // check 01 : no untransformed resources
    if ( is.getUntransformedResources().size() > 0 ) {
        output += "Untransformed resources: " + is.getUntransformedResources() + "\n";
    }

    // check 02 : no active resources
    if ( is.getActiveResources().size() > 0 ) {
        output += "Active resources: " + is.getActiveResources() + "\n";
    }
    // check 03 : duplicates
    final List<ResourceGroup> resources = is.getInstalledResources();
    for(final ResourceGroup group : resources) {
        if ( group.getResources().size() > 1 ) {            
            boolean first = true;
            for(final Resource rsrc : group.getResources()) {
                if ( first ) {
                    output += "Duplicate resources for '" + rsrc.getEntityId() + "' : ";
                    first = false;
                } else {
                    output += ", ";
                }
                output += rsrc.getURL();
            }
            output += "\n";
        }
    }
    if ( output.length() > 0 ) {
        %><%= output %><%
    } else {
        %>TEST_PASSED<%
    }
%>