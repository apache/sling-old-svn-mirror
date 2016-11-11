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
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
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
            if ( modelTxt == null ) {
                ctx.log("Unable to install model resource {} : no model found", this.getResource());
                this.getResourceGroup().setFinishState(ResourceState.IGNORED);
            } else {
                final String name = this.getModelName();
                final Result result = this.transform(name, modelTxt);
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
                            this.getResourceGroup().setFinishState(ResourceState.INSTALLED);
                        } else {
                            ctx.log("Unable to install model resource {} : unable to get OSGi installer", this.getResource());
                            this.getResourceGroup().setFinishState(ResourceState.IGNORED);
                        }
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

    private Result transform(final String name, final String modelText) {
        try ( final Reader reader = new StringReader(modelText)) {
            final Model model = ModelUtility.getEffectiveModel(ModelReader.read(reader, name));

            final List<ArtifactDescription> files = new ArrayList<>();

            Map<Traceable, String> errors = collectArtifacts(model, files);
            if ( errors == null ) {
                final Result result = new Result();
                for(final ArtifactDescription desc : files) {
                    if ( desc.artifactFile != null ) {
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
        } catch ( final IOException ioe) {
            logger.warn("Unable to read potential model file " + name, ioe);
        }
        return null;
    }

    private static class ArtifactDescription {
        public int startLevel;
        public File artifactFile;
        public Configuration cfg;
        public Section section;
    }

    private Map<Traceable, String> collectArtifacts(final Model effectiveModel, final List<ArtifactDescription> files) {
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
                            final File file = repo.get(artifact);
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
        return "30-" + getModelName();
    }
}
