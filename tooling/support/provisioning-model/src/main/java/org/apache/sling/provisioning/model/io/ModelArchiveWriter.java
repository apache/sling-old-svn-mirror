/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.provisioning.model.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import org.apache.sling.provisioning.model.Artifact;
import org.apache.sling.provisioning.model.ArtifactGroup;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelUtility;
import org.apache.sling.provisioning.model.RunMode;
import org.apache.sling.provisioning.model.Traceable;

/**
 * The model archive writer can be used to create an archive based on a model
 * The archive contains the model file and all artifacts.
 * @since 1.3
 */
public class ModelArchiveWriter {

    /** The manifest header marking an archive as a model archive. */
    public static final String MANIFEST_HEADER = "Model-Archive-Version";

    /** Current support version of the model archive. */
    public static final int ARCHIVE_VERSION = 1;

    /** Default extension for model archives. */
    public static final String DEFAULT_EXTENSION = "mar";

    /** Model name. */
    public static final String MODEL_NAME = "models/feature.model";

    /** Artifacts prefix. */
    public static final String ARTIFACTS_PREFIX = "artifacts/";

    public interface ArtifactProvider {

        /**
         * Provide an input stream for the artifact.
         * The input stream will be closed by the caller.
         * @param artifact The artifact
         * @return The input stream
         * @throws IOException If the input stream can't be provided
         */
        InputStream getInputStream(Artifact artifact) throws IOException;
    }

    /**
     * Create a model archive.
     * The output stream will not be closed by this method. The caller
     * must call {@link JarOutputStream#close()} or {@link JarOutputStream#finish()}
     * on the return output stream. The caller can add additional files through
     * the return stream.
     *
     * In order to create an archive for a model, each feature in the model must
     * have a name and a version and the model must be valid, therefore {@link ModelUtility#validateIncludingVersion(Model)}
     * is called first. If the model is invalid an {@code IOException} is thrown.
     *
     * @param out The output stream to write to
     * @param model The model to write
     * @param baseManifest Optional base manifest used for creating the manifest.
     * @param provider The artifact provider
     * @return The jar output stream.
     * @throws IOException If anything goes wrong
     */
    public static JarOutputStream write(final OutputStream out,
                             final Model model,
                             final Manifest baseManifest,
                             final ArtifactProvider provider)
    throws IOException {
        // check model
        final Map<Traceable, String> errors = ModelUtility.validate(model);
        if ( errors != null ) {
            throw new IOException("Model is not valid: " + errors);
        }

        // create manifest
        final Manifest manifest = (baseManifest == null ? new Manifest() : new Manifest(baseManifest));
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue(MANIFEST_HEADER, String.valueOf(ARCHIVE_VERSION));

        // create archive
        final JarOutputStream jos = new JarOutputStream(out, manifest);

        // write model first
        final JarEntry entry = new JarEntry(MODEL_NAME);
        jos.putNextEntry(entry);
        final Writer writer = new OutputStreamWriter(jos, "UTF-8");
        ModelWriter.write(writer, model);
        writer.flush();
        jos.closeEntry();

        final byte[] buffer = new byte[1024*1024*256];
        for(final Feature f : model.getFeatures() ) {
            for(final RunMode rm : f.getRunModes()) {
                for(final ArtifactGroup g : rm.getArtifactGroups()) {
                    for(final Artifact a : g) {
                        final JarEntry artifactEntry = new JarEntry(ARTIFACTS_PREFIX + a.getRepositoryPath());
                        jos.putNextEntry(artifactEntry);

                        try (final InputStream is = provider.getInputStream(a)) {
                            int l = 0;
                            while ( (l = is.read(buffer)) > 0 ) {
                                jos.write(buffer, 0, l);
                            }
                        }
                        jos.closeEntry();
                    }
                }
            }
        }
        return jos;
    }
}
