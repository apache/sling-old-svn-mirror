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
package org.apache.sling.maven.projectsupport;

import static org.apache.sling.maven.projectsupport.BundleListUtils.nodeValue;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.Bundle;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.Xpp3Dom;

/**
 * The definition of an artifact.
 */
public class ArtifactDefinition {

    /** The artifactId */
    private String artifactId;

    /** The classifier */
    private String classifier;

    /** The groupId */
    private String groupId;

    /** The start level at which this artifact should be started */
    private int startLevel;

    /** The artifact type */
    private String type;

    /** The artifact version */
    private String version;

    /** The artifact run modes */
    private String runModes;

    private ArtifactDefinition[] bundles;

    public ArtifactDefinition() {
    }

    public ArtifactDefinition(Bundle bundle, int startLevel) {
        this.groupId = bundle.getGroupId();
        this.artifactId = bundle.getArtifactId();
        this.type = bundle.getType();
        this.version = bundle.getVersion();
        this.classifier = bundle.getClassifier();
        this.startLevel = startLevel;
        this.runModes = bundle.getRunModes();
    }

    public ArtifactDefinition(final Xpp3Dom config) {
        if ( config.getChild("bundles") != null ) {
            final Xpp3Dom[] children = config.getChild("bundles").getChildren("bundle");
            this.bundles = new ArtifactDefinition[children.length];
            for(int i=0; i<this.bundles.length; i++) {
                this.bundles[i] = new ArtifactDefinition(children[i]);
            }
        } else {
            this.groupId = nodeValue(config, "groupId", null);
            this.artifactId = nodeValue(config, "artifactId", null);
            this.type = nodeValue(config, "type", null);
            this.version = nodeValue(config, "version", null);
            this.classifier = nodeValue(config, "classifier", null);
            this.startLevel = nodeValue(config, "startLevel", 0);
            this.runModes = nodeValue(config, "runModes", null);
        }
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getClassifier() {
        return classifier;
    }

    public String getGroupId() {
        return groupId;
    }

    public int getStartLevel() {
        return startLevel;
    }

    public String getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public String getRunModes() {
        return runModes;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public void setClassifier(String classifier) {
        this.classifier = classifier;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setStartLevel(int startLevel) {
        this.startLevel = startLevel;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "ArtifactDefinition [artifactId=" + artifactId + ", classifier="
                + classifier + ", groupId=" + groupId + ", startLevel="
                + startLevel + ", type=" + type + ", version=" + version
                + ", runModes=" + runModes + "]";
    }

    /**
     * Initialize this ArtifactDefinition with a set of default values from a
     * comma-delimited string. This string must have 6 items in it:
     * [groupId],[artifactId],[version],[type],[classifier],[startLevel]
     *
     * The only required parameter is the last one, which must be parseable as
     * an integer.
     *
     * @param commaDelimitedList
     *            the comma-delimited list
     */
    public void initDefaults(String commaDelimitedList) {
        String[] values = commaDelimitedList.split(",");
        if (values.length == 0 || values.length % 6 != 0 ) {
            throw new IllegalArgumentException(
                    String
                            .format(
                                    "The string %s does not have the correct number of items (a multiple of 6).",
                                    commaDelimitedList));
        }
        if ( values.length == 6 ) {
            initDefaults(values[0], values[1], values[2], values[3], values[4],
                    Integer.valueOf(values[5]));
        }
        this.bundles = new ArtifactDefinition[values.length / 6];
        for(int i=0; i<values.length / 6; i++) {
            this.bundles[i] = new ArtifactDefinition();
            this.bundles[i].initDefaults(values[i*6 + 0], values[i*6 + 1], values[i*6 + 2], values[i*6 + 3], values[i*6 + 4],
                    Integer.valueOf(values[i*6 + 5]));
        }
    }

    /**
     * Initialize this ArtifactDefinition with a set of default values. If the
     * corresponding field in this object is null (or 0 in the case of start
     * level) and the parameter is non-null, the parameter value will be used.
     *
     * @param groupId
     *            the groupId
     * @param artifactId
     *            the artifactId
     * @param version
     *            the version
     * @param type
     *            the artifact type
     * @param classifier
     *            the artifact classified
     * @param startLevel
     *            the start level
     */
    public void initDefaults(String groupId, String artifactId, String version,
            String type, String classifier, int startLevel) {
        if (this.bundles == null) {
            if (this.groupId == null && StringUtils.isNotEmpty(groupId)) {
                this.groupId = groupId;
            }
            if (this.artifactId == null && StringUtils.isNotEmpty(artifactId)) {
                this.artifactId = artifactId;
            }
            if (this.version == null && StringUtils.isNotEmpty(version)) {
                this.version = version;
            }
            if (this.type == null && StringUtils.isNotEmpty(groupId)) {
                this.type = type;
            }
            if (this.classifier == null && StringUtils.isNotEmpty(classifier)) {
                this.classifier = classifier;
            }
            if (this.startLevel == 0) {
                this.startLevel = startLevel;
            }
        } else {
            for (ArtifactDefinition bundle : this.bundles) {
                bundle.initDefaults(groupId, artifactId, version, type, classifier, startLevel);
            }
        }
    }

    public List<Bundle> toBundleList() {
        ArrayList<Bundle> bundleList = new ArrayList<Bundle>();

        if (bundles == null) {
            Bundle bnd = new Bundle();
            bnd.setArtifactId(artifactId);
            bnd.setGroupId(groupId);
            bnd.setVersion(version);
            if (type != null) {
                bnd.setType(type);
            }
            bnd.setClassifier(classifier);
            bnd.setStartLevel(startLevel);
            bundleList.add(bnd);
        } else {
            for (ArtifactDefinition bundle : bundles) {
                bundleList.addAll(bundle.toBundleList());
            }
        }

        return bundleList;
    }

    public List<Dependency> toDependencyList(String scope) {
        ArrayList<Dependency> depList = new ArrayList<Dependency>();

        if (bundles == null) {
            Dependency dep = new Dependency();
            dep.setArtifactId(artifactId);
            dep.setGroupId(groupId);
            dep.setVersion(version);
            if (type != null) {
                dep.setType(type);
            }
            dep.setClassifier(classifier);
            dep.setScope(scope);
            depList.add(dep);
        } else {
            for (ArtifactDefinition bundle : bundles) {
                depList.addAll(bundle.toDependencyList(scope));
            }
        }

        return depList;
    }

    public static List<Dependency> toDependencyList(Bundle bundle, String scope) {
        return new ArtifactDefinition(bundle, 0).toDependencyList(scope);
    }

}
