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
package org.apache.sling.installer.factories.subsystems.base.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.sling.installer.api.InstallableResource;
import org.apache.sling.installer.api.tasks.RegisteredResource;
import org.apache.sling.installer.api.tasks.ResourceTransformer;
import org.apache.sling.installer.api.tasks.TransformationResult;
import org.apache.sling.settings.SlingSettingsService;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.namespace.IdentityNamespace;
import org.osgi.service.subsystem.SubsystemConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubsystemBaseTransformer implements ResourceTransformer {
    private static final String POTENTIAL_BUNDLES = "Potential_Bundles/";

    private static final String TYPE_SUBSYSTEM_BASE = "subsystem-base";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SlingSettingsService slingSettings;

    public SubsystemBaseTransformer(SlingSettingsService settings) {
        slingSettings = settings;
    }

    public TransformationResult[] transform(RegisteredResource resource) {
        // TODO start level of the subsystem
        if ( resource.getType().equals(InstallableResource.TYPE_FILE) ) {
            if ( resource.getURL().endsWith("." + TYPE_SUBSYSTEM_BASE) ) {
                logger.info("Found subsystem-base resource {}", resource);

                try {
                    SubsystemData ssd = createSubsystemFile(resource);

                    TransformationResult tr = new TransformationResult();
                    Attributes mfAttributes = ssd.manifest.getMainAttributes();
                    tr.setId(mfAttributes.getValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME));
                    tr.setVersion(new Version(mfAttributes.getValue(SubsystemConstants.SUBSYSTEM_VERSION)));
                    tr.setResourceType("esa");
                    tr.setInputStream(new DeleteOnCloseFileInputStream(ssd.file));

                    Map<String, Object> attr = new HashMap<String, Object>();
                    attr.put(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME, mfAttributes.getValue(SubsystemConstants.SUBSYSTEM_SYMBOLICNAME));
                    attr.put(SubsystemConstants.SUBSYSTEM_VERSION, mfAttributes.getValue(SubsystemConstants.SUBSYSTEM_VERSION));
                    tr.setAttributes(attr);

                    return new TransformationResult[] {tr};
                } catch (IOException ioe) {
                    logger.error("Problem processing subsystem-base file " + resource, ioe);
                }
            }
        }
        return null;
    }

    private SubsystemData createSubsystemFile(RegisteredResource resource) throws IOException {
        SubsystemData data = new SubsystemData();
        StringBuilder subsystemContent = new StringBuilder();
        try (JarInputStream jis = new JarInputStream(resource.getInputStream())) {
            Manifest runModesManifest = jis.getManifest();
            Set<String> runModeResources = processRunModesManifest(runModesManifest);

            File zf = File.createTempFile("sling-generated", ".esa");
            zf.deleteOnExit();
            data.file = zf;
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zf))) {
                ZipEntry zie = null;
                while((zie = jis.getNextEntry()) != null) {
                    String zieName = zie.getName();
                    if ("SUBSYSTEM-MANIFEST-BASE.MF".equals(zieName)) {
                        data.manifest = new Manifest(jis);
                    } else if (runModeResources.contains(zieName)) {
                        zieName = zieName.substring(POTENTIAL_BUNDLES.length());

                        handleSubsystemArtifact(zieName, jis, zos, subsystemContent);
                    }
                }

                data.manifest.getMainAttributes().putValue(SubsystemConstants.SUBSYSTEM_CONTENT, subsystemContent.toString());
                ZipEntry zoe = new ZipEntry("OSGI-INF/SUBSYSTEM.MF");
                try {
                    zos.putNextEntry(zoe);
                    data.manifest.write(zos);
                } finally {
                    zos.closeEntry();
                }
            }
        }

        return data;
    }

    private Set<String> processRunModesManifest(Manifest runModesManifest) {
        Set<String> res = new HashSet<>();

        Attributes attrs = runModesManifest.getMainAttributes();

        res.addAll(parseManifestHeader(attrs, "_all_"));
        for (String rm : slingSettings.getRunModes()) {
            res.addAll(parseManifestHeader(attrs, rm));
        }

        return res;
    }

    private List<String> parseManifestHeader(Attributes attrs, String key) {
        List<String> res = new ArrayList<>();

        String val = attrs.getValue(key);
        if (val == null)
            return Collections.emptyList();

        for (String r : val.split("[|]")) {
            if (r.length() > 0)
                res.add(r);
        }
        return res;
    }

    private void handleSubsystemArtifact(String artifactName, ZipInputStream zis, ZipOutputStream zos,
            StringBuilder subsystemContent) throws IOException {
        int idx = artifactName.indexOf('/');
        int idx2 = artifactName.lastIndexOf('/');
        if (idx != idx2 || idx == 0)
            throw new IOException("Invalid entry name, should have format .../<num>/artifact.jar: " + artifactName);

        int startOrder = Integer.parseInt(artifactName.substring(0, idx));
        idx++;
        if (artifactName.length() > idx) {
            File tempArtifact = File.createTempFile("sling-generated", ".tmp");
            tempArtifact.deleteOnExit();
            Path tempPath = tempArtifact.toPath();
            Files.copy(zis, tempPath, StandardCopyOption.REPLACE_EXISTING);

            try (JarFile jf = new JarFile(tempArtifact)) {
                Attributes ma = jf.getManifest().getMainAttributes();
                String bsn = ma.getValue(Constants.BUNDLE_SYMBOLICNAME);
                String version = ma.getValue(Constants.BUNDLE_VERSION);
                if (version == null)
                    version = "0";
                String type = ma.getValue(Constants.FRAGMENT_HOST) != null ?
                        IdentityNamespace.TYPE_FRAGMENT : IdentityNamespace.TYPE_BUNDLE;
                if (bsn != null) {
                    if (subsystemContent.length() > 0)
                        subsystemContent.append(',');

                    subsystemContent.append(bsn);
                    subsystemContent.append(';');
                    subsystemContent.append(IdentityNamespace.CAPABILITY_VERSION_ATTRIBUTE);
                    subsystemContent.append('=');
                    subsystemContent.append(version);
                    subsystemContent.append(';');
                    subsystemContent.append(IdentityNamespace.CAPABILITY_TYPE_ATTRIBUTE);
                    subsystemContent.append('=');
                    subsystemContent.append(type);
                    subsystemContent.append(';');
                    subsystemContent.append(SubsystemConstants.START_ORDER_DIRECTIVE);
                    subsystemContent.append(":=");
                    subsystemContent.append(startOrder);
                }

            } catch (Exception ex) {
                // apparently not a valid JarFile
            }
            ZipEntry zoe = new ZipEntry(artifactName.substring(idx));
            try {
                zos.putNextEntry(zoe);
                Files.copy(tempPath, zos);
            } finally {
                zos.closeEntry();
                tempArtifact.delete();
            }
        }
    }

    private static class SubsystemData {
        File file;
        Manifest manifest;
    }

    static class DeleteOnCloseFileInputStream extends FileInputStream {
        final File file;

        public DeleteOnCloseFileInputStream(File f) throws FileNotFoundException {
            super(f);
            file = f;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                file.delete();
            }
        }
    }
}
