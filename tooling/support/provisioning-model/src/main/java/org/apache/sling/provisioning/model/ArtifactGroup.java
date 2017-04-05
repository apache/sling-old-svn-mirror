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
package org.apache.sling.provisioning.model;

import java.util.Collections;


/**
 * A artifact group holds a set of artifacts.
 *
 * A valid start level is positive, start level 0 means the default OSGi start level.
 */
public class ArtifactGroup extends ItemList<Artifact>
    implements Comparable<ArtifactGroup> {

    /** The start level. */
    private final int level;

    /**
     * Create a new artifact group with the level.
     * @param startLevel The start level.
     */
    public ArtifactGroup(final int startLevel) {
        this.level = startLevel;
    }

    @Override
    public void add(Artifact item) {
        super.add(item);
        Collections.sort(super.items);
    }

    /**
     * Get the start level.
     * @return The start level.
     */
    public int getStartLevel() {
        return this.level;
    }

    /**
     * Search an artifact with the same groupId, artifactId, type and classifier.
     * Version is not considered.
     * @param template A template artifact
     * @return The artifact or {@code null}.
     */
    public Artifact search(final Artifact template) {
        Artifact found = null;
        for(final Artifact current : this) {
            if ( current.getGroupId().equals(template.getGroupId())
              && current.getArtifactId().equals(template.getArtifactId())
              && ((current.getClassifier() == null && template.getClassifier() == null)
                  || (current.getClassifier().equals(template.getClassifier()) ))
              && current.getType().equals(template.getType()) ) {
                found = current;
                break;
            }
        }
        return found;
    }

    @Override
    public int compareTo(final ArtifactGroup o) {
        if ( this.level < o.level ) {
            return -1;
        } else if ( this.level > o.level ) {
            return 1;
        }
        return 0;
    }

    @Override
    public String toString() {
        return "ArtifactGroup [level=" + level
                + ", artifacts=" + this.items
                + ( this.getLocation() != null ? ", location=" + this.getLocation() : "")
                + "]";
    }
}
