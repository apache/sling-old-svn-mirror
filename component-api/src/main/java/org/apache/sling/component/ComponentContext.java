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
package org.apache.sling.component;

import javax.servlet.ServletContext;

/**
 * The <code>ComponentContext</code> interface defines a component view of the
 * Component Framework. The <code>ComponentContext</code> also makes resources
 * available to the component. Using the context, a component can obtain URL
 * references to resources.
 * <p>
 * Attibutes stored in the context are global for <I>all</I> users and <I>all</I>
 * components in the component application.
 * <p>
 * It is intended that components use the logging framework provided by the
 * application they are part of. For this reason, this class provides no logging
 * methods.
 */
public interface ComponentContext extends ServletContext {

    /**
     * Returns a {@link ComponentRequestDispatcher} object that acts as a
     * wrapper for the content located at the given path. A
     * <code>ComponentRequestDispatcher</code> object can be used to include
     * the resource in a response.
     * <p>
     * This method returns <code>null</code> if the
     * <code>ComponentContext</code> cannot return a
     * <code>ComponentRequestDispatcher</code> for any reason.
     *
     * @param content The {@link Content} instance whose response content may be
     *            included by the returned dispatcher.
     * @return a <code>ComponentRequestDispatcher</code> object that acts as a
     *         wrapper for the <code>content</code> or <code>null</code> if
     *         an error occurrs preparing the dispatcher.
     * @see ComponentRequestDispatcher
     */
    ComponentRequestDispatcher getRequestDispatcher(Content content);

}
