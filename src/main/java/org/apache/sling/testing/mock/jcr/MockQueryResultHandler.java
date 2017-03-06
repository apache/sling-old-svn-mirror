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
package org.apache.sling.testing.mock.jcr;

import org.osgi.annotation.versioning.ConsumerType;

/**
 * Allows to set mocked query results for a mocked {@link javax.jcr.query.QueryManager}.
 */
@ConsumerType
public interface MockQueryResultHandler {

    /**
     * Checks if this handler accepts the given query, and returns the result if this is the case.
     * @param query Query that is executed
     * @return Query result if the query can be executed by this handler.
     *   If not, null is returned and other handlers are asked to provide a result.
     */
    MockQueryResult executeQuery(MockQuery query);
    
}
