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
package org.apache.sling.crankstart.core.commands;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import org.apache.sling.crankstart.api.CrankstartCommand;
import org.apache.sling.crankstart.api.CrankstartCommandLine;
import org.apache.sling.crankstart.api.CrankstartContext;
import org.apache.sling.provisioning.model.Artifact;
import org.apache.sling.provisioning.model.ArtifactGroup;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelUtility;
import org.apache.sling.provisioning.model.RunMode;
import org.apache.sling.provisioning.model.io.ModelReader;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** CrankstartCommand that reads a Sling Provisioning Model
 *  and installs all its artifacts */
public class ProvisioningModel implements CrankstartCommand {
    public static final String I_CMD = "provisioning.model";
    public static final String MODEL_FILE_EXT = ".txt";
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    
    @Override
    public boolean appliesTo(CrankstartCommandLine commandLine) {
        return I_CMD.equals(commandLine.getVerb());
    }

    public String getDescription() {
        return I_CMD + ": read a provisioning model and install its artifacts";
    }
    
    @Override
    public void execute(CrankstartContext crankstartContext, CrankstartCommandLine commandLine) throws Exception {
        final File modelFolder = new File(commandLine.getQualifier());
        if(!modelFolder.isDirectory() || !modelFolder.canRead()) {
            throw new IOException("Cannot read specified provisioning model folder " + modelFolder.getAbsolutePath());
        }
        Model m = null;
        for(String filename : modelFolder.list()) {
            final File modelFile = new File(modelFolder, filename);
            if(!modelFile.getName().endsWith(MODEL_FILE_EXT)) {
                log.warn("Model file name does not end with {}, ignored: {}", MODEL_FILE_EXT, modelFile.getAbsolutePath());
                continue;
            }
            final Reader r = new FileReader(modelFile);
            try {
                final Model current = ModelReader.read(r, modelFile.getAbsolutePath());
                if(m == null) {
                    log.info("Initial model: {}", modelFile.getName());
                    m = current;
                } else {
                    log.info("Merging additional model: {}", modelFile.getName());
                    ModelUtility.merge(m, current);
                }
            } finally {
                r.close();
            }
            
        }
        
        log.info("Processing the merged provisioning model from {}", modelFolder);
        processModel(crankstartContext, m);
        log.info("Done processing the merged provisioning model");
    }
    
    private void processModel(CrankstartContext crankstartContext, Model m) throws IOException, BundleException {
        final BundleContext ctx = crankstartContext.getOsgiFramework().getBundleContext();
        m = ModelUtility.getEffectiveModel(m, null);
        for(Feature f : m.getFeatures()) {
            log.info("Processing provisioning model feature: {}", f.getName());
            for(RunMode rm : f.getRunModes()) {
                for(ArtifactGroup g : rm.getArtifactGroups()) {
                    final int startLevel = g.getStartLevel();
                    for(Artifact a : g) {
                        // TODO for now, naively assume a is a bundle, and mvn: protocol
                        final String url = "mvn:" + a.getGroupId() + "/" + a.getArtifactId() + "/" + a.getVersion();
                        InstallBundle.installBundle(ctx, log, url, startLevel);
                    }
                }
            }
        }
    }
}
