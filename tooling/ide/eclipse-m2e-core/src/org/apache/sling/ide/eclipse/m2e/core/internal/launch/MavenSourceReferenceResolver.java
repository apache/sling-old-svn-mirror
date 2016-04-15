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
package org.apache.sling.ide.eclipse.m2e.core.internal.launch;

import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.sling.ide.eclipse.core.launch.SourceReferenceResolver;
import org.apache.sling.ide.osgi.MavenSourceReference;
import org.apache.sling.ide.osgi.SourceReference;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.m2e.core.MavenPlugin;

public class MavenSourceReferenceResolver implements SourceReferenceResolver {
    @Override
    public IRuntimeClasspathEntry resolve(SourceReference reference) throws CoreException {
        if ( reference == null || reference.getType() != SourceReference.Type.MAVEN) {
            return null;
        }
        
        MavenSourceReference sr = (MavenSourceReference) reference;
        
        List<ArtifactRepository> repos = MavenPlugin.getMaven().getArtifactRepositories();
        
        Artifact jarArtifact = MavenPlugin.getMaven().resolve(sr.getGroupId(), sr.getArtifactId(), sr.getVersion(), "jar", "", repos, new NullProgressMonitor());
        Artifact sourcesArtifact = MavenPlugin.getMaven().resolve(sr.getGroupId(), sr.getArtifactId(), sr.getVersion(), "jar", "sources", repos, new NullProgressMonitor());
        
        IPath jarPath = Path.fromOSString(jarArtifact.getFile().getAbsolutePath());
        IPath sourcePath = Path.fromOSString(sourcesArtifact.getFile().getAbsolutePath());
        
        IRuntimeClasspathEntry mavenEntry = JavaRuntime.newArchiveRuntimeClasspathEntry(jarPath);
        mavenEntry.setSourceAttachmentPath(sourcePath);
        
        return mavenEntry;
    }
}
