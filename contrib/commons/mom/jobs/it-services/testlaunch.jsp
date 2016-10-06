<%--
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
 */
--%><%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" contentType="text/html; charset=utf-8"
	pageEncoding="UTF-8"
    import="org.apache.sling.api.resource.*,
    java.util.*,
    javax.jcr.*,
    com.day.cq.search.*,
    com.day.cq.wcm.api.*,
    com.day.cq.dam.api.*,
    org.apache.sling.jobs.*,
    com.google.common.collect.*"%><%

    // This is an AEM Fiddle that runs some jobs.

    JobManager jobManager = sling.getService(JobManager.class);
    for ( int i = 0; i < 100; i++ ) {
        Job job = jobManager.newJobBuilder(
                Types.jobQueue("org/apache/sling/jobs/it/services"),
                Types.jobType("treadding/asyncthreadpoolwithbacklog"))
            .addProperties(
                        ImmutableMap.of("jobtest", (Object) "jobtest"))
            .add();
%>Added Job <%= job.getId() %><br/><%
    }
%>