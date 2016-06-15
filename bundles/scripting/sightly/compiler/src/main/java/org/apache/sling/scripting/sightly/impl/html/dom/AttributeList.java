/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.html.dom;

import java.util.Iterator;

/**
 * Contains the list of attributes inside an HTML tag.
 */
public interface AttributeList {

    /**
     * Return the count of attributes
     * @return count of attributes
     */
    int attributeCount();

    /**
     * Return the list of attribute names
     * @return <code>Iterator</code> iterating over the attribute names
     */
    Iterator<String> attributeNames();

    /**
     * Return a flag indicating whether a specified attribute exists
     *
     * @param name the attribute's name
     * @return <code>true</code> if the specified attribute exists, <code>false</code> otherwise
     */
    boolean containsAttribute(String name);

    /**
     * Return an attribute's value, given its name or <code>null</code>
     * if the attribute cannot be found.
     * @param name   attribute name
     * @return an attribute's value
     */
    String getValue(String name);

    /**
     * Return an attribute's quote character, given its name or <code>0</code>
     * if the attribute cannot be found.
     * @param name   attribute name
     * @return an attribute's quote character
     */
    char getQuoteChar(String name);

    /**
     * Return an attribute's value, already surrounded with the quotes
     * originally in place. Returns <code>null</code> if the attribute
     * cannot be found
     * @param name   attribute name
     * @return an attribute's value
     */
    String getQuotedValue(String name);

    /**
     * Set an attribute's value. If the value is <code>null</code>, this
     * is semantically different to a {@link #removeValue(String)}.
     *
     * @param name      attribute name
     * @param value     attribute value
     */
    void setValue(String name, String value);

    /**
     * Remove an attribute's value.
     * @param name      attribute name
     */
    void removeValue(String name);

    /**
     * Return a flag indicating whether this object was modified.
     * @return <code>true</code> if the object was modified
     *         <code>false</code> otherwise
     */
    boolean isModified();
}