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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;

public abstract class AbstractBundleListMojo extends AbstractMojo {

    /**
     * @parameter expression="${configDirectory}"
     *            default-value="src/main/config"
     */
    protected File configDirectory;

    /**
     * JAR Packaging type.
     */
    protected static final String JAR = "jar";

    /**
     * WAR Packaging type.
     */
    protected static final String WAR = "war";

    /**
     * Partial Bundle List type
     */
    protected static final String PARTIAL = "partialbundlelist";

    protected static boolean shouldCopy(File source, File dest) {
        if (!dest.exists()) {
            return true;
        } else {
            return source.lastModified() > dest.lastModified();
        }
    }

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

}
