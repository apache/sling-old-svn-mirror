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

/**
 * A Hypermedia API property for a {@link HApiType}
 */
public interface HApiProperty {

    /**
     * Get the name of this property
     * @return
     */
    String getName();

    /**
     * Set the name of this property
     * @param name
     */
    void setName(String name);

    /**
     * Get the description of this property
     * @return
     */
    String getDescription();

    /**
     * Set the description of this property
     * @return
     */
    void setDescription(String description);

    /**
     * Get the type of this property
     * @return
     */
    HApiType getType();

    /**
     * Set the type of this property
     * @return
     */
    void setType(HApiType type);

    /**
     * Whether this property is a multiple value
     * @return
     */
    Boolean getMultiple();

    /**
     * Set the boolean value for multiple
     * @param multiple
     */
    void setMultiple(Boolean multiple);
}
