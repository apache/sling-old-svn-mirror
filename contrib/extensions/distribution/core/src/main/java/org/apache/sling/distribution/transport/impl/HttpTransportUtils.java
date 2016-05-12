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

package org.apache.sling.distribution.transport.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpTransportUtils {

    private static final Logger log = LoggerFactory.getLogger(HttpTransportUtils.class);

    public static InputStream fetchNextPackage(Executor executor, URI distributionURI) throws URISyntaxException, IOException {
        URI fetchUri = getFetchUri(distributionURI);
        Request fetchReq = Request.Post(fetchUri).useExpectContinue();
        HttpResponse httpResponse = executor.execute(fetchReq).returnResponse();

        if (httpResponse.getStatusLine().getStatusCode() != 200) {
            return null;
        }

        HttpEntity entity = httpResponse.getEntity();

        return entity.getContent();
    }

    public static boolean deletePackage(Executor executor, URI distributionURI, String remotePackageId) throws URISyntaxException, IOException {

        URI deleteUri = getDeleteUri(distributionURI, remotePackageId);
        Request deleteReq = Request.Post(deleteUri).useExpectContinue();
        HttpResponse httpResponse = executor.execute(deleteReq).returnResponse();

        return httpResponse.getStatusLine().getStatusCode() == 200;
    }

    private static URI getFetchUri(URI uri) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(uri);
        uriBuilder.addParameter("operation", "fetch");

        return uriBuilder.build();
    }

    private static URI getDeleteUri(URI uri, String id) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(uri);
        uriBuilder.addParameter("operation", "delete");
        uriBuilder.addParameter("id", id);

        return uriBuilder.build();
    }

}
