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

package org.apache.sling.replication.transport.impl;

import org.apache.http.client.utils.URIBuilder;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.replication.communication.ReplicationActionType;
import org.apache.sling.replication.communication.ReplicationParameter;
import org.apache.sling.replication.communication.ReplicationRequest;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;

public class RequestUtils {

    public static ReplicationRequest fromServletRequest(HttpServletRequest request) {
        String action = request.getParameter(ReplicationParameter.ACTION.toString());
        String[] paths = request.getParameterValues(ReplicationParameter.PATH.toString());

        ReplicationRequest replicationRequest = new ReplicationRequest(System.currentTimeMillis(),
                ReplicationActionType.fromName(action),
                paths);

        return replicationRequest;

    }

    public static URI appendReplicationRequest(URI uri, ReplicationRequest replicationRequest) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(uri);
        uriBuilder.addParameter(ReplicationParameter.ACTION.toString(), ReplicationActionType.POLL.getName());
        for (String path : replicationRequest.getPaths()) {
            uriBuilder.addParameter(ReplicationParameter.PATH.toString(), path);
        }
        return uriBuilder.build();
    }

}
