package org.apache.sling.ide.eclipse.m2e.core.internal.launch;

import java.io.File;

import org.apache.sling.ide.eclipse.core.launch.SourceReferenceResolver;
import org.apache.sling.ide.osgi.MavenSourceReference;
import org.apache.sling.ide.osgi.SourceReference;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.launching.IRuntimeClasspathEntry;
import org.eclipse.jdt.launching.JavaRuntime;

public class MavenSourceReferenceResolver implements SourceReferenceResolver {
    @Override
    public IRuntimeClasspathEntry resolve(SourceReference reference) {
        if ( reference == null || reference.getType() != SourceReference.Type.MAVEN) {
            return null;
        }
        
        MavenSourceReference sr = (MavenSourceReference) reference;
        
        // TODO - delegate to m2e and move to eclipse-m2e-core
        StringBuilder path = new StringBuilder();
        path.append(System.getProperty("user.home"))
            .append(File.separator).append(".m2").append(File.separator).append("repository").append(File.separator);
        
        for (String segment : sr.getGroupId().split("\\.") ) {
            path.append(segment).append(File.separator);
        }
        path.append(sr.getArtifactId()).append(File.separator).append(sr.getVersion()).append(File.separator);
        path.append(sr.getArtifactId()).append("-").append(sr.getVersion());
        
        IPath jarPath = Path.fromOSString(path.toString() + ".jar");
        IPath sourcePath = Path.fromOSString(path.toString() + "-sources.jar");
        // TODO - ensure exists
        if ( !jarPath.toFile().exists() || !sourcePath.toFile().exists()) {
            return null;
        }
        
        IRuntimeClasspathEntry mavenEntry = JavaRuntime.newArchiveRuntimeClasspathEntry(jarPath);
        mavenEntry.setSourceAttachmentPath(sourcePath);
        
        return mavenEntry;
    }
}
