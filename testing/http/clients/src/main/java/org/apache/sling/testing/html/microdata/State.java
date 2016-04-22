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
package org.apache.sling.testing.html.microdata;

/**
 * An application state returned by the REST server. In practical term it is also considered as HTML document.
 * 
 * @see <a href="http://en.wikipedia.org/wiki/Representational_state_transfer#About">http://en.wikipedia.org/wiki/Representational_state_transfer#About</a>
 */
public interface State {

    /**
     * Returns all the links (&lt;a&gt; or &lt;link&gt;) having the given relationship in the this state.
     *
     * @param rel the relationship to filter links
     * @return the links
     */
    Items link(String rel);

    /**
     * Returns all the forms (&lt;form&gt;) having the given relationship in the this state.
     *
     * @param rel the relationship to filter forms
     * @return the forms
     */
    Items form(String rel);

    /**
     * Returns all the Microdata items having the given relationship in the this state.
     *
     * @param rel the relationship to filter items
     * @return the items
     * @see <a href="http://schema.org/docs/gs.html">http://schema.org/docs/gs.html</a>
     */
    Items item(String rel);

    /**
     * @return all the top level (not contained inside another object) Microdata items in the this state.
     * @see <a href="http://schema.org/docs/gs.html">http://schema.org/docs/gs.html</a>
     */
    Items items();


}
