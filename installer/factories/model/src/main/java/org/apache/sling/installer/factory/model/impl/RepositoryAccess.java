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
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.sling.provisioning.model.Artifact;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for getting maven artifacts.
 *
 * It is a simple class assuming that the mvn command is installed
 * and that the .m2 directory is in the home directory of the current
 * user.
 */
public class RepositoryAccess {

    /**
     * A logger.
     */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * The .m2 directory.
     */
    private final String repoHome;

    /**
     * Create a new instance.
     */
    public RepositoryAccess() {
        this.repoHome = System.getProperty("user.home") + "/.m2/repository/";
    }

    /**
     * Get the file for an artifact
     * @param artifact The artifact
     * @return The file or {@code null}
     */
    public File get(final Artifact artifact) {

        final File artifactFile = this.getArtifact(artifact);

        if ( artifactFile == null ) {
            return null;
        }
        logger.info("Responding for {} with {}", artifact, artifactFile);
        return artifactFile;
    }

    private File getArtifact(final Artifact artifact) {
        logger.info("Requesting {}", artifact);

        final String filePath = (this.repoHome.concat(artifact.getRepositoryPath())).replace('/', File.separatorChar);
        logger.info("Trying to fetch artifact from {}", filePath);
        final File f = new File(filePath);
        if ( !f.exists() || !f.isFile() || !f.canRead() ) {
            logger.info("Trying to download {}", artifact.getRepositoryPath());
            try {
                this.downloadArtifact(artifact);
            } catch ( final IOException ioe ) {
                logger.debug("Error downloading file.", ioe);
            }
            if ( !f.exists() || !f.isFile() || !f.canRead() ) {
                logger.info("Artifact not found {}", artifact);

                return null;
            }
        }
        return f;
    }

    /**
     * Download artifact from maven
     * @throws IOException
     */
    private void downloadArtifact(final Artifact artifact) throws IOException {
        // create fake pom
        final Path dir = Files.createTempDirectory(null);
        final List<String> lines = new ArrayList<String>();
        lines.add("<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">");
        lines.add("    <modelVersion>4.0.0</modelVersion>");
        lines.add("    <groupId>org.apache.sling</groupId>");
        lines.add("    <artifactId>temp-artifact</artifactId>");
        lines.add("    <version>1-SNAPSHOT</version>");
        lines.add("    <dependencies>");
        lines.add("        <dependency>");
        lines.add("            <groupId>" + artifact.getGroupId() + "</groupId>");
        lines.add("            <artifactId>" + artifact.getArtifactId() + "</artifactId>");
        lines.add("            <version>" + artifact.getVersion() + "</version>");
        if ( artifact.getClassifier() != null ) {
            lines.add("            <classifier>" + artifact.getClassifier() + "</classifier>");
        }
        if ( !"bundle".equals(artifact.getType()) && !"jar".equals(artifact.getType()) ) {
            lines.add("            <type>" + artifact.getType() + "</type>");
        }
        lines.add("            <scope>provided</scope>");
        lines.add("        </dependency>");
        lines.add("    </dependencies>");
        lines.add("</project>");
        logger.info("Writing pom to {}", dir);
        Files.write(dir.resolve("pom.xml"), lines, Charset.forName("UTF-8"));

        final File output = dir.resolve("output.txt").toFile();
        final File error = dir.resolve("error.txt").toFile();

        // invoke maven
        logger.info("Invoking maven...");
        final ProcessBuilder pb = new ProcessBuilder("mvn", "verify");
        pb.directory(dir.toFile());
        pb.redirectOutput(Redirect.to(output));
        pb.redirectError(Redirect.to(error));

        final Process p = pb.start();
        try {
            p.waitFor();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}