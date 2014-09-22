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
package org.apache.sling.slingstart.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A start level holds a set of artifacts.
 * A valid start level is positive, start level 0 means the default OSGi start level.
 */
public class SSMStartLevel {

    public int level;

    public final List<SSMArtifact> artifacts = new ArrayList<SSMArtifact>();

    /**
     * validates the object and throws an IllegalStateException
     *
     * @throws IllegalStateException
     */
    public void validate() {
        for(final SSMArtifact sl : this.artifacts) {
            sl.validate();
        }
        if ( level < 0 ) {
            throw new IllegalStateException("level");
        }
    }

    /**
     * Search an artifact with the same groupId, artifactId, version, type and classifier.
     * Version is not considered.
     */
    public SSMArtifact search(final SSMArtifact template) {
        SSMArtifact found = null;
        for(final SSMArtifact current : this.artifacts) {
            if ( current.groupId.equals(template.groupId)
              && current.artifactId.equals(template.artifactId)
              && current.classifier.equals(template.classifier)
              && current.type.equals(template.type) ) {
                found = current;
                break;
            }
        }
        return found;
    }

    public void merge(final SSMStartLevel other) {
        for(final SSMArtifact a : other.artifacts) {
            final SSMArtifact found = this.search(a);
            if ( found != null ) {
                found.version = a.version;
            } else {
                this.artifacts.add(a);
            }
        }
    }

    @Override
    public String toString() {
        return "SSMStartLevel [level=" + level + ", artifacts=" + artifacts
                + "]";
    }
}
