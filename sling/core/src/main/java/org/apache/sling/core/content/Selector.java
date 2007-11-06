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
package org.apache.sling.core.content;

import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.Content;

/**
 * The <code>Selector</code> interface defines the API to be implemented to
 * select content.
 *
 * @ocm.mapped jcrType="sling:Selector" discriminator="false"
 */
public interface Selector {

    /**
     * Selects different content based on selection criteria defined in this
     * object.
     * <p>
     * If the selection criteria stipulates, that there is no content, this
     * method may return <code>null</code> in which case, no content is
     * selected at all. This method may also return the input
     * <code>content</code> to indicate no different content selection at all.
     *
     * @param request The <code>ComponentRequest</code> object, which may be
     *            used to ask for request specific selection configuration.
     *            Namely the cookies, request parameters and component session
     *            may be queried in this process.
     * @param content The originally resolved <code>Content</code> object.
     * @return The <code>Content</code> object to further handle. This may be
     *         <code>null</code> if selection resolves to no content at all
     *         (for example, if the original <code>content</code> has been
     *         descheduled and no current content is available) or the original
     *         <code>content</code> if no change is used.
     */
    Content select(ComponentRequest request, Content content);

}
