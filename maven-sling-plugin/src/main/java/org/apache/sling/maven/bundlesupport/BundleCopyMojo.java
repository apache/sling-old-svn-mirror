/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.maven.bundlesupport;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;

/**
 * Copies artifacts with scope <code>bundle</code> to the web archive at a
 * configurable location - <code>WEB-INF/resources/bundles</code> by default.
 * The main intent of this bundle is to include OSGi bundles with a Web
 * Application for installation on first startup.
 *
 * @goal copy
 * @phase process-resources
 * @description Copy "provided" artifacts to the web bundle as a resource
 * @requiresDependencyResolution bundle
 */
public class BundleCopyMojo extends AbstractMojo {

    /**
     * The directory to which the artifacts are copied.
     *
     * @parameter expression="${project.build.directory}/${project.build.finalName}/WEB-INF/resources/bundles"
     * @required
     */
    private String bundleDestination;

    /**
     * Project dependencies of which the ones with scope <code>bundle</code>
     * are selected to be included in the final artifact.
     *
     * @parameter expression="${project.artifacts}"
     * @required
     * @readonly
     */
    private Set dependencies;

    /**
     * Execute this Mojo
     */
    public void execute() {
        File bundleDestFile = new File(this.bundleDestination);
        bundleDestFile.mkdirs();

        Iterator artifacts = this.dependencies.iterator();
        while (artifacts.hasNext()) {
            Artifact artifact = (Artifact) artifacts.next();
            if (!"bundle".equals(artifact.getScope())) {
                this.getLog().debug(
                    "Ignoring non-bundle artifact " + artifact.getArtifactId());
                continue;
            }

            // fix scope to not include the artifact in the final bundle
            artifact.setScope(Artifact.SCOPE_PROVIDED);

            // copy file
            File source = artifact.getFile();
            String destName = this.getArtifactFileName(artifact);
            File dest = new File(bundleDestFile, destName);
            try {
                this.copyFile(source, dest);
                this.getLog().info(
                    "Copied Bundle " + source.getName() + " to " + dest);
            } catch (IOException ioe) {
                this.getLog().error(
                    "Failed to copy Bundle " + source.getName() + " to " + dest,
                    ioe);
            }
        }
    }

    private String getArtifactFileName(Artifact artifact) {
        if (artifact.getClassifier() != null) {
            return artifact.getArtifactId() + "-" + artifact.getVersion() + "-"
                + artifact.getClassifier() + "." + artifact.getType();
        }
        return artifact.getArtifactId() + "-" + artifact.getVersion() + "."
            + artifact.getType();
    }

    private void copyFile(File source, File dest) throws IOException {
        FileInputStream ins = null;
        FileOutputStream out = null;
        try {
            ins = new FileInputStream(source);
            out = new FileOutputStream(dest);

            byte[] buf = new byte[8192];
            int rd = 0;
            while ((rd = ins.read(buf)) >= 0) {
                out.write(buf, 0, rd);
            }
        } finally {
            if (ins != null) {
                try {
                    ins.close();
                } catch (IOException ignore) {
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ignore) {
                }
            }
        }
    }
}
