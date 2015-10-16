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
package org.apache.sling.tooling.lc.aether;

import java.util.Objects;

import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.Bundle;
import org.apache.sling.provisioning.model.Artifact;

public class ArtifactKey implements Comparable<ArtifactKey> {
    
    private String groupId;
    private String artifactId;
    private String classifier;
    private String type;
    
    public ArtifactKey(Bundle bundle) {
        
        this(bundle.getGroupId(), bundle.getArtifactId(), bundle.getClassifier(), bundle.getType());
    }

    public ArtifactKey(Artifact artifact) {
        
        this(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(), artifact.getType());
    }
    
    private ArtifactKey(String groupId, String artifactId, String classifier, String type) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.classifier = classifier != null ? classifier : "";
        this.type = type;
    }

    @Override
    public int hashCode() {
        
        return Objects.hash(artifactId, classifier, groupId, type);
    }

    @Override
    public boolean equals(Object obj) {
        
        if ( !(obj instanceof ArtifactKey) ) {
            return false;
        }
        
        ArtifactKey other = (ArtifactKey) obj;
        
        return Objects.equals(artifactId, other.artifactId)
                && Objects.equals(groupId, other.groupId)
                && Objects.equals(classifier, other.classifier)
                && Objects.equals(type, other.type);
    }
    
    public String getArtifactId() {
        return artifactId;
    }
    
    public String getGroupId() {
        return groupId;
    }
    
    public String getClassifier() {
        return classifier;
    }
    
    public String getType() {
        return type;
    }
    
    @Override
    public String toString() {
        
        return "Artifact [groupId=" + groupId + ", artifactId=" + artifactId + ", classifier=" + classifier + ", type=" + type + "]";
    }

    @Override
    public int compareTo(ArtifactKey o) {
        
        Artifact us = new Artifact(groupId, artifactId, "0.0.0", classifier, type);
        Artifact them = new Artifact(o.groupId, o.artifactId, "0.0.0", o.classifier, o.type);
        
        return us.compareTo(them);
    }
}