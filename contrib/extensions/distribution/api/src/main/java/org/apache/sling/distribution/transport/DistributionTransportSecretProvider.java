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
package org.apache.sling.distribution.transport;

import javax.annotation.CheckForNull;

import aQute.bnd.annotation.ConsumerType;

import java.net.URI;

/**
 * A provider for {@link org.apache.sling.distribution.transport.DistributionTransportSecret}s
 * <p/>
 * Such providers can be used by distribution agents implementations in order to plug
 * in different types of {@link org.apache.sling.distribution.transport.DistributionTransportSecret secrets} to be used
 * to authenticate the underlying Sling instances.
 */
@ConsumerType
public interface DistributionTransportSecretProvider {

    /**
     * Get a {@link org.apache.sling.distribution.transport.DistributionTransportSecret} for the specified URI
     *
     * @param uri - the uri than needs authentication
     * @return a {@link org.apache.sling.distribution.transport.DistributionTransportSecret secret}, or {@code null} if
     * that cannot be obtained
     */
    @CheckForNull
    DistributionTransportSecret getSecret(URI uri);
}
