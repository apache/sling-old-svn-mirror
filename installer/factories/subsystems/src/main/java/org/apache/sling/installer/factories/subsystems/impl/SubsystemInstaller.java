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
package org.apache.sling.installer.factories.subsystems.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.ChangeStateTask;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.Version;
import org.osgi.service.subsystem.Subsystem;
import org.osgi.service.subsystem.Subsystem.State;
import org.osgi.service.subsystem.SubsystemConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an extension for the OSGi installer
 * It listens for files ending with ".esa" and a proper subsystem manifest.
 * Though subsystems does not require a complete manifest, the installer supports
 * only subsystems with the basic info (name and version).
 *
 * As subsystems currently do not support an update, an uninstall/install is done
 * instead - which will lose bundle private data, bound configurations etc.
 */
public class SubsystemInstaller
    implements ResourceTransformer, InstallTaskFactory {

    private static final String TYPE_SUBSYSTEM = "esa";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final Subsystem rootSubsystem;

    private final BundleContext bundleContext;

    public SubsystemInstaller(final Subsystem root, final BundleContext bundleContext) {
        this.rootSubsystem = root;
        this.bundleContext = bundleContext;
    }

    /**
     * @see org.apache.sling.installer.api.tasks.ResourceTransformer#transform(org.apache.sling.installer.api.tasks.RegisteredResource)
     */
    public TransformationResult[] transform(final RegisteredResource resource) {
        if ( resource.getType().equals(InstallableResource.TYPE_FILE) ) {
            if ( resource.getURL().endsWith("." + TYPE_SUBSYSTEM) ) {
                logger.info("Found potential subsystem resource {}", resource);
                final SubsystemInfo headers = readSubsystemHeaders(resource);
                if ( headers != null ) {
                    // check the version for validity
                    boolean validVersion = true;
                    try {
                        new Version(headers.version);
                    } catch (final IllegalArgumentException iae) {
                        logger.info("Rejecting subsystem {} from {} due to invalid version information: {}.",
                                new Object[] {headers.symbolicName, resource, headers.version});
                        validVersion = false;
                    }
                    if ( validVersion ) {
                        final Map<String, Object> attr = new HashMap<String, Object>();
                        attr.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, headers.symbolicName);
                        attr.put(SubsystemConstants.SUBSYSTEM_VERSION, headers.version);

                        final TransformationResult tr = new TransformationResult();
                        tr.setId(headers.symbolicName);
                        tr.setResourceType(TYPE_SUBSYSTEM);
                        tr.setAttributes(attr);
                        tr.setVersion(new Version(headers.version));

                        return new TransformationResult[] {tr};
                    }

                } else {
                    logger.info("Subsystem resource does not have required headers.");
                }
            }
        }
        return null;
    }

    /**
     * Check that the required attributes are available.
     * This is just a sanity check
     */
    private SubsystemInfo checkResource(final TaskResourceGroup toActivate) {
        final TaskResource rsrc = toActivate.getActiveResource();

        SubsystemInfo result = null;
        final String symbolicName = (String) rsrc.getAttribute(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME);
        if ( symbolicName == null ) {
            logger.error("Subsystem resource is missing symbolic name {}", rsrc);
        } else {
            final String version = (String)rsrc.getAttribute(SubsystemConstants.SUBSYSTEM_VERSION);
            if ( version == null ) {
                logger.error("Subsystem resource is missing version {}", rsrc);
            } else {
                // check the version for validity
                boolean validVersion = true;
                try {
                    new Version(version);
                } catch (final IllegalArgumentException iae) {
                    logger.info("Rejecting subsystem {} from {} due to invalid version information: {}.",
                            new Object[] {symbolicName, rsrc, version});
                    validVersion = false;
                }
                if ( validVersion ) {
                    result = new SubsystemInfo();
                    result.symbolicName = symbolicName;
                    result.version = version;
                }
            }
        }
        return result;
    }

    private ServiceReference<Subsystem> getSubsystemReference(final String symbolicName) {
        // search a subsystem with the symbolic name
        ServiceReference<Subsystem> ref = null;
        try {
            final Collection<ServiceReference<Subsystem>> refs = this.bundleContext.getServiceReferences(Subsystem.class, "(subsystem.symbolicName=" + symbolicName + ")");
            if ( refs.size() > 0 ) {
                ref = refs.iterator().next();
            }
        } catch (final InvalidSyntaxException e) {
            logger.error("Problem searching for subsystem with symbolic name " + symbolicName, e);
        }
        return ref;
    }

    /**
     * @see org.apache.sling.installer.api.tasks.InstallTaskFactory#createTask(org.apache.sling.installer.api.tasks.TaskResourceGroup)
     */
    public InstallTask createTask(final TaskResourceGroup toActivate) {
        final InstallTask result;

        final TaskResource rsrc = toActivate.getActiveResource();
        if ( rsrc.getType().equals(TYPE_SUBSYSTEM) ) {

            // check if the required info is available
            final SubsystemInfo info = checkResource(toActivate);
            if ( info == null ) {
                // ignore as info is missing
                result = new ChangeStateTask(toActivate, ResourceState.IGNORED);
            } else {
                // search a subsystem with the symbolic name
                final ServiceReference<Subsystem> ref = this.getSubsystemReference(info.symbolicName);

                final Subsystem currentSubsystem = (ref != null ? this.bundleContext.getService(ref) : null);
                try {
                    final Version newVersion = new Version(info.version);
                    final Version oldVersion = (ref == null ? null : (Version)ref.getProperty("subsystem.version"));

                    // Install
                    if ( rsrc.getState() == ResourceState.INSTALL ) {
                        if ( oldVersion != null ) {

                            final int compare = oldVersion.compareTo(newVersion);
                            if (compare < 0) {
                                // installed version is lower -> update
                                result = new UpdateSubsystemTask(toActivate, this.bundleContext, ref, this.rootSubsystem);
                            } else if ( compare == 0 && isSnapshot(newVersion) ) {
                                // same version but snapshot -> update
                                result = new UpdateSubsystemTask(toActivate, this.bundleContext, ref, this.rootSubsystem);
                            } else if ( compare == 0 && currentSubsystem != null && currentSubsystem.getState() != State.ACTIVE ) {
                                // try to start the version
                                result = new StartSubsystemTask(toActivate, currentSubsystem);
                            } else {
                                logger.info("{} is not installed, subsystem with same or higher version is already installed: {}", info, newVersion);
                                result = new ChangeStateTask(toActivate, ResourceState.IGNORED);
                            }
                        } else {
                            result = new InstallSubsystemTask(toActivate, this.rootSubsystem);
                        }

                    // Uninstall
                    } else if ( rsrc.getState() == ResourceState.UNINSTALL ) {
                        if ( oldVersion == null ) {
                            logger.error("Nothing to uninstall. {} is currently not installed.", info);
                            result = new ChangeStateTask(toActivate, ResourceState.IGNORED);
                        } else {

                            final int compare = oldVersion.compareTo(newVersion);
                            if ( compare == 0 ) {
                                result = new UninstallSubsystemTask(toActivate, this.bundleContext, ref);
                            } else {
                                logger.error("Nothing to uninstall. {} is currently not installed, different version is installed {}", info, oldVersion);
                                result = new ChangeStateTask(toActivate, ResourceState.IGNORED);
                            }
                        }
                    } else {
                        result = null;
                    }
                } finally {
                    if ( currentSubsystem != null ) {
                        this.bundleContext.ungetService(ref);
                    }
                }
            }
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Read the manifest from supplied input stream, which is closed before return.
     */
    private static Manifest getManifest(final RegisteredResource rsrc, final Logger logger)
    throws IOException {
        final InputStream ins = rsrc.getInputStream();

        Manifest result = null;

        if ( ins != null ) {
            ZipInputStream jis = null;
            try {
                jis = new ZipInputStream(ins);

                ZipEntry entry;

                while ( (entry = jis.getNextEntry()) != null ) {
                    if (entry.getName().equals("OSGI-INF/SUBSYSTEM.MF") ) {
                        result = new Manifest(jis);
                    }
                }

            } finally {

                // close the jar stream or the input stream, if the jar
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

    final static public class SubsystemInfo {
        public String symbolicName;
        public String version;
        @Override

        public String toString() {
            return "Subsystem[symbolicName=" + symbolicName + ", version="
                    + version + "]";
        }

    }

    /**
     * Read the subsystem info from the manifest (if available)
     */
    private SubsystemInfo readSubsystemHeaders(final RegisteredResource resource) {
        try {
            final Manifest m = SubsystemInstaller.getManifest(resource, logger);
            if (m != null) {
                final String sn = m.getMainAttributes().getValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME);
                if (sn != null) {
                    final String v = m.getMainAttributes().getValue(SubsystemConstants.SUBSYSTEM_VERSION);
                    final int paramPos = sn.indexOf(';');
                    final String symbolicName = (paramPos == -1 ? sn : sn.substring(0, paramPos));
                    final SubsystemInfo headers = new SubsystemInfo();
                    headers.symbolicName = symbolicName;
                    headers.version = v;

                    // if no version is specified, use default version
                    if ( headers.version == null ) {
                        headers.version = "0.0.0.0";
                    }
                    return headers;
                }
            }
        } catch (final IOException ignore) {
            // ignore
        }
        return null;
    }

    private static final String MAVEN_SNAPSHOT_MARKER = "SNAPSHOT";

    /**
     * Check if the version is a snapshot version
     */
    public static boolean isSnapshot(final Version v) {
        return v.toString().indexOf(MAVEN_SNAPSHOT_MARKER) >= 0;
    }
}
