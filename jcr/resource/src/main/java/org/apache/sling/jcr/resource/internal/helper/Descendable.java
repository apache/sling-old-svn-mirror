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
package org.apache.sling.jcr.resource.internal.helper;

import java.util.Iterator;

import org.apache.sling.api.resource.Resource;

/**
 * The <code>Descendable</code> defines the API for resources (or other
 * objects) which may have descendants, which may be enumerated or directly
 * access.
 */
public interface Descendable {

    /**
     * Returns an iterator on all direct descendents aka children. If this
     * resource has no children, an empty iterator is returned.
     */
    Iterator<Resource> listChildren();

    /**
     * Returns the descedent at the given relative path or <code>null</code>
     * if this resource does not have such a descendent.
     */
    Resource getDescendent(String relPath);

}
