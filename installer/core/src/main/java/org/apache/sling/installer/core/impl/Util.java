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
package org.apache.sling.installer.core.impl;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.osgi.framework.Constants;

public class Util {

    /**
     * Set a (final) field during deserialization.
     */
    public static void setField(final Object obj, final String name, final Object value)
    throws IOException {
        try {
            final Field field = obj.getClass().getDeclaredField(name);
            if ( field == null ) {
                throw new IOException("Field " + name + " not found in class " + obj.getClass());
            }
            field.setAccessible(true);
            field.set(obj, value);
        } catch (final SecurityException e) {
            throw (IOException)new IOException().initCause(e);
        } catch (final NoSuchFieldException e) {
            throw (IOException)new IOException().initCause(e);
        } catch (final IllegalArgumentException e) {
            throw (IOException)new IOException().initCause(e);
        } catch (final IllegalAccessException e) {
            throw (IOException)new IOException().initCause(e);
        }
    }

    /**
     * Read the manifest from supplied input stream, which is closed before return.
     */
    private static Manifest getManifest(final RegisteredResource rsrc)
    throws IOException {
        final InputStream ins = rsrc.getInputStream();

        Manifest result = null;

        if ( ins != null ) {
            JarInputStream jis = null;
            try {
                jis = new JarInputStream(ins);
                result= jis.getManifest();

            } finally {

                // close the jar stream or the inputstream, if the jar
                // stream is set, we don't need to close the input stream
                // since closing the jar stream closes the input stream
                if (jis != null) {
                    try {
                        jis.close();
                    } catch (IOException ignore) {
                    }
                } else {
                    try {
                        ins.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }
        return result;
    }

    final static public class BundleHeaders {
        public String symbolicName;
        public String version;
        public String activationPolicy; // optional
    }

    /**
     * Read the bundle info from the manifest (if available)
     */
    public static BundleHeaders readBundleHeaders(final RegisteredResource resource) {
        try {
            final Manifest m = Util.getManifest(resource);
            if (m != null) {
                final String sn = m.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
                if (sn != null) {
                    final String v = m.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
                    if (v != null) {
                        final int paramPos = sn.indexOf(';');
                        final String symbolicName = (paramPos == -1 ? sn : sn.substring(0, paramPos));
                        final BundleHeaders headers = new BundleHeaders();
                        headers.symbolicName = symbolicName;
                        headers.version = v.toString();

                        // check for activation policy
                        final String actPolicy = m.getMainAttributes().getValue(Constants.BUNDLE_ACTIVATIONPOLICY);
                        if ( Constants.ACTIVATION_LAZY.equals(actPolicy) ) {
                            headers.activationPolicy = actPolicy;
                        }

                        return headers;
                    }
                }
            }
        } catch (final IOException ignore) {
            // ignore
        }
        return null;
    }
}
