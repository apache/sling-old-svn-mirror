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
package org.apache.sling.scripting.sightly.api;

import java.util.Dictionary;

import org.osgi.service.component.ComponentContext;

/**
 * Component-based implementation for extensions
 */
public abstract class RuntimeExtensionComponent implements RuntimeExtension {

    public static final String SCR_PROP_NAME = "org.apache.sling.scripting.sightly.rtextension.name";

    private String name;

    @Override
    public String name() {
        return name;
    }

    @SuppressWarnings("UnusedDeclaration")
    protected void activate(ComponentContext componentContext) {
        Dictionary properties = componentContext.getProperties();
        name = (String) properties.get(SCR_PROP_NAME);
    }

    protected void checkArgumentCount(Object[] arguments, int count) {
        if (arguments.length != count) {
            throw new RuntimeExtensionException(String.format("Extension %s requires %d arguments", name(), count));
        }
    }

    protected void checkArguments(Object[] arguments, Class<?> ... classes) {
        checkArgumentCount(arguments, classes.length);
        for (int i = 0; i < arguments.length; i++) {
            Object arg = arguments[i];
            Class<?> cls = classes[i];
            if (!(cls.isAssignableFrom(arg.getClass()))) {
                throw new RuntimeExtensionException(String.format("Argument on position %d is not of expected type %s",
                        i, cls.getCanonicalName()));
            }
        }
    }
}
