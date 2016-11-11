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


import org.apache.sling.installer.api.tasks.InstallTask;
import org.apache.sling.installer.api.tasks.InstallTaskFactory;
import org.apache.sling.installer.api.tasks.ResourceState;
import org.apache.sling.installer.api.tasks.TaskResource;
import org.apache.sling.installer.api.tasks.TaskResourceGroup;
import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.repoinit.JcrRepoInitOpsProcessor;
import org.apache.sling.repoinit.parser.RepoInitParser;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This task factory process model resources detected by
 * the {@link ModelTransformer}.
 */
@Component(service = InstallTaskFactory.class)
public class ModelTaskFactory implements InstallTaskFactory {

    /** Logger. */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Reference
    private SlingSettingsService settings;

    @Reference
    private SlingRepository repository;

    @Reference
    private JcrRepoInitOpsProcessor repoInitProcessor;

    @Reference
    private RepoInitParser repoInitParser;

    private BundleContext bundleContext;

    @Activate
    private void activate(final BundleContext ctx) {
        this.bundleContext = ctx;
    }

    @Override
    public InstallTask createTask(final TaskResourceGroup group) {
        final TaskResource rsrc = group.getActiveResource();
        if ( !ModelTransformer.TYPE_PROV_MODEL.equals(rsrc.getType()) ) {
            return null;
        }
        if (rsrc.getState() == ResourceState.UNINSTALL ) {
            logger.info("Uninstalling {}", rsrc.getEntityId());

            return new UninstallModelTask(group, bundleContext);
        }
        logger.info("Installing {}", rsrc.getEntityId());
        return new InstallModelTask(group,
                this.settings.getRunModes(),
                this.repository,
                this.repoInitProcessor,
                this.repoInitParser,
                this.bundleContext);
    }
}
