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
package org.apache.sling.caconfig.management.impl.console;

import java.io.PrintWriter;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Print additional configuration per service.
 * @param <T> Service type
 */
interface ServiceConfigurationPrinter<T> {
    
    /**
     * Bullet character
     */
    String BULLET = "- ";

    /**
     * Indentation 1 step
     */
    String INDENT = "    ";

    /**
     * Indentation 2 steps
     */
    String INDENT_2 = INDENT + INDENT;

    /**
     * Indentation 3 steps
     */
    String INDENT_3 = INDENT_2 + INDENT;
    
    /**
     * Print configuration
     * @param printWriter Print writer
     * @param serviceReference Service reference
     * @param bundleContext Bundle context
     */
    void printConfiguration(PrintWriter printWriter, ServiceReference<T> serviceReference, BundleContext bundleContext);
    
}
