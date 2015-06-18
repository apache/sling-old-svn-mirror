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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.sling.provisioning.model.Artifact;
import org.apache.sling.provisioning.model.ArtifactGroup;
import org.apache.sling.provisioning.model.Feature;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.RunMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** Merge nested models, provided by slingfeature/slingstart artifacts */
public class NestedModelsMerger extends ArtifactsVisitor {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private List<Artifact> toMerge;
    
    public NestedModelsMerger(Model m) {
        super(m);
    }
    
    @Override
    public synchronized void visit() throws Exception {
        toMerge = new ArrayList<Artifact>();
        super.visit();
        log.info("{} nested models found in provisioning model", toMerge.size());
        
        for(Artifact a : toMerge) {
            log.info("Resolving and merging nested model {}", a);
            InputStream is = null;
            Reader r = null;
            try {
                is = MavenResolver.resolve(a);
                r = new InputStreamReader(is);
                Launcher.mergeModel(model, r, a.toString());
            } catch(Exception e) {
                log.error("Failed to read nested model " + a, e);
            } finally {
                if(r != null) {
                    r.close();
                }
                if(is != null) {
                    is.close();
                }
            }
        }
    }
    
    boolean isNestedModel(Artifact a) {
        final String aType = a.getType();
        final String classifier = a.getClassifier();
        final boolean result = 
                MavenResolver.SLINGFEATURE_ARTIFACT_TYPE.equals(aType)
                || MavenResolver.SLINGFEATURE_ARTIFACT_TYPE.equals(classifier)
                || MavenResolver.SLINGSTART_ARTIFACT_TYPE.equals(aType)
                || MavenResolver.SLINGSTART_ARTIFACT_TYPE.equals(classifier);
        return result;
    }
    
    @Override
    protected void visitArtifact(Feature f, RunMode rm, ArtifactGroup g, Artifact a) throws Exception {
        if(isNestedModel(a)) {
            log.info("Artifact identified as a nested model , will be merged: {}", a);
            toMerge.add(a);
        }
    }
}