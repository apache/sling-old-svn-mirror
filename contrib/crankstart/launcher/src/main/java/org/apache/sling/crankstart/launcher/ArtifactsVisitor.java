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
package org.apache.sling.crankstart.launcher;

import org.apache.sling.provisioning.model.Artifact;
import org.apache.sling.provisioning.model.ArtifactGroup;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.RunMode;

/** Visit the Artifacts of a Model */
public abstract class ArtifactsVisitor {
    protected final Model model;
    
    public ArtifactsVisitor(Model m) {
        model = m;
    }
    
    protected abstract void visitArtifact(Feature f, RunMode rm, ArtifactGroup g, Artifact a) throws Exception;
    
    public void visit() throws Exception {
        for(Feature f : model.getFeatures()) {
            if(!acceptFeature(f)) {
                continue;
            }
            for(RunMode rm : f.getRunModes()) {
                if(!acceptRunMode(rm)) {
                    continue;
                }
                for(ArtifactGroup g : rm.getArtifactGroups()) {
                    if(!acceptArtifactGroup(g)) {
                        continue;
                    }
                    for(Artifact a : g) {
                        visitArtifact(f, rm, g, a);
                    }
                }
            }
        }
    }
    
    protected boolean acceptFeature(Feature f) {
        return true;
    }
    
    protected boolean acceptRunMode(RunMode rm) {
        return true;
    }
    
    protected boolean acceptArtifactGroup(ArtifactGroup g) {
        return true;
    }   
}