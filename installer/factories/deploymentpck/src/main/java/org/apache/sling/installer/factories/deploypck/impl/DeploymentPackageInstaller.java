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
package org.apache.sling.installer.factories.deploypck.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.osgi.framework.Version;
import org.osgi.service.deploymentadmin.DeploymentAdmin;

/**
 * This is an extension for the OSGi installer
 * It listens for files ending with ".dp" and installs them through
 * the deployment package admin.
 */
public class DeploymentPackageInstaller
    implements ResourceTransformer, InstallTaskFactory {

    public static final String DEPLOYMENTPACKAGE_SYMBOLICMAME = "DeploymentPackage-SymbolicName";
    public static final String DEPLOYMENTPACKAGE_VERSION = "DeploymentPackage-Version";

    private static final String TYPE_DP = "dp";

    private final DeploymentAdmin deploymentAdmin;

    public DeploymentPackageInstaller(final DeploymentAdmin dpA) {
        this.deploymentAdmin = dpA;
    }

    /**
     * @see org.apache.sling.installer.api.tasks.ResourceTransformer#transform(org.apache.sling.installer.api.tasks.RegisteredResource)
     */
    public TransformationResult[] transform(final RegisteredResource resource) {
        if ( resource.getType().equals(InstallableResource.TYPE_FILE) ) {
            try {
                final Manifest m = getManifest(resource.getInputStream());
                if (m != null) {
                    final String sn = m.getMainAttributes().getValue(DEPLOYMENTPACKAGE_SYMBOLICMAME);
                    if (sn != null) {
                        final String v = m.getMainAttributes().getValue(DEPLOYMENTPACKAGE_VERSION);
                        if (v != null) {
                            final Map<String, Object> attr = new HashMap<String, Object>();
                            attr.put(DEPLOYMENTPACKAGE_SYMBOLICMAME, sn);

                            final TransformationResult tr = new TransformationResult();
                            tr.setId(sn);
                            tr.setVersion(new Version(v));
                            tr.setResourceType(TYPE_DP);
                            tr.setAttributes(attr);

                            return new TransformationResult[] {tr};
                        }
                    }
                }
            } catch (final IOException ignore) {
                // ignore
            }
        }
        return null;
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
                    } catch (final IOException ignore) {
                    }
                } else {
                    try {
                        ins.close();
                    } catch (final IOException ignore) {
                    }
                }
            }
        }
        return result;
    }

    /**
     * @see org.apache.sling.installer.api.tasks.InstallTaskFactory#createTask(org.apache.sling.installer.api.tasks.TaskResourceGroup)
     */
    public InstallTask createTask(final TaskResourceGroup toActivate) {
        if ( toActivate.getActiveResource().getType().equals(TYPE_DP) ) {
            return new DeployPckTask(toActivate, this.deploymentAdmin);
        }
        return null;
    }
}
