/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/

package org.apache.sling.hapi;

import java.util.List;
import java.util.Map;

/**
 * A Hypermedia API type.
 */
public interface HApiType {

    /**
     * The name of this type
     * @return
     */
    String getName();


    /**
     * The description of this type
     * @return
     */
    String getDescription();

    /**
     * The path of the Resource representing this type
     * @return
     */
    String getPath();

    /**
     * The external URL of the node representing this type
     * @return
     */
    String getUrl();

    /**
     * The fully qualified domain name of this type
     * @return
     */
    String getFqdn();

    /**
     * A list of {@link String} representing java-like generic types that can be used as types for the properties belonging to this type
     * @return
     */
    List<String> getParameters();

    /**
     * A map with the names of the properties as keys and the HApiProperty object as values defined for this type
     * <p>This list does not include properties inherited from the parent type</p>
     * @return
     */
    Map<String, HApiProperty> getProperties();

    /**
     * A map with the names of the properties as keys and the HApiProperty object as values defined for this type,
     * including the properties inherited from the parent type
     * @return
     */
    Map<String, HApiProperty> getAllProperties();

    /**
     * Returns the parent type object
     * @return
     */
    HApiType getParent();

    /**
     * Whether this type is abstract or not.
     * An abstract type is an identifier that does not map to a jcr node as a path or as a FQDN
     * @return
     */
    boolean isAbstract();
}
