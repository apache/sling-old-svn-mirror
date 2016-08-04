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

}
