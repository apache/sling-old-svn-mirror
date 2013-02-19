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
%><%@page import="org.junit.Assert,
                  org.apache.sling.api.resource.ResourceResolver"%><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.0" %><%
%><sling:defineObjects/><%

    final ResourceResolver resResolver = slingRequest.getResourceResolver();

    resResolver.create(resResolver.getResource("/"), "node", null);
    Assert.assertEquals("/node", resResolver.getResource("/node").getPath());
    resResolver.delete(resResolver.getResource("/node"));
    resResolver.commit();
    Assert.assertNull(resResolver.getResource("/node"));
%>TEST_PASSED