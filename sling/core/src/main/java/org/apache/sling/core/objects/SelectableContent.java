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
package org.apache.sling.core.objects;

/**
 * The <code>SelectableContent</code> interface extends the
 * <code>Content</code> interface by a method which allows to retrieve a
 * {@link Selector} which may return a different content object than the primary
 * object resolved from the path.
 *
 * @ocm.mapped jcrMixinTypes="sling:Selectable" discriminator="false"
 */
public interface SelectableContent {

    /**
     * Returns the {@link Selector} to call when the request is resolved to a
     * <code>Content</code> object to select a different object based on the
     * current request or some other situation like the current system time in
     * the case of content scheduling
     *
     * @return The {@link Selector} to call or <code>null</code> if none has
     *         been configured or none should be used.
     */
    Selector getSelector();

}
