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
package org.apache.sling.ide.eclipse.internal.validation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.jar.Manifest;

import org.apache.sling.ide.eclipse.core.internal.Activator;
import org.apache.sling.ide.eclipse.core.internal.ProjectHelper;
import org.apache.sling.ide.log.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;

public class ServiceComponentHeaderValidator {

    /**
     * Finds missing SCR descriptor files referenced in the manifest
     * 
     * <p>
     * Only acts if the Manifest is located under the project's output directory at
     * </p>
     * 
     * @param manifest the location of the manifest to parse for the Service-Component header
     * @return a list of missing files, empty if no problems are found
     * @throws CoreException any errors
     */
    public List<IFile> findMissingScrDescriptors(IFile manifest) throws CoreException {

        IProject project = manifest.getProject();

        Logger pluginLogger = Activator.getDefault().getPluginLogger();

        IJavaProject javaProject = ProjectHelper.asJavaProject(project);

        IFolder outputFolder = (IFolder) project.getWorkspace().getRoot().findMember(javaProject.getOutputLocation());

        if (!outputFolder.getFullPath().isPrefixOf(manifest.getFullPath())) {
            pluginLogger.trace("Ignoring manifest found at {0} since it is not under the output directory at {1}",
                    manifest.getFullPath(), outputFolder.getFullPath());
            return Collections.emptyList();
        }

        List<IFile> missingDescriptors = new ArrayList<>();

        try (InputStream contents = manifest.getContents();) {
            Manifest mf = new Manifest(contents);
            String serviceComponentHeader = mf.getMainAttributes().getValue("Service-Component");
            if (serviceComponentHeader != null) {
                String[] entries = serviceComponentHeader.split(",");
                for (String entry : entries) {
                    entry = entry.trim();

                    if (entry.contains("*")) {
                        pluginLogger.trace("Ignoring wildcard Service-Component entry {0}", entry);
                        continue;
                    }

                    IFile descriptor = outputFolder.getFile(entry);

                    if (descriptor.exists()) {
                        pluginLogger.trace("Found matching resource for Service-Component entry {0}", entry);
                        continue;
                    }

                    missingDescriptors.add(descriptor);

                    pluginLogger.trace("Raising error for missing DS descriptor entry {0}", entry);
                }
            }
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Unable to access "
                    + manifest.getFullPath(), e));
        }

        return missingDescriptors;
    }
}
