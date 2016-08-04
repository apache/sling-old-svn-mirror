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
package org.apache.sling.ide.test.impl.helpers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Manifest;

import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.wst.common.project.facet.core.IFacetedProject;
import org.eclipse.wst.common.project.facet.core.IProjectFacet;
import org.eclipse.wst.common.project.facet.core.IProjectFacetVersion;
import org.eclipse.wst.common.project.facet.core.ProjectFacetsManager;

/**
 * The <tt>ProjectAdapter</tt> adapts the Eclipse project and resource APIs to make them simpler to use for testing
 * purposes
 *
 */
public class ProjectAdapter {

    private final IProject project;
    private IJavaProject javaProject;

    public ProjectAdapter(IProject project) {
        this.project = project;
    }

    public void addNatures(String... naturesToAdd) throws CoreException {

        IProjectDescription desc = project.getDescription();
        String[] natures = desc.getNatureIds();
        String[] newNatures = new String[natures.length + naturesToAdd.length];
        System.arraycopy(natures, 0, newNatures, 0, natures.length);
        for (int i = 0; i < naturesToAdd.length; i++) {
            newNatures[natures.length + i] = naturesToAdd[i];
        }
        desc.setNatureIds(newNatures);

        project.setDescription(desc, new NullProgressMonitor());

    }

    public void configureAsJavaProject(MavenDependency... dependencies) throws CoreException {

        // get dependency to required artifacts

        List<Artifact> resolvedArtifacts = new ArrayList<>(dependencies.length);
        for (MavenDependency d : dependencies) {
            resolvedArtifacts.add(MavenPlugin.getMaven().resolve(d.getGroupId(), d.getArtifactId(), d.getVersion(),
                    "jar", "", MavenPlugin.getMaven().getArtifactRepositories(), new NullProgressMonitor()));
        }

        // create java project
        javaProject = JavaCore.create(project);
        Set<IClasspathEntry> entries = new HashSet<>();
        entries.add(JavaRuntime.getDefaultJREContainerEntry());
        for (Artifact artifact : resolvedArtifacts) {
            entries.add(JavaCore.newLibraryEntry(Path.fromOSString(artifact.getFile().getAbsolutePath()), null, null));
        }

        IFolder src = project.getFolder("src");
        src.create(true, true, new NullProgressMonitor());
        entries.add(JavaCore.newSourceEntry(src.getFullPath()));

        javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[entries.size()]), new NullProgressMonitor());

        IFolder bin = project.getFolder("bin");
        if (!bin.exists()) { // TODO - not sure why this exists...
            bin.create(true, true, new NullProgressMonitor());
        }

        javaProject.setOutputLocation(bin.getFullPath(), new NullProgressMonitor());

    }

    /**
     * Creates or updates an existing file
     * 
     * @param fileLocation the path where the resource will be created or updated
     * @param contents the contents to write to the file. This stream will be closed after being used
     * @throws CoreException
     */
    public void createOrUpdateFile(IPath fileLocation, InputStream contents) throws CoreException {

        if (contents == null) {
            throw new IllegalArgumentException("resourceAsStream may not be null");
        }

        try {
            
            IContainer holder = ensureDirectoryExists(fileLocation.removeLastSegments(1));

            
            IPath fileName = Path.fromPortableString(fileLocation.lastSegment());
            IFile file = holder.getFile(fileName);
            if (file.exists()) {
                file.setContents(contents, true, true, new NullProgressMonitor());
            } else {
                file.create(contents, true, new NullProgressMonitor());
            }
        } finally {
            IOUtils.closeQuietly(contents);
        }

    }
    
    /**
     * Ensures that the specified directory exists
     * 
     * @param path the path where the directory should exist
     * @return the created or existing directory
     * @throws CoreException
     */
    public IContainer ensureDirectoryExists(IPath path) throws CoreException {
        
        IContainer current = project;
        
        for (int i = 0; i < path.segmentCount(); i++) {

            String currentSegment = path.segment(i);
            IResource container = current.findMember(currentSegment);

            if (container != null) {
                if (container.getType() != IContainer.FOLDER) {
                    throw new IllegalArgumentException("Resource " + container
                            + " exists and is not a folder; unable to create file at path " + path);
                }

                current = (IContainer) container;
            } else {

                IFolder newFolder = ((IContainer) current).getFolder(Path.fromPortableString(currentSegment));
                newFolder.create(true, true, new NullProgressMonitor());
                current = newFolder;
            }
        }
        
        return current;
    }

    public void createVltFilterWithRoots(String... roots) throws CoreException {

        StringBuilder builder = new StringBuilder();
        builder.append("<workspaceFilter vesion=\"1.0\">\n");
        for (String root : roots) {
            builder.append("  <filter root=\"").append(root).append("\"/>\n");
        }
        builder.append("</workspaceFilter>\n");

        createOrUpdateFile(Path.fromPortableString("META-INF/vault/filter.xml"), new ByteArrayInputStream(builder
                .toString().getBytes()));
    }

    public void createOsgiBundleManifest(OsgiBundleManifest osgiManifest) throws CoreException, IOException {

        Manifest m = new Manifest();
        for (Map.Entry<String, String> entry : osgiManifest.getAttributes().entrySet()) {
            m.getMainAttributes().putValue(entry.getKey(), entry.getValue());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        m.write(out);

        createOrUpdateFile(Path.fromPortableString("src/META-INF/MANIFEST.MF"), new ByteArrayInputStream(out.toByteArray()));
    }

    public void installFacet(String facetId, String facetVersion) throws CoreException {

        IFacetedProject facetedProject = ProjectFacetsManager.create(project);
        IProjectFacet slingBundleFacet = ProjectFacetsManager.getProjectFacet(facetId);
        IProjectFacetVersion projectFacetVersion = slingBundleFacet.getVersion(facetVersion);

        facetedProject.installProjectFacet(projectFacetVersion, null, new NullProgressMonitor());

    }


    public void deleteMember(IPath path) throws CoreException {

        IResource member = project.findMember(path);
        if (member != null) {
            member.delete(true, new NullProgressMonitor());
        }
    }

}
