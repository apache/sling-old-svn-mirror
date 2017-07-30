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
package org.apache.sling.installer.factory.packages.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.vault.fs.io.Archive;
import org.apache.jackrabbit.vault.fs.io.ImportOptions;
import org.apache.jackrabbit.vault.fs.io.ZipStreamArchive;
import org.apache.jackrabbit.vault.packaging.Dependency;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.ChangeStateTask;
import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.api.tasks.RetryHandler;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.framework.Version;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** The package transformer:
 * <ul>
 * <li>detects content packages (ResourceTransformer)
 * <li>and creates tasks for installing / removing of content packages
 * </ul>
*/
@Component(service = { ResourceTransformer.class, InstallTaskFactory.class })
@Designate(ocd=PackageTransformerConfiguration.class)
public class PackageTransformer implements ResourceTransformer, InstallTaskFactory {

    /** The attribute holding the package id. */
    private static final String ATTR_PCK_ID = "package-id";

    /** The resource types for packages. */
    private static final String RESOURCE_TYPE_REGULAR = "content-package";
    private static final String RESOURCE_TYPE_HOLLOW = "content-package-hollow";

    /** The logger. */
    private static final Logger logger = LoggerFactory.getLogger(PackageTransformer.class);

    @Reference
    private SlingRepository repository;

    @Reference
    private Packaging pkgSvc;

    @Reference
    private RetryHandler retryHandler;

    private PackageTransformerConfiguration configuration;

    @Activate
    private void activate(final PackageTransformerConfiguration configuration) {
        this.configuration = configuration;
    }

    /** @see org.apache.sling.installer.api.tasks.ResourceTransformer#transform(org.apache.sling.installer.api.tasks.RegisteredResource) */
    @Override
    public TransformationResult[] transform(final RegisteredResource resource) {
        if (resource.getType().equals(InstallableResource.TYPE_FILE)) {
            return checkForPackage(resource);
        }
        return null;
    }

    /** Check if the resource is a content package
     * 
     * @param resource The resource
     * @return {@code null} if not a content package, a result otherwise */
    private TransformationResult[] checkForPackage(final RegisteredResource resource) {
        // first check if this is a zip archive
        try (final ZipInputStream zin = new ZipInputStream(new BufferedInputStream(resource.getInputStream()))) {
            if (zin.getNextEntry() == null) {
                return null;
            }
        } catch (final IOException ioe) {
            logger.debug("Unable to read resource.", ioe);
            return null;
        }

        Session session = null;
        JcrPackage pck = null;
        try {
            // create an admin session
            session = repository.loginAdministrative(null);

            final JcrPackageManager pckMgr = pkgSvc.getPackageManager(session);
            final TransformationResult tr = new TransformationResult();
            if (configuration.shouldCreateHollowPackages()) {
                tr.setResourceType(RESOURCE_TYPE_HOLLOW);
                // TODO: getting the real package id, currently this requires opening and extracting the archive (and copying files)
                // Solved with https://issues.apache.org/jira/browse/JCRVLT-187
                // for now we just take the filename from the URL
                tr.setId(extractNameFromUrl(resource.getURL()));
            } else {
                pck = pckMgr.upload(resource.getInputStream(), true, true);
                if (pck.isValid()) {
                    final PackageId pid = pck.getDefinition().getId();
                    final Map<String, Object> attrs = new HashMap<String, Object>();
                    attrs.put(ATTR_PCK_ID, pid.toString());
                    tr.setId(pid.getGroup() + ':' + pid.getName());
                    tr.setResourceType(RESOURCE_TYPE_REGULAR);
                    tr.setAttributes(attrs);

                    // version
                    final String version = pid.getVersionString();
                    if (version.length() > 0) {
                        tr.setVersion(new Version(cleanupVersion(version)));
                    }
                } else {
                    logger.warn("Package from resource {} is invalid", resource);
                    return null;
                }
            }
            return new TransformationResult[] { tr };
        } catch (final Exception ioe) {
            logger.debug("Unable to check content package " + resource.getURL(), ioe);
        } finally {
            if (pck != null) {
                pck.close();
            }
            if (session != null) {
                session.logout();
            }
        }
        return null;
    }

    /**
     * Converts all separators to the Unix separator of forward slash.
     *
     * @param path  the path to be changed, null ignored
     * @return the updated path
     */
    private static String separatorsToUnix(String path) {
        if (path == null || path.indexOf('\\') == -1) {
            return path;
        }
        return path.replace('\\', '/');
    }

    private static String extractNameFromUrl(String url) {
        String lastIdPart = separatorsToUnix(url);;
        final int pos = lastIdPart.lastIndexOf('/');
        if ( pos != -1 ) {
            lastIdPart = lastIdPart.substring(pos + 1);
        }
        return lastIdPart;
    }

    /** @see org.apache.sling.installer.api.tasks.InstallTaskFactory#createTask(org.apache.sling.installer.api.tasks.TaskResourceGroup) */
    @Override
    public InstallTask createTask(final TaskResourceGroup toActivate) {
        final TaskResource resource = toActivate.getActiveResource();
        if (resource == null) {
            logger.warn("The given resource to createTask is null");
            return null;
        }

        InstallTask task = null;
        switch (resource.getType()) {
        case RESOURCE_TYPE_REGULAR:
            // extract the package id
            final String id = (String) resource.getAttribute(ATTR_PCK_ID);
            final PackageId pkgId = PackageId.fromString(id);
            if (pkgId == null) {
                logger.error("Error during processing of {}: Package id is wrong/null.", resource);
                return new ChangeStateTask(toActivate, ResourceState.IGNORED);
            }
            if (resource.getState() == ResourceState.INSTALL) {
                task = new InstallPackageTask(pkgId, toActivate);
            } else {
                task = new UninstallPackageTask(pkgId, toActivate);
            }
            break;
        case RESOURCE_TYPE_HOLLOW:
            if (resource.getState() == ResourceState.INSTALL) {
                task = new InstallHollowPackageTask(toActivate);
            } else {
                // most probably uninstallation is not successful because this has also been installed as hollow-package!
                logger.info("Do not uninstall {}: Hollow-Packages cannot be uninstalled.", resource);
                return new ChangeStateTask(toActivate, ResourceState.IGNORED);
            }
            break;
        default:
            logger.error("Unupported type of {}: {}.", resource, resource.getType());
            task = new ChangeStateTask(toActivate, ResourceState.IGNORED);
        }
        return task;
    }

    private abstract class AbstractPackageInstallTask extends InstallTask {

        protected final String name;

        public AbstractPackageInstallTask(final String name, final TaskResourceGroup erl) {
            super(erl);
            this.name = name;
        }

        @Override
        public void execute(final InstallationContext ctx) {
            final TaskResource resource = this.getResource();

            // now check the dependencies
            Session session = null;
            JcrPackage pkg = null;
            try {
                session = repository.loginAdministrative(null);
                final JcrPackageManager pkgMgr = pkgSvc.getPackageManager(session);

                doExecute(ctx, pkgMgr, resource);
            } catch (final Exception e) {
                String message = MessageFormat.format("Error while processing {0} content package task of {1} due to {2}, no retry.", name, resource,
                        e.getLocalizedMessage());
                logger.error(message, e);
                this.setFinishedState(ResourceState.IGNORED, null, message);
            } finally {
                if (pkg != null) {
                    pkg.close();
                }
                if (session != null) {
                    session.logout();
                }
            }
        }

        protected abstract void doExecute(final InstallationContext ctx, final JcrPackageManager pkgMgr, final TaskResource resource)
                throws RepositoryException, PackageException, IOException;
    }

    /** Task for installing a package. */
    private class InstallPackageTask extends AbstractPackageInstallTask {
        private final PackageId pkgId;
        
        public InstallPackageTask(final PackageId pkgId, final TaskResourceGroup erl) {
            super("install", erl);
            this.pkgId = pkgId;
        }

        @Override
        protected void doExecute(final InstallationContext ctx, final JcrPackageManager pkgMgr, final TaskResource resource)
                throws RepositoryException, PackageException, IOException {

            // open package
            JcrPackage pkg = pkgMgr.open(pkgId);
            try {
                if (pkg == null) {
                    String message = MessageFormat.format("Error during installation of {0}: Package {1} missing.", resource, pkgId);
                    logger.error(message);
                    this.setFinishedState(ResourceState.IGNORED, null, message);
                    return;
                }

                // check if package was installed previously by some other means (or even by a previous run of the installer)
                if (pkg.isInstalled()) {
                    String message = MessageFormat.format("Package {0} was installed externally. Marking as installed.", pkgId);
                    logger.info(message);
                    this.setFinishedState(ResourceState.INSTALLED, null, message);
                    return;
                }

                // check if dependencies are installed
                for (final Dependency d : pkg.getDefinition().getDependencies()) {
                    if (pkgMgr.resolve(d, true) == null) {
                        logger.info("Delaying installation of {} due to missing dependency {}.", pkgId, d);
                        return;
                    }
                }

                // finally, install package
                final ImportOptions opts = new ImportOptions();
                if (configuration.shouldCreateSnapshots()) {
                    pkg.install(opts);
                    ctx.log("Content package installed: {}", resource);
                } else {
                    pkg.extract(opts);
                    ctx.log("Content package extracted: {}", resource);
                }

                setFinishedState(ResourceState.INSTALLED);

                // notify retry handler to install dependent packages.
                retryHandler.scheduleRetry();

            } finally {
                if (pkg != null) {
                    pkg.close();
                }
            }
        }

        @Override
        public String getSortKey() {
            return "25-" + getResource().getEntityId();
        }
    }

    private final class InstallHollowPackageTask extends AbstractPackageInstallTask {

        public InstallHollowPackageTask(TaskResourceGroup erl) {
            super("install-hollow", erl);
        }

        @Override
        protected void doExecute(InstallationContext ctx, JcrPackageManager pkgMgr, TaskResource resource)
                throws RepositoryException, PackageException, IOException {
            Archive archive = new ZipStreamArchive(resource.getInputStream());
            JcrPackage oldPackage = null;
            try {
                archive.open(false);
                // we always have to do that, as there is no possibility to figure out whether the same package has already been installed
                // https://issues.apache.org/jira/browse/JCRVLT-188
                final ImportOptions opts = new ImportOptions();
                pkgMgr.extract(archive, opts, true);
            } finally {
                archive.close();
                if (oldPackage != null) {
                    oldPackage.close();
                }
            }
        }

        @Override
        public String getSortKey() {
            return "35-" + getResource().getEntityId();
        }

    }

    /** Task for uninstalling a package. */
    private final class UninstallPackageTask extends AbstractPackageInstallTask {

        private PackageId pkgId;

        public UninstallPackageTask(final PackageId pkgId, final TaskResourceGroup erl) {
            super("uninstall", erl);
            this.pkgId = pkgId;
        }

        @Override
        public String getSortKey() {
            return "55-" + getResource().getEntityId();
        }

        @Override
        protected void doExecute(InstallationContext ctx, JcrPackageManager pkgMgr, TaskResource resource)
                throws RepositoryException, PackageException, IOException {
            JcrPackage pkg = pkgMgr.open(this.pkgId);
            try {
                if (pkg != null) {
                    final ImportOptions opts = new ImportOptions();
                    pkg.uninstall(opts);
                }
            } finally {
                if (pkg != null) {
                    pkg.close();
                }
            }
            ctx.log("Uninstalled content package {}", getResource());
            setFinishedState(ResourceState.UNINSTALLED);
            retryHandler.scheduleRetry();
        }
    }

    private static final Pattern FUZZY_VERSION = Pattern.compile("(\\d+)(\\.(\\d+)(\\.(\\d+))?)?([^a-zA-Z0-9](.*))?",
            Pattern.DOTALL);

    /** Clean up version parameters. Other builders use more fuzzy definitions of the version syntax. This method cleans up such a version
     * to match an OSGi version.
     *
     * @param version The version string to clean up
     * @return the clean version */
    private static String cleanupVersion(final String version) {
        final StringBuilder result = new StringBuilder();
        final Matcher m = FUZZY_VERSION.matcher(version);
        if (m.matches()) {
            final String major = m.group(1);
            final String minor = m.group(3);
            final String micro = m.group(5);
            final String qualifier = m.group(7);

            if (major != null) {
                result.append(major);
                if (minor != null) {
                    result.append(".");
                    result.append(minor);
                    if (micro != null) {
                        result.append(".");
                        result.append(micro);
                        if (qualifier != null) {
                            result.append(".");
                            cleanupModifier(result, qualifier);
                        }
                    } else if (qualifier != null) {
                        result.append(".0.");
                        cleanupModifier(result, qualifier);
                    } else {
                        result.append(".0");
                    }
                } else if (qualifier != null) {
                    result.append(".0.0.");
                    cleanupModifier(result, qualifier);
                } else {
                    result.append(".0.0");
                }
            }
        } else {
            result.append("0.0.0.");
            cleanupModifier(result, version);
        }
        return result.toString();
    }

    private static void cleanupModifier(final StringBuilder result, final String modifier) {
        for (int i = 0; i < modifier.length(); i++) {
            char c = modifier.charAt(i);
            if ((c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z')
                    || c == '_'
                    || c == '-') {
                result.append(c);
            } else {
                result.append('_');
            }
        }
    }
}
