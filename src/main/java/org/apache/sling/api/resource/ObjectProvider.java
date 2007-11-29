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
package org.apache.sling.api.resource;

/**
 * The <code>ObjectProvider</code> interface defines the API to be implemented
 * by classes which support access to data as some form of Java Object. For
 * example this interface may be implemented by an implementation of the
 * {@link Resource} interface if the resource abstract access to a JCR Node. In
 * this case the provided object might be created from an Object Content mapping
 * such as Jackrabbit OCM.
 */
public interface ObjectProvider {

    /**
     * Returns the object mapped from internal data or <code>null</code>, if
     * the data cannot be mapped. In a JCR-based implementation, the Jackrabbit
     * OCM mapping would be used to provide this object.
     */
    Object getObject();

}
