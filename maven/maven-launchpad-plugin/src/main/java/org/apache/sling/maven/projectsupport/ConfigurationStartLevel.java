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
package org.apache.sling.maven.projectsupport;

import org.apache.maven.plugin.dependency.utils.DependencyUtil;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.artifact.filter.collection.ArtifactIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ClassifierFilter;
import org.apache.maven.shared.artifact.filter.collection.FilterArtifacts;
import org.apache.maven.shared.artifact.filter.collection.GroupIdFilter;
import org.apache.maven.shared.artifact.filter.collection.ProjectTransitivityFilter;
import org.apache.maven.shared.artifact.filter.collection.ScopeFilter;
import org.apache.maven.shared.artifact.filter.collection.TypeFilter;

public class ConfigurationStartLevel {
    
    private int level;

    private String includeTypes;

    private String excludeTypes;

    private String includeScope;

    private String excludeScope;

    private String includeClassifiers;

    private String excludeClassifiers;

    private String excludeArtifactIds;

    private String includeArtifactIds;

    private String excludeGroupIds;

    private String includeGroupIds;

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public String getIncludeTypes() {
        return includeTypes;
    }

    public void setIncludeTypes(String includeTypes) {
        this.includeTypes = includeTypes;
    }

    public String getExcludeTypes() {
        return excludeTypes;
    }

    public void setExcludeTypes(String excludeTypes) {
        this.excludeTypes = excludeTypes;
    }

    public String getIncludeScope() {
        return includeScope;
    }

    public void setIncludeScope(String includeScope) {
        this.includeScope = includeScope;
    }

    public String getExcludeScope() {
        return excludeScope;
    }

    public void setExcludeScope(String excludeScope) {
        this.excludeScope = excludeScope;
    }

    public String getIncludeClassifiers() {
        return includeClassifiers;
    }

    public void setIncludeClassifiers(String includeClassifiers) {
        this.includeClassifiers = includeClassifiers;
    }

    public String getExcludeClassifiers() {
        return excludeClassifiers;
    }

    public void setExcludeClassifiers(String excludeClassifiers) {
        this.excludeClassifiers = excludeClassifiers;
    }

    public String getExcludeArtifactIds() {
        return excludeArtifactIds;
    }

    public void setExcludeArtifactIds(String excludeArtifactIds) {
        this.excludeArtifactIds = excludeArtifactIds;
    }

    public String getIncludeArtifactIds() {
        return includeArtifactIds;
    }

    public void setIncludeArtifactIds(String includeArtifactIds) {
        this.includeArtifactIds = includeArtifactIds;
    }

    public String getExcludeGroupIds() {
        return excludeGroupIds;
    }

    public void setExcludeGroupIds(String excludeGroupIds) {
        this.excludeGroupIds = excludeGroupIds;
    }

    public String getIncludeGroupIds() {
        return includeGroupIds;
    }

    public void setIncludeGroupIds(String includeGroupIds) {
        this.includeGroupIds = includeGroupIds;
    }

    public FilterArtifacts buildFilter(MavenProject project) {
        // add filters in well known order, least specific to most specific
        FilterArtifacts filter = new FilterArtifacts();

        filter.addFilter(new ProjectTransitivityFilter(project.getDependencyArtifacts(), true));

        filter.addFilter(new ScopeFilter(DependencyUtil.cleanToBeTokenizedString(this.includeScope), DependencyUtil
                .cleanToBeTokenizedString(this.excludeScope)));

        filter.addFilter(new TypeFilter(DependencyUtil.cleanToBeTokenizedString(this.includeTypes), DependencyUtil
                .cleanToBeTokenizedString(this.excludeTypes)));

        filter.addFilter(new ClassifierFilter(DependencyUtil.cleanToBeTokenizedString(this.includeClassifiers),
                DependencyUtil.cleanToBeTokenizedString(this.excludeClassifiers)));

        filter.addFilter(new GroupIdFilter(DependencyUtil.cleanToBeTokenizedString(this.includeGroupIds),
                DependencyUtil.cleanToBeTokenizedString(this.excludeGroupIds)));

        filter.addFilter(new ArtifactIdFilter(DependencyUtil.cleanToBeTokenizedString(this.includeArtifactIds),
                DependencyUtil.cleanToBeTokenizedString(this.excludeArtifactIds)));
        return filter;
    }
    
    

}
