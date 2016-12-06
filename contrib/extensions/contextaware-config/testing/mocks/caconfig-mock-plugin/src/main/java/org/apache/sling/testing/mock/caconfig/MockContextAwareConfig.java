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

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.framework.BundleContext;

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
     * @param bundleContext Bundle context
     * @param classNames Java class names
     */
    public static void registerAnnotationClasses(BundleContext bundleContext, String... classNames) {
        ConfigurationMetadataUtil.registerAnnotationClasses(bundleContext, classNames);
    }

    /**
     * Search classpath for given class names to scan for and register all classes with @Configuration annotation.
     * @param bundleContext Bundle context
     * @param classNames Java class names
     */
    public static void registerAnnotationClasses(BundleContext bundleContext, Class... classes) {
        ConfigurationMetadataUtil.registerAnnotationClasses(bundleContext, classes);
    }

}
