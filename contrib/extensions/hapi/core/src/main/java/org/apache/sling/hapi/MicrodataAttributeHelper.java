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

import java.util.Map;

public interface MicrodataAttributeHelper {

    /**
     * Calls {@link #itemtypeMap()} and normalizes the map into a String of the form 'attr1="val1" attr2="val2"'
     * @return
     */
    String itemtype();

    /**
     * Get a map with the HTMl attributes for a new item of the type defined through
     * a new {@link MicrodataAttributeHelper} object
     * <p>The key is the HTMl attribute name and the value is the HTML attribute value</p>
     * @return
     */
    Map<String, String> itemtypeMap();

    /**
     * Calls {@link #itemprop(String, boolean)} with 'withType' true
     * @param propName
     * @return
     */
    String itemprop(String propName);

    /**
     * Calls {@link #itempropMap(String, boolean)} and normalizes the map into a String of the form 'attr1="val1" attr2="val2"'
     * @param propName
     * @param withType
     * @return
     */
    String itemprop(String propName, boolean withType);

    /**
     * Get a map with the HTMl attributes for the given property of the type defined through
     * a new {@link MicrodataAttributeHelper}
     * <p>The key is the HTMl attribute name and the value is the HTML attribute value</p>
     * <p> Will through a {@link HApiException}
     * runtime exception if the property propName does not exist for the type</p>
     * @param propName the name of the property
     * @param withType whether to include the 'itemtype' attribute
     * @return
     */
    Map<String, String> itempropMap(String propName, boolean withType);

    /**
     *  Get a map of maps with the HTMl attributes for each property of the type defined through
     *  a new {@link MicrodataAttributeHelper}
     *  <p>The key is the property name and the value is a map of attributes like the one returned
     *  by {@link #itempropMap(String, boolean)}</p>
     * @return
     */
    Map<String, Map<String, String>> allItemPropMap();

    /**
     * Get a map of types for each type property.
     * <p> The key is the property name and the value is the type path identifier of that property</p>
     * @return
     */
    Map<String, String> allPropTypesMap();
}
