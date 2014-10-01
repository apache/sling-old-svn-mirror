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

import java.util.HashMap;
import java.util.Map;

/**
 * Description of an artifact.
 * An artifact is described by it's Apache Maven coordinates consisting of group id, artifact id, and version.
 * In addition, the classifier and type can be specified as well. If no type is specified, "jar" is assumed.
 * An artifact can have any metadata.
 */
public class Artifact extends Commentable {

    /** The required group id. */
    private final String groupId;
    /** The required artifact id. */
    private final String artifactId;
    /** The required version. */
    private final String version;
    /** The optional classifier. */
    private final String classifier;
    /** The optional type. */
    private final String type;

    /** Artifact metadata. */
    private final Map<String, String> metadata = new HashMap<String, String>();

    /**
     * Create a new artifact object
     * @param gId   The group id (required)
     * @param aId   The artifact id (required)
     * @param version The version (required)
     * @param classifier The classifier (optional)
     * @param type The type/extension (optional, defaults to jar)
     */
    public Artifact(final String gId,
            final String aId,
            final String version,
            final String classifier,
            final String type) {
        this.groupId = (gId != null ? gId.trim() : null);
        this.artifactId = (aId != null ? aId.trim() : null);
        this.version = (version != null ? version.trim() : null);
        final String trimmedType = (type != null ? type.trim() : null);
        if ( "bundle".equals(trimmedType) || trimmedType == null || trimmedType.isEmpty() ) {
            this.type = "jar";
        } else {
            this.type = trimmedType;
        }
        final String trimmedClassifier = (classifier != null ? classifier.trim() : null);
        if ( trimmedClassifier != null && trimmedClassifier.isEmpty() ) {
            this.classifier = null;
        } else {
            this.classifier = trimmedClassifier;
        }
    }

    /**
     * Create a new artifact from a maven url,
     * 'mvn:' [ repository-url '!' ] group-id '/' artifact-id [ '/' [version] [ '/' [type] [ '/' classifier ] ] ] ]
     * @param url The url
     * @return A new artifact
     * @throws IllegalArgumentException If the url is not valid
     */
    public static Artifact fromMvnUrl(final String url) {
        if ( url == null || !url.startsWith("mvn:") ) {
            throw new IllegalArgumentException("Invalid mvn url: " + url);
        }
        final String content = url.substring(4);
        // ignore repository url
        int pos = content.indexOf('!');
        if ( pos != -1 ) {
            throw new IllegalArgumentException("Repository url is not supported for Maven artifacts at the moment.");
        }
        final String coordinates = (pos == -1 ? content : content.substring(pos + 1));
        String gId = null;
        String aId = null;
        String version = null;
        String type = null;
        String classifier = null;
        int part = 0;
        String value = coordinates;
        while ( value != null ) {
            pos = value.indexOf('/');
            final String current;
            if ( pos == -1 ) {
                current = value;
                value = null;
            } else {
                if ( pos == 0 ) {
                    current = null;
                } else {
                    current = value.substring(0, pos);
                }
                value = value.substring(pos + 1);
            }
            if ( current != null ) {
                if ( part == 0 ) {
                    gId = current;
                } else if ( part == 1 ) {
                    aId = current;
                } else if ( part == 2 ) {
                    version = current;
                } else if ( part == 3 ) {
                    type = current;
                } else if ( part == 4 ) {
                    classifier = current;
                }
            }
            part++;
        }
        if ( version == null ) {
            version = "LATEST";
        }
        return new Artifact(gId, aId, version, classifier, type);
    }

    /**
     * Return a mvn url
     * @return A mvn url
     * @see #fromMvnUrl(String)
     */
    public String toMvnUrl() {
        final StringBuilder sb = new StringBuilder("mvn:");
        sb.append(this.groupId);
        sb.append('/');
        sb.append(this.artifactId);
        sb.append('/');
        sb.append(this.version);
        if ( this.classifier != null || !"jar".equals(this.type)) {
            sb.append('/');
            sb.append(this.type);
            if ( this.classifier != null ) {
                sb.append('/');
                sb.append(this.classifier);
            }
        }
        return sb.toString();
    }

    /**
     * Return the group id.
     * @return The group id.
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * Return the artifact id.
     * @return The artifact id.
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * Return the version.
     * @return The version.
     */
    public String getVersion() {
        return version;
    }

    /**
     * Return the optional classifier.
     * @return The classifier or null.
     */
    public String getClassifier() {
        return classifier;
    }

    /**
     * Return the type.
     * @return The type.
     */
    public String getType() {
        return type;
    }

    /**
     * Get the metadata of the artifact.
     * @return The metadata.
     */
    public Map<String, String> getMetadata() {
        return this.metadata;
    }

    /**
     * Create a Maven like relative repository path.
     * @return A relative repository path.
     */
    public String getRepositoryPath() {
        final StringBuilder sb = new StringBuilder();
        sb.append(groupId.replace('.', '/'));
        sb.append('/');
        sb.append(artifactId);
        sb.append('/');
        sb.append(version);
        sb.append('/');
        sb.append(artifactId);
        sb.append('-');
        sb.append(version);
        if ( classifier != null ) {
            sb.append('-');
            sb.append(classifier);
        }
        sb.append('.');
        sb.append(type);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Artifact [groupId=" + groupId
                + ", artifactId=" + artifactId
                + ", version=" + version
                + ", classifier=" + classifier
                + ", type=" + type
                + ( this.getLocation() != null ? ", location=" + this.getLocation() : "")
                + "]";
    }
}
