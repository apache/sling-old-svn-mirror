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

/**
 * Description of an artifact.
 */
public class SSMArtifact {

    public final String groupId;
    public final String artifactId;
    public final String version;
    public final String classifier;
    public final String type;

    public SSMArtifact(final String gId, final String aId, final String version,
            final String classifier, final String type) {
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

    /**
     * validates the object and throws an IllegalStateException
     * This object needs:
     * - groupId
     * - artifactId
     * - version
     * If type is null, it's set to "jar"
     * If type is "bundle", it's set to "jar"
     * - classifier is optional
     *
     * @throws IllegalStateException
     */
    public void validate() {
        // check/correct values
        if ( groupId == null || groupId.isEmpty() ) {
            throw new IllegalStateException(this + " : groupId");
        }
        if ( artifactId == null || artifactId.isEmpty() ) {
            throw new IllegalStateException(this + " : artifactId");
        }
        if ( version == null || version.isEmpty() ) {
            throw new IllegalStateException(this + " : version");
        }
        if ( type == null || type.isEmpty() ) {
            throw new IllegalStateException(this + " : type");
        }
    }

    public static SSMArtifact fromMvnUrl(final String url) {
        // 'mvn:' [ repository-url '!' ] group-id '/' artifact-id [ '/' [version] [ '/' [type] [ '/' classifier ] ] ] ]
        final String content = url.substring(4);
        // ignore repository url
        int pos = content.indexOf('!');
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
        final SSMArtifact ad = new SSMArtifact(gId, aId, version, classifier, type);
        return ad;
    }

    @Override
    public String toString() {
        return "SSMArtifact [groupId=" + groupId + ", artifactId=" + artifactId
                + ", version=" + version + ", classifier=" + classifier
                + ", type=" + type + "]";
    }
}
