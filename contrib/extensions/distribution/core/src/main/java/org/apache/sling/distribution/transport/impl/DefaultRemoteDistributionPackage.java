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

import org.apache.http.client.fluent.Executor;
import org.apache.sling.distribution.packaging.impl.DistributionPackageUtils;
import org.apache.sling.distribution.serialization.DistributionPackage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;


public class DefaultRemoteDistributionPackage implements RemoteDistributionPackage {
    private static final Logger log = LoggerFactory.getLogger(DefaultRemoteDistributionPackage.class);


    private final DistributionPackage wrappedPackage;
    private final Executor executor;
    private final URI distributionURI;
    private final String remoteId;

    public DefaultRemoteDistributionPackage(DistributionPackage wrappedPackage, Executor executor, URI distributionURI) {
        this.wrappedPackage = wrappedPackage;
        this.executor = executor;
        this.distributionURI = distributionURI;
        this.remoteId = (String) wrappedPackage.getInfo().get(DistributionPackageUtils.PROPERTY_REMOTE_PACKAGE_ID);
    }


    public DistributionPackage getPackage() {
        return wrappedPackage;
    }

    public void deleteRemotePackage() {

        try {
            HttpTransportUtils.deletePackage(executor, distributionURI, remoteId);
        } catch (URISyntaxException e) {
            log.error("cannot delete remote package", e);
        } catch (IOException e) {
            log.error("cannot delete remote package", e);
        }
    }
}
