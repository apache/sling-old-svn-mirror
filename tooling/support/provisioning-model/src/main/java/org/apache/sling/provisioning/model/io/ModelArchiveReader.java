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
import java.io.InputStreamReader;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.sling.provisioning.model.Artifact;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelUtility;

/**
 * The model archive reader can be used to read an archive based on a model
 * The archive contains the model file and all artifacts.
 * @since 1.3
 */
public class ModelArchiveReader {

    public interface ArtifactConsumer {

        /**
         * Consume the artifact from the archive
         * The input stream must not be closed by the consumer.
         * @param artifact The artifact
         * @param is The input stream for the artifact
         * @throws IOException If the artifact can't be consumed
         */
        void consume(Artifact artifact, final InputStream is) throws IOException;
    }

    /**
     * Read a model archive.
     * The input stream is not closed. It is up to the caller to close the input stream.
     * @param in The input stream to read from.
     * @return The model
     * @throws IOException If anything goes wrong
     */
    @SuppressWarnings("resource")
    public static Model read(final InputStream in,
                             final ArtifactConsumer consumer)
    throws IOException {
        Model model = null;

        final JarInputStream jis = new JarInputStream(in);

        // check manifest
        final Manifest manifest = jis.getManifest();
        if ( manifest == null ) {
            throw new IOException("Not a model archive - manifest is missing.");
        }
        // check manifest header
        final String version = manifest.getMainAttributes().getValue(ModelArchiveWriter.MANIFEST_HEADER);
        if ( version == null ) {
            throw new IOException("Not a model archive - manifest header is missing.");
        }
        // validate manifest header
        try {
            final int number = Integer.valueOf(version);
            if ( number < 1 || number > ModelArchiveWriter.ARCHIVE_VERSION ) {
                throw new IOException("Not a model archive - invalid manifest header value: " + version);
            }
        } catch (final NumberFormatException nfe) {
            throw new IOException("Not a model archive - invalid manifest header value: " + version);
        }

        // read contents
        JarEntry entry = null;
        while ( ( entry = jis.getNextJarEntry() ) != null ) {
            if ( ModelArchiveWriter.MODEL_NAME.equals(entry.getName()) ) {
                model = ModelUtility.getEffectiveModel(ModelReader.read(new InputStreamReader(jis, "UTF-8"), null));
            } else if ( !entry.isDirectory() && entry.getName().startsWith(ModelArchiveWriter.ARTIFACTS_PREFIX) ) { // artifact
                final Artifact artifact = Artifact.fromMvnUrl("mvn:" + entry.getName().substring(ModelArchiveWriter.ARTIFACTS_PREFIX.length()));
                consumer.consume(artifact, jis);
            }
            jis.closeEntry();
        }
        if ( model == null ) {
            throw new IOException("Not a model archive - model file is missing.");
        }

        // TODO - we could check whether all artifacts from the model are in the archive

        return model;
    }
}
