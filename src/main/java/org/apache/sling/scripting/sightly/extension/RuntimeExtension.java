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
package org.apache.sling.scripting.sightly.extension;

import org.apache.sling.scripting.sightly.render.RenderContext;

import aQute.bnd.annotation.ConsumerType;

/**
 * A {@code RuntimeExtension} represents a HTL runtime construct that provides some processing capabilities for the various
 * {@code data-sly-*} block elements.
 */
@ConsumerType
public interface RuntimeExtension {

    /**
     * For OSGi environments this is the name of the service registration property indicating the {@code RuntimeExtension} name.
     */
    String NAME = "org.apache.sling.scripting.sightly.extension.name";

    /**
     * Call the {@code RuntimeExtension}
     *
     * @param renderContext the runtime context
     * @param arguments     the call arguments
     * @return an extension instance
     */
    Object call(RenderContext renderContext, Object... arguments);
}
