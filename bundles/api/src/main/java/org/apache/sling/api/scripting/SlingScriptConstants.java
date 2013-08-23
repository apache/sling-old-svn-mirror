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
package org.apache.sling.api.scripting;

/**
 * Some constants for the scripting.
 * <p>
 * This class is not intended to be extended or instantiated because it just
 * provides constants not intended to be overwritten.
 *
 * @since 2.0.6
 */
public class SlingScriptConstants {

    /**
     * The name of the script context attribute holding the {@link org.apache.sling.api.resource.ResourceResolver} which
     * has been used to resolve the script. This resource resolver can be used by the
     * script engines to further locate scripts (for includes etc.).
     * The value is set in the {@link SlingScriptConstants#SLING_SCOPE} of the script context.
     * @since 2.0.6
     */
    public static final String ATTR_SCRIPT_RESOURCE_RESOLVER = "org.apache.sling.api.scripting.ScriptResourceResolver";

    /**
     * The name of the script scope holding the {@link #ATTR_SCRIPT_RESOURCE_RESOLVER}.
     * @since 2.0.6
     */
    public static final int SLING_SCOPE = -314;

    /**
     * The topic for the OSGi event which is sent when a script engine factory has been added.
     * The event contains at least the {@link #PROPERTY_SCRIPT_ENGINE_FACTORY_NAME},
     * {@link #PROPERTY_SCRIPT_ENGINE_FACTORY_VERSION},
     * {@link #PROPERTY_SCRIPT_ENGINE_FACTORY_EXTENSIONS},
     * {@link #PROPERTY_SCRIPT_ENGINE_FACTORY_LANGUAGE_NAME},
     * {@link #PROPERTY_SCRIPT_ENGINE_FACTORY_LANGUAGE_VERSION},
     * and {@link #PROPERTY_SCRIPT_ENGINE_FACTORY_MIME_TYPES} poperties.
     * @since 2.0.6
     */
    public static final String TOPIC_SCRIPT_ENGINE_FACTORY_ADDED = "javax/script/ScriptEngineFactory/ADDED";

    /**
     * The topic for the OSGi event which is sent when a script engine factory has been removed.
     * The event contains at least the {@link #PROPERTY_SCRIPT_ENGINE_FACTORY_NAME},
     * {@link #PROPERTY_SCRIPT_ENGINE_FACTORY_VERSION},
     * {@link #PROPERTY_SCRIPT_ENGINE_FACTORY_EXTENSIONS},
     * {@link #PROPERTY_SCRIPT_ENGINE_FACTORY_LANGUAGE_NAME},
     * {@link #PROPERTY_SCRIPT_ENGINE_FACTORY_LANGUAGE_VERSION},
     * and {@link #PROPERTY_SCRIPT_ENGINE_FACTORY_MIME_TYPES} poperties.
     * @since 2.0.6
     */
    public static final String TOPIC_SCRIPT_ENGINE_FACTORY_REMOVED = "javax/script/ScriptEngineFactory/REMOVED";

    /**
     * The event property listing the script engine factory name. The value is a string.
     * @since 2.0.6
     */
    public static final String PROPERTY_SCRIPT_ENGINE_FACTORY_NAME = "engineName";

    /**
     * The event property listing the script engine factory name. The value is a string.
     * @since 2.0.6
     */
    public static final String PROPERTY_SCRIPT_ENGINE_FACTORY_VERSION = "engineVersion";

    /**
     * The event property listing the script engine factory extensions. The value is
     * a string array.
     * @since 2.0.6
     */
    public static final String PROPERTY_SCRIPT_ENGINE_FACTORY_EXTENSIONS = "extensions";

    /**
     * The event property listing the script engine factory language. The value is
     * a string.
     * @since 2.0.6
     */
    public static final String PROPERTY_SCRIPT_ENGINE_FACTORY_LANGUAGE_NAME = "languageName";

    /**
     * The event property listing the script engine factory language version. The value is
     * a string.
     * @since 2.0.6
     */
    public static final String PROPERTY_SCRIPT_ENGINE_FACTORY_LANGUAGE_VERSION = "languageVersion";

    /**
     * The event property listing the script engine factory mime types. The value is
     * a string array.
     * @since 2.0.6
     */
    public static final String PROPERTY_SCRIPT_ENGINE_FACTORY_MIME_TYPES = "mimeTypes";
}
