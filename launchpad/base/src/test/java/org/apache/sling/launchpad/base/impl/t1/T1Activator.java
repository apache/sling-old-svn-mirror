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
package org.apache.sling.launchpad.base.impl.t1;

import java.io.IOException;
import java.net.URL;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * The <code>T1Activator</code> uses the framework to connect to an URL
 * which causes the Felix.getBundle(Class) method to be called to resolve
 * the framework from the calling class.
 *
 * If SlingFelix does not overwrite the Felix.getBundle(Class) method
 * the URLHandlers class cannot find the method and thus the
 * {@link #run()} method throws an exception.
 */
public class T1Activator implements BundleActivator, Runnable {

    private String url;

    public void start(BundleContext context) throws Exception {
        this.url = context.getBundle().getEntry("META-INF/MANIFEST.MF").toString();
        context.registerService(Runnable.class.getName(), this, null);
    }

    public void stop(BundleContext context) throws Exception {
    }

    public void run() {
        try {
            new URL(url).openConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
