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
package org.apache.sling.distribution.transport.authentication;

import aQute.bnd.annotation.ConsumerType;
import org.apache.sling.distribution.component.DistributionComponent;

/**
 * A {@link org.apache.sling.distribution.transport.authentication.TransportAuthenticationProvider} is responsible for
 * authentication of instances sending and receiving distribution items via transport algorithms
 * A {@link org.apache.sling.distribution.transport.authentication.TransportAuthenticationProvider} will authenticate
 * 'authenticables' objects of type {@link A}, producing 'authenticated' objects of type {@link T}.
 *
 */
@ConsumerType
public interface TransportAuthenticationProvider<A, T> extends DistributionComponent {

    /**
     * Check if this provider is able to authenticate objects belonging to given 'authenticable' class.
     *
     * @param authenticable class of objects to be authenticated
     * @return {@code true} if this provider can check authentication on instances of this class, {@code false}
     * otherwise
     */
    boolean canAuthenticate(Class<A> authenticable);

    /**
     * Authenticate an 'authenticable' object by performing some implementation specific operation on it, and producing
     * an 'authenticated' object to be passed back to a transport algorithm.
     * The returned 'authenticated' object may be of the same class of the 'authenticable' object (e.g. passing an 'authenticable'
     * http client and returning an 'authenticated' http client) or of a different class (e.g. passing an 'authenticable'
     * jcr repository and returning an 'authenticated' jcr session).
     *
     * @param authenticable class of objects to be authenticated
     * @param context       authentication context holding authentication information
     * @return an 'authenticated' object to be used by the transport
     * @throws TransportAuthenticationException
     */
    T authenticate(A authenticable, TransportAuthenticationContext context)
            throws TransportAuthenticationException;

}
