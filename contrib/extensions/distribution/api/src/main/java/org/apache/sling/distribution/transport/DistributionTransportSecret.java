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
import java.io.InputStream;
import java.util.Map;

import aQute.bnd.annotation.ConsumerType;

/**
 * The secret to be transported for authenticating transport layer connecting two instances.
 * <p/>
 * Secrets can take different forms, like e.g. username and password, tokens, public keys, etc. and are meant to be used
 * by transport implementations used by distribution agents.
 */
@ConsumerType
public interface DistributionTransportSecret {

    /**
     * Get the secret as a {@link java.util.Map} of credentials, this can contain, for example, entries holding information
     * about username and password for HTTP authentication.
     *
     * @return the credentials as a {@link java.util.Map}, or {@code null} if {@code secret} cannot be represented in terms
     * of a set of key -> value entries
     */
    @CheckForNull
    Map<String, String> asCredentialsMap();

    /**
     * Get the secrete as a raw {@link java.io.InputStream binary}.
     * Note that each call to this method will create a new stream, so the caller will be responsible of closing it.
     *
     * @return the secret as an {@link java.io.InputStream}, or {@code null} if such a secret cannot represented as a stream.
     */
    @CheckForNull
    InputStream asStream();

}
