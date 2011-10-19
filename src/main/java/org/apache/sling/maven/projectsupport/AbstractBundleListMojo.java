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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.io.xpp3.BundleListXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

public abstract class AbstractBundleListMojo extends AbstractMojo {

    /**
     * Partial Bundle List type
     */
    protected static final String PARTIAL = "partialbundlelist";

    /**
     * @parameter default-value="${basedir}/src/main/bundles/list.xml"
     */
    protected File bundleListFile;

    /**
     * The Maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * @component
     */
    protected MavenProjectHelper projectHelper;

    /**
     * @parameter expression="${configDirectory}"
     *            default-value="src/main/config"
     */
    private File configDirectory;

    /**
     * @parameter expression="${commonSlingProps}"
     *            default-value="src/main/sling/common.properties"
     */
    protected File commonSlingProps;

    /**
     * @parameter expression="${commonSlingBootstrap}"
     *            default-value="src/main/sling/common.bootstrap.txt"
     */
    protected File commonSlingBootstrap;

    /**
     * @parameter expression="${webappSlingProps}"
     *            default-value="src/main/sling/webapp.properties"
     */
    protected File webappSlingProps;

    /**
     * @parameter expression="${webappSlingBootstrap}"
     *            default-value="src/main/sling/webapp.bootstrap.txt"
     */
    protected File webappSlingBootstrap;

    /**
     * @parameter expression="${standaloneSlingProps}"
     *            default-value="src/main/sling/standalone.properties"
     */
    protected File standaloneSlingProps;

    /**
     * @parameter expression="${standaloneSlingBootstrap}"
     *            default-value="src/main/sling/standalone.bootstrap.txt"
     */
    protected File standaloneSlingBootstrap;

    /**
     * @parameter expression="${ignoreBundleListConfig}"
     *            default-value="false"
     */
    protected boolean ignoreBundleListConfig;

    /**
     * The start level to be used when generating the bundle list.
     * 
     * @parameter default-value="-1"
     */
    private int dependencyStartLevel;

    protected File getConfigDirectory() {
        return this.configDirectory;
    }

    protected BundleList readBundleList(File file) throws IOException, XmlPullParserException {
        BundleListXpp3Reader reader = new BundleListXpp3Reader();
        FileInputStream fis = new FileInputStream(file);
        try {
            return reader.read(fis);
        } finally {
            fis.close();
        }
    }

    @SuppressWarnings("unchecked")
    protected void addDependencies(final BundleList bundleList) {
        if (dependencyStartLevel >= 0) {
            final List<Dependency> dependencies = project.getDependencies();
            for (Dependency dependency : dependencies) {
                if (!PARTIAL.equals(dependency.getType())) {
                    bundleList.add(ArtifactDefinition.toBundle(dependency, dependencyStartLevel));
                }
            }
        }
    }
}
