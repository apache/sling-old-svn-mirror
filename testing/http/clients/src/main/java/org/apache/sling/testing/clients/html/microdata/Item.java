/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.clients.html.microdata;


import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.html.Values;

/**
 * A representation of Microdata item.
 * 
 * @see <a href="http://schema.org/docs/gs.html">http://schema.org/docs/gs.html</a>
 */
public interface Item {

    /**
     * Returns the property of the item having the given name.
     *
     * @param name the name of the item
     * @return the property
     * @throws ClientException if the request could not be completed
     */
    Items prop(String name) throws ClientException;


    /**
     * @return the text value of the property
     * @throws ClientException if the request could not be completed
     */
    String text() throws ClientException;

    /**
     * @return the boolean value of the property
     * @throws ClientException if the request could not be completed
     */
    boolean bool() throws ClientException;

    /**
     * @return the href value of the property. This only makes sense for hyperlink (&lt;a&gt; or &lt;link&gt;)
     */
    String href();

    /**
     * @return the <i>src</i> value of the property
     */
    String src();

    /**
     * Makes transition to a new state exposed by this hyperlink item (&lt;a&gt; or &lt;link&gt;).
     *
     * @return the State after navigate
     * @throws ClientException if the operation could not be completed
     *
     * @see <a href="http://en.wikipedia.org/wiki/Representational_state_transfer#About">http://en.wikipedia.org/wiki/Representational_state_transfer#About</a>
     */
    State navigate() throws ClientException;

    /**
     * Submits this form item and returns the resulting state.
     *
     * @param values the form values to be submitted
     * @return the State after submit
     * @throws ClientException if the operation could not be completed
     */
    State submit(Values values) throws ClientException;
}
