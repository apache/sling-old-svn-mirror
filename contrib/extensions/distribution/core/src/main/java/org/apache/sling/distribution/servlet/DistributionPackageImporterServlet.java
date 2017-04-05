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
package org.apache.sling.distribution.servlet;

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.sling.distribution.util.impl.DigestUtils.openDigestInputStream;
import static org.apache.sling.distribution.util.impl.DigestUtils.readDigestMessage;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.distribution.packaging.DistributionPackageImporter;
import org.apache.sling.distribution.packaging.DistributionPackageInfo;
import org.apache.sling.distribution.resources.DistributionResourceTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servlet to handle reception of distribution content.
 */
@SuppressWarnings("serial")
@SlingServlet(resourceTypes = DistributionResourceTypes.IMPORTER_RESOURCE_TYPE, methods = "POST")
public class DistributionPackageImporterServlet extends SlingAllMethodsServlet {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The <code>Digest</code> header, see <a href="https://tools.ietf.org/html/rfc3230#section-4.3.2">section-4.3.2</a>
     * of Instance Digests in HTTP (RFC3230)
     */
    private static final String DIGEST_HEADER = "Digest";

    private final Pattern digestHeaderRegex = Pattern.compile("(MD[25]|SHA-(?:1|256|384|512))=([a-fA-F0-9]+)");

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        DistributionPackageImporter distributionPackageImporter = request
                .getResource()
                .adaptTo(DistributionPackageImporter.class);

        String digestAlgorithm = null;
        String digestMessage = null;
        String digestHeader = request.getHeader(DIGEST_HEADER);
        if (isNotEmpty(digestHeader)) {
            log.debug("Found Digest header {}, extracting algorithm and message...", digestHeader);

            Matcher matcher = digestHeaderRegex.matcher(digestHeader);
            if (matcher.matches()) {
                digestAlgorithm = matcher.group(1);
                digestMessage = matcher.group(2);
            } else {
                log.debug("Digest header {} not supported, it doesn't match with expected pattern {}",
                          new Object[]{ digestHeader, digestHeaderRegex.pattern() });
            }
        }

        final long start = System.currentTimeMillis();
        response.setContentType("application/json");

        InputStream stream;
        if (isNotEmpty(digestAlgorithm) && isNotEmpty(digestMessage)) {
            stream = openDigestInputStream(request.getInputStream(), digestAlgorithm);
        } else {
            stream = request.getInputStream();
        }

        ResourceResolver resourceResolver = request.getResourceResolver();
        try {
            if (request.getParameter("forceError") != null) {
                throw new Exception("manually forced error");
            }

            DistributionPackageInfo distributionPackageInfo = distributionPackageImporter.importStream(resourceResolver, stream);

            long end = System.currentTimeMillis();

            if (isNotEmpty(digestAlgorithm) && isNotEmpty(digestMessage)) {
                String receivedDigestMessage = readDigestMessage((DigestInputStream) stream);
                if (!digestMessage.equalsIgnoreCase(receivedDigestMessage)) {
                    log.error("Error during distribution import: received distribution package is corrupted, expected [{}] but received [{}]",
                              new Object[]{ digestMessage, receivedDigestMessage });
                    Map<String, String> kv = new HashMap<String, String>();
                    kv.put("digestAlgorithm", digestAlgorithm);
                    kv.put("expected", digestMessage);
                    kv.put("received", receivedDigestMessage);
                    ServletJsonUtils.writeJson(response, SC_BAD_REQUEST, "Received distribution package is corrupted", kv);
                    return;
                }
            }

            log.info("Package {} imported successfully in {}ms", distributionPackageInfo, end - start);
            ServletJsonUtils.writeJson(response, SC_OK, "package imported successfully", null);

        } catch (final Throwable e) {
            ServletJsonUtils.writeJson(response, SC_INTERNAL_SERVER_ERROR, "an unexpected error has occurred during distribution import", null);
            log.error("Error during distribution import", e);
        } finally {
            long end = System.currentTimeMillis();
            log.debug("Processed package import request in {} ms", end - start);
        }
    }

    private static boolean isNotEmpty(String s) {
        return s != null && !s.isEmpty();
    }

}
