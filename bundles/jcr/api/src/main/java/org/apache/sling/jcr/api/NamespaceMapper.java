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
package org.apache.sling.jcr.api;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * This interface is used to customize the namespace mapping of
 * a session.
 * @since 2.1
 * @deprecated Per session namespace mapping is not supported anymore.
 */
@Deprecated
@ConsumerType
public interface NamespaceMapper {

    /**
     * This method is invoked whenever a new session is created.
     * It allows the service to add own namespace prefixes.
     * @param session The new session
     * @throws RepositoryException If anything goes wrong
     */
    void defineNamespacePrefixes(Session session)
    throws RepositoryException;
}
