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
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * The default transformer transforms:
 * - file resources containing a bundle into OSGI bundle resources
 * - properties resources with specific extensions into OSGi configurations
 */
public class DefaultTransformer
    implements InternalService, ResourceTransformer {

    /**
     * @see org.apache.sling.installer.core.impl.InternalService#init(org.osgi.framework.BundleContext)
     */
    public void init(final BundleContext bctx) {
        // nothing to do
    }

    /**
     * @see org.apache.sling.installer.core.impl.InternalService#deactivate()
     */
    public void deactivate() {
        // nothing to do
    }

    /**
     * @see org.apache.sling.installer.core.impl.InternalService#getDescription()
     */
    public String getDescription() {
        return "Apache Sling Installer - Default Resource Transformer";
    }

    /**
     * @see org.apache.sling.installer.api.tasks.ResourceTransformer#transform(org.apache.sling.installer.api.tasks.RegisteredResource)
     */
    public TransformationResult[] transform(final RegisteredResource resource) {
        if ( resource.getType().equals(InstallableResource.TYPE_FILE) ) {
            return checkBundle(resource);
        } else if ( resource.getType().equals(InstallableResource.TYPE_PROPERTIES) ) {
            return checkConfiguration(resource);
        }
        return null;
    }

    /**
     * Check if the registered resource is a bundle.
     * @return
     */
    private TransformationResult[] checkBundle(final RegisteredResource resource) {
        try {
            final Manifest m = getManifest(resource.getInputStream());
            if (m != null) {
                final String sn = m.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
                if (sn != null) {
                    final String v = m.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
                    if (v != null) {
                        final Map<String, Object> attr = new HashMap<String, Object>();
                        attr.put(Constants.BUNDLE_SYMBOLICNAME, sn);
                        attr.put(Constants.BUNDLE_VERSION, v.toString());

                        // check for activation policy
                        final String actPolicy = m.getMainAttributes().getValue(Constants.BUNDLE_ACTIVATIONPOLICY);
                        if ( Constants.ACTIVATION_LAZY.equals(actPolicy) ) {
                            attr.put(Constants.BUNDLE_ACTIVATIONPOLICY, actPolicy);
                        }

                        final TransformationResult tr = new TransformationResult();
                        tr.setId(sn);
                        tr.setResourceType(InstallableResource.TYPE_BUNDLE);
                        tr.setAttributes(attr);

                        return new TransformationResult[] {tr};
                    }
                }
            }
        } catch (final IOException ignore) {
            // ignore
        }
        return null;
    }

    /**
     * Check if the registered resource is a configuration
     * @param resource The resource
     */
    private TransformationResult[] checkConfiguration(final RegisteredResource resource) {
        final String url = resource.getURL();
        String lastIdPart = url;
        final int pos = lastIdPart.lastIndexOf('/');
        if ( pos != -1 ) {
            lastIdPart = lastIdPart.substring(pos + 1);
        }

        final String pid;
        // remove extension if known
        if ( isConfigExtension(getExtension(lastIdPart)) ) {
            final int lastDot = lastIdPart.lastIndexOf('.');
            pid = lastIdPart.substring(0, lastDot);
        } else {
            pid = lastIdPart;
        }

        // split pid and factory pid alias
        final String factoryPid;
        final String configPid;
        int n = pid.indexOf('-');
        if (n > 0) {
            configPid = pid.substring(n + 1);
            factoryPid = pid.substring(0, n);
        } else {
            factoryPid = null;
            configPid = pid;
        }

        final Map<String, Object> attr = new HashMap<String, Object>();

        attr.put(Constants.SERVICE_PID, configPid);
        // Factory?
        if (factoryPid != null) {
            attr.put(ConfigurationAdmin.SERVICE_FACTORYPID, factoryPid);
        }

        final TransformationResult tr = new TransformationResult();
        final String id = (factoryPid == null ? "" : factoryPid + ".") + configPid;
        tr.setId(id);
        tr.setResourceType(InstallableResource.TYPE_CONFIG);
        tr.setAttributes(attr);

        return new TransformationResult[] {tr};
    }

    /**
     * Read the manifest from supplied input stream, which is closed before return.
     */
    private Manifest getManifest(final InputStream ins) throws IOException {
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

    /**
     * Compute the extension
     */
    private static String getExtension(String url) {
        final int pos = url.lastIndexOf('.');
        return (pos < 0 ? "" : url.substring(pos+1));
    }

    private static boolean isConfigExtension(String extension) {
        if ( extension.equals("cfg")
                || extension.equals("config")
                || extension.equals("xml")
                || extension.equals("properties")) {
            return true;
        }
        return false;
    }
}
