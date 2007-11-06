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
package org.apache.sling.content.jcr;

import java.util.Iterator;

import org.apache.jackrabbit.ocm.exception.ObjectContentManagerException;
import org.apache.jackrabbit.ocm.manager.ObjectContentManager;
import org.apache.sling.content.ContentManager;

/**
 * The <code>ContentManager</code> TODO
 */
public interface JcrContentManager extends ContentManager, ObjectContentManager {

    /**
     * Creates a new query by specifying the query <code>statement</code> itself and the
     * <code>language</code> in which the query is stated. If the query <code>statement</code> is
     * syntactically invalid, given the language specified, an
     * <code>InvalidQueryException</code> is thrown. The <code>language</code> must
     * be a string from among those returned by QueryManager.getSupportedQueryLanguages();
     * if it is not, then an <code>InvalidQueryException</code> is thrown.
     *
     * @return A <code>Query</code> object.
     *
     * @throws InvalidQueryException If the query cannot be parsed
     * @throws ObjectContentManagerException If an error occurrs while
     *      evaluating the query.
     */
    Iterator getObjects(String query, String language);

}
