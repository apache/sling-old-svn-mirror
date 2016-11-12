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
package org.apache.sling.installer.factory.model.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.OsgiInstaller;
import org.apache.sling.installer.api.tasks.InstallationContext;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.repoinit.JcrRepoInitOpsProcessor;
import org.apache.sling.provisioning.model.Artifact;
import org.apache.sling.provisioning.model.ArtifactGroup;
import org.apache.sling.provisioning.model.Configuration;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelUtility;
import org.apache.sling.provisioning.model.RunMode;
import org.apache.sling.provisioning.model.Section;
import org.apache.sling.provisioning.model.Traceable;
import org.apache.sling.provisioning.model.io.ModelArchiveReader;
import org.apache.sling.provisioning.model.io.ModelReader;
import org.apache.sling.repoinit.parser.RepoInitParser;
import org.apache.sling.repoinit.parser.RepoInitParsingException;
import org.apache.sling.repoinit.parser.operations.Operation;
import org.osgi.framework.BundleContext;

/**
 * This task installs model resources.
 */
public class InstallModelTask extends AbstractModelTask {

    private final Set<String> activeRunModes;

    private final SlingRepository repository;

    private final JcrRepoInitOpsProcessor repoInitProcessor;

    private final RepoInitParser repoInitParser;

    public InstallModelTask(final TaskResourceGroup group,
            final Set<String> runModes,
            final SlingRepository repository,
            final JcrRepoInitOpsProcessor repoInitProcessor,
            final RepoInitParser repoInitParser,
            final BundleContext bundleContext) {
        super(group, bundleContext);
        this.activeRunModes = runModes;
        this.repository = repository;
        this.repoInitProcessor = repoInitProcessor;
        this.repoInitParser = repoInitParser;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void execute(final InstallationContext ctx) {
        try {
            final TaskResource resource = this.getResource();
            final String modelTxt = (String) resource.getAttribute(ModelTransformer.ATTR_MODEL);
            final Integer featureIndex = (Integer) resource.getAttribute(ModelTransformer.ATTR_FEATURE_INDEX);
            final String name = (String) resource.getAttribute(ModelTransformer.ATTR_FEATURE_NAME);
            if ( modelTxt == null || featureIndex == null || name == null ) {
                ctx.log("Unable to install model resource {} : no model found", this.getResource());
                this.getResourceGroup().setFinishState(ResourceState.IGNORED);
            } else {
                final String path = (String) resource.getAttribute(ModelTransformer.ATTR_BASE_PATH);
                final File baseDir = (path == null ? null : new File(path));

                boolean success = false;
                try {
                    final Result result = this.transform(name, modelTxt, featureIndex, resource, baseDir);
                    if ( result == null ) {
                        ctx.log("Unable to install model resource {} : unable to create resources", this.getResource());
                        this.getResourceGroup().setFinishState(ResourceState.IGNORED);
                    } else {
                        // repo init first
                        if ( result.repoinit != null ) {
                            List<Operation> ops = null;
                            try ( final Reader r = new StringReader(result.repoinit) ) {
                                ops = this.repoInitParser.parse(r);
                            } catch (final IOException | RepoInitParsingException e) {
                                logger.error("Unable to parse repoinit block.", e);
                                ctx.log("Unable to install model resource {} : unable parse repoinit block.", this.getResource());
                                this.getResourceGroup().setFinishState(ResourceState.IGNORED);
                                return;
                            }

                            // login admin is required for repo init
                            Session session = null;
                            try {
                                session = this.repository.loginAdministrative(null);
                                this.repoInitProcessor.apply(session, ops);
                                session.save();
                            } catch ( final RepositoryException re) {
                                logger.error("Unable to process repoinit block.", re);
                                ctx.log("Unable to install model resource {} : unable to process repoinit block.", this.getResource());
                                this.getResourceGroup().setFinishState(ResourceState.IGNORED);
                                return;

                            } finally {
                                if ( session != null ) {
                                    session.logout();
                                }
                            }
                        }
                        if ( !result.resources.isEmpty() ) {
                            final OsgiInstaller installer = this.getService(OsgiInstaller.class);
                            if ( installer != null ) {
                                installer.registerResources("model-" + name, result.resources.toArray(new InstallableResource[result.resources.size()]));
                            } else {
                                ctx.log("Unable to install model resource {} : unable to get OSGi installer", this.getResource());
                                this.getResourceGroup().setFinishState(ResourceState.IGNORED);
                                return;
                            }
                        }
                        this.getResourceGroup().setFinishState(ResourceState.INSTALLED);
                        success = true;
                    }
                } finally {
                    if ( !success && baseDir != null ) {
                        this.deleteDirectory(baseDir);
                    }
                }
            }
        } finally {
            this.cleanup();
        }
    }

    public static final class Result {
        public final List<InstallableResource> resources = new ArrayList<>();
        public String repoinit;
    }

    private Result transform(final String name,
            final String modelText,
            final int featureIndex,
            final TaskResource rsrc,
            final File baseDir) {
        Model model = null;
        try ( final Reader reader = new StringReader(modelText)) {
           model = ModelUtility.getEffectiveModel(ModelReader.read(reader, name));
        } catch ( final IOException ioe) {
            logger.warn("Unable to read model file for feature " + name, ioe);
        }
        if ( model == null ) {
            return null;
        }
        int index = 0;
        final Iterator<Feature> iter = model.getFeatures().iterator();
        while ( iter.hasNext() ) {
            iter.next();
            if ( index != featureIndex ) {
                iter.remove();
            }
            index++;
        }

        if ( baseDir != null ) {
            final List<Artifact> artifacts = new ArrayList<>();
            final Feature feature = model.getFeatures().get(0);
            for(final RunMode rm : feature.getRunModes()) {
                for(final ArtifactGroup group : rm.getArtifactGroups()) {
                    for(final Artifact a : group) {
                        artifacts.add(a);
                    }
                }
            }

            // extract artifacts
            final byte[] buffer = new byte[1024*1024*256];

            try ( final InputStream is = rsrc.getInputStream() ) {
                ModelArchiveReader.read(is, new ModelArchiveReader.ArtifactConsumer() {

                    @Override
                    public void consume(final Artifact artifact, final InputStream is) throws IOException {
                        if ( artifacts.contains(artifact) ) {
                            final File artifactFile = new File(baseDir, artifact.getRepositoryPath().replace('/', File.separatorChar));
                            if ( !artifactFile.exists() ) {
                                artifactFile.getParentFile().mkdirs();
                                try (final OutputStream os = new FileOutputStream(artifactFile)) {
                                    int l = 0;
                                    while ( (l = is.read(buffer)) > 0 ) {
                                        os.write(buffer, 0, l);
                                    }
                                }
                            }
                        }
                    }
                });
            } catch ( final IOException ioe) {
                logger.warn("Unable to extract artifacts from model " + name, ioe);
                return null;
            }
        }

        final List<ArtifactDescription> files = new ArrayList<>();

        Map<Traceable, String> errors = collectArtifacts(model, files, baseDir);
        if ( errors == null ) {
            final Result result = new Result();
            for(final ArtifactDescription desc : files) {
                if ( desc.artifactFile != null ) {
                    try {
                        final InputStream is = new FileInputStream(desc.artifactFile);
                        final String digest = String.valueOf(desc.artifactFile.lastModified());
                        // handle start level
                        final Dictionary<String, Object> dict = new Hashtable<String, Object>();
                        if ( desc.startLevel > 0 ) {
                            dict.put(InstallableResource.BUNDLE_START_LEVEL, desc.startLevel);
                        }
                        dict.put(InstallableResource.RESOURCE_URI_HINT, desc.artifactFile.toURI().toString());

                        result.resources.add(new InstallableResource("/" + desc.artifactFile.getName(), is, dict, digest,
                                                              InstallableResource.TYPE_FILE, null));
                    } catch ( final IOException ioe ) {
                        logger.warn("Unable to read artifact " + desc.artifactFile, ioe);
                        return null;
                    }
                } else if ( desc.cfg != null ) {
                    final String id = (desc.cfg.getFactoryPid() != null ? desc.cfg.getFactoryPid() + "-" + desc.cfg.getPid() : desc.cfg.getPid());
                    result.resources.add(new InstallableResource("/" + id + ".config",
                            null,
                            desc.cfg.getProperties(),
                            null,
                            InstallableResource.TYPE_CONFIG, null));

                } else if ( desc.section != null ) {
                    result.repoinit = desc.section.getContents();
                }
            }
            return result;
        }
        logger.warn("Errors during parsing model file {} : {}", name, errors.values());

        return null;
    }

    private static class ArtifactDescription {
        public int startLevel;
        public File artifactFile;
        public Configuration cfg;
        public Section section;
    }

    private Map<Traceable, String> collectArtifacts(final Model effectiveModel,
            final List<ArtifactDescription> files,
            final File baseDir) {
        final RepositoryAccess repo = new RepositoryAccess();
        final Map<Traceable, String> errors = new HashMap<>();
        for(final Feature f : effectiveModel.getFeatures()) {
            if ( f.isSpecial() ) {
                continue;
            }
            for(final Section section : f.getAdditionalSections()) {
                final ArtifactDescription desc = new ArtifactDescription();
                desc.section = section;
                files.add(desc);
            }
            for(final RunMode mode : f.getRunModes()) {
                if ( mode.isSpecial() ) {
                    continue;
                }
                if ( mode.isActive(this.activeRunModes) ) {
                    for(final ArtifactGroup group : mode.getArtifactGroups()) {
                        for(final Artifact artifact : group) {
                            File file = (baseDir == null ? null : new File(baseDir, artifact.getRepositoryPath().replace('/', File.separatorChar)));
                            if ( file == null || !file.exists() ) {
                                file = repo.get(artifact);
                            }
                            if ( file == null ) {
                                errors.put(artifact, "Artifact " + artifact.toMvnUrl() + " not found.");
                            } else {
                                final ArtifactDescription desc = new ArtifactDescription();
                                desc.artifactFile = file;
                                desc.startLevel = group.getStartLevel();
                                files.add(desc);
                            }
                        }
                    }
                    for(final Configuration cfg : mode.getConfigurations() ) {
                        if ( cfg.isSpecial() ) {
                            continue;
                        }
                        final ArtifactDescription desc = new ArtifactDescription();
                        desc.cfg = cfg;
                        files.add(desc);
                    }
                }
            }
        }
        return errors.isEmpty() ? null : errors;
    }

    @Override
    public String getSortKey() {
        return "30-" + getResource().getAttribute(ModelTransformer.ATTR_FEATURE_NAME);
    }
}
