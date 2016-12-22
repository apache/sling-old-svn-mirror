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
package org.apache.sling.testing.mock.caconfig;

import org.apache.sling.testing.mock.sling.context.SlingContextImpl;
import org.osgi.annotation.versioning.ProviderType;

/**
 * Helps setting up a mock environment for Context-Aware Configuration.
 */
@ProviderType
public final class MockContextAwareConfig {
    
    private MockContextAwareConfig() {
        // static methods only
    }

    /**
     * Search classpath for given class names to scan for and register all classes with @Configuration annotation.
     * @param context Sling context
     * @param classNames Java class names
     */
    public static void registerAnnotationClasses(SlingContextImpl context, String... classNames) {
        ConfigurationMetadataUtil.registerAnnotationClasses(context.bundleContext(), classNames);
    }

    /**
     * Search classpath for given class names to scan for and register all classes with @Configuration annotation.
     * @param context Sling context
     * @param classes Java classes
     */
    public static void registerAnnotationClasses(SlingContextImpl context, Class... classes) {
        ConfigurationMetadataUtil.registerAnnotationClasses(context.bundleContext(), classes);
    }

}
