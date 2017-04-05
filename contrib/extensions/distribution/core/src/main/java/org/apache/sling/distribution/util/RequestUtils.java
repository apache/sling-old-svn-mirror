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
package org.apache.sling.distribution.util;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.http.client.utils.URIBuilder;
import org.apache.sling.distribution.DistributionRequest;
import org.apache.sling.distribution.DistributionRequestType;
import org.apache.sling.distribution.SimpleDistributionRequest;
import org.apache.sling.distribution.impl.DistributionParameter;

/**
 * Utility class for HTTP / distribution request related operations.
 */
public class RequestUtils {

    public static DistributionRequest fromServletRequest(HttpServletRequest request) {
        String action = request.getParameter(DistributionParameter.ACTION.toString());
        String[] paths = request.getParameterValues(DistributionParameter.PATH.toString());
        String deepParam = request.getParameter(DistributionParameter.DEEP.toString());

        boolean deep = false;
        if ("true".equals(deepParam)) {
            deep = true;
        }

        if (paths == null) {
            paths = new String[0];
        }


        return new SimpleDistributionRequest(DistributionRequestType.fromName(action), deep, paths);
    }

    public static URI appendDistributionRequest(URI uri, DistributionRequest distributionRequest) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(uri);
        uriBuilder.addParameter(DistributionParameter.ACTION.toString(), distributionRequest.getRequestType().name());

        String[] paths = distributionRequest.getPaths();

        if (paths != null) {
            for (String path : paths) {
                uriBuilder.addParameter(DistributionParameter.PATH.toString(), path);
            }
        }

        return uriBuilder.build();
    }

}
