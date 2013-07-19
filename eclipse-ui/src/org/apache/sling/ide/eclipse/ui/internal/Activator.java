/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.eclipse.ui.internal;

import org.apache.sling.ide.serialization.SerializationManager;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class Activator extends Plugin {

    public static final String PLUGIN_ID = "org.apache.sling.ide.eclipse-core";
    public static Activator INSTANCE;

    private SerializationManager serializationManager;
    private ServiceReference<SerializationManager> serializationManagerRef;

    public static Activator getDefault() {

        return INSTANCE;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);

        System.out.println("Starting with context " + context);

        serializationManagerRef = context.getServiceReference(SerializationManager.class);
        serializationManager = context.getService(serializationManagerRef);

        INSTANCE = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        INSTANCE = null;

        context.ungetService(serializationManagerRef);

        System.out.println("Stopping with context " + context);

        super.stop(context);
    }

    public SerializationManager getSerializationManager() {
        return serializationManager;
    }
}
