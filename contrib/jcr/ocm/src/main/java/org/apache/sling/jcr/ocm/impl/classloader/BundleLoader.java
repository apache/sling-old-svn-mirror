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
package org.apache.sling.jcr.ocm.impl.classloader;

import org.osgi.framework.Bundle;


/**
 * The <code>ClassLoaderLoader</code> TODO
 */
public class BundleLoader implements Loader {

    private Bundle bundle;

    BundleLoader(Bundle bundle) {
        this.bundle = bundle;
    }

    /**
     * @see org.apache.sling.jcr.ocm.impl.classloader.Loader#loadClass(java.lang.String)
     */
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            // to prevent nasty messages, we check for the class resource
            // before actually accessing the class
            String resource = name.replace('.', '/') + ".class";
            if (this.bundle.getResource(resource) != null) {
                return this.bundle.loadClass(name);
            }

            // fail if the bundle does not have the resource
            throw new ClassNotFoundException(name);
        } catch (IllegalStateException ise) {
            // TODO: log
            throw new ClassNotFoundException(name, ise);
        }
    }

    /**
     * @see org.apache.sling.jcr.ocm.impl.classloader.Loader#getLoader()
     */
    public Object getLoader() {
        return this.bundle;
    }
}
