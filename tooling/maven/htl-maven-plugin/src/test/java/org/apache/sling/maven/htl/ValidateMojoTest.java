/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.maven.htl;

import java.io.File;
import java.util.List;

import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.SilentLog;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.sonatype.plexus.build.incremental.DefaultBuildContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ValidateMojoTest {

    private static final String ERROR_SLY = "src/main/resources/apps/projects/error.sly";
    private static final String WARNING_SLY = "src/main/resources/apps/projects/warning.sly";
    private static final String SCRIPT_HTML = "src/main/resources/apps/projects/script.html";
    private static final String EXCLUDE_HTML = "src/main/resources/apps/projects/exclude.html";
    private static final String TEST_PROJECT = "test-project";
    private static final String EXPLICIT_INCLUDES_POM = "explicit-includes.pom.xml";
    private static final String EXPLICIT_EXCLUDES_POM = "explicit-excludes.pom.xml";
    private static final String FAIL_ON_WARNINGS_POM = "fail-on-warnings.pom.xml";
    private static final String DEFAULT_INCLUDES_POM = "default-includes.pom.xml";

    @Rule
    public MojoRule mojoRule = new MojoRule() {
        @Override
        protected void before() throws Throwable {
            super.before();
            /*
             * Make sure the base directory is initialised properly for this test
             */
            System.setProperty("basedir", new File("src" + File.separator + "test" + File.separator + "resources" + File
                    .separator + TEST_PROJECT).getAbsolutePath());
        }
    };

    @Test
    public void testExplicitIncludes() throws Exception {
        File baseDir = new File(System.getProperty("basedir"));
        ValidateMojo validateMojo = getMojo(baseDir, EXPLICIT_INCLUDES_POM);
        try {
            validateMojo.execute();
        } catch (MojoFailureException e) {
            List<File> processedFiles = validateMojo.getProcessedFiles();
            assertEquals("Expected 2 files to process.", 2, processedFiles.size());
            assertTrue("Expected error.sly to be one of the processed files.", processedFiles.contains(new File(baseDir, ERROR_SLY)));
            assertTrue("Expected warning.sly to be one of the processed files.", processedFiles.contains(new File(baseDir, WARNING_SLY)));
            assertEquals("Expected compilation errors.", true, validateMojo.hasErrors());
            assertEquals("Expected compilation warnings.", true, validateMojo.hasWarnings());
        }
    }

    @Test
    public void testExplicitExcludes() throws Exception {
        File baseDir = new File(System.getProperty("basedir"));
        ValidateMojo validateMojo = getMojo(baseDir, EXPLICIT_EXCLUDES_POM);
        validateMojo.execute();
        List<File> processedFiles = validateMojo.getProcessedFiles();
        assertEquals("Expected 1 file to process.", 1, processedFiles.size());
        assertTrue("Expected script.html to be the only processed file.", processedFiles.contains(new File(baseDir, SCRIPT_HTML)));
        assertEquals("Did not expect compilation errors.", false, validateMojo.hasErrors());
        assertEquals("Did not expect compilation warnings.", false, validateMojo.hasWarnings());
    }

    @Test
    public void testFailOnWarnings() throws Exception {
        File baseDir = new File(System.getProperty("basedir"));
        ValidateMojo validateMojo = getMojo(baseDir, FAIL_ON_WARNINGS_POM);
        Exception exception = null;
        try {
            validateMojo.execute();
        } catch (MojoFailureException e) {
            exception = e;
        }
        List<File> processedFiles = validateMojo.getProcessedFiles();
        assertNotNull("Expected a MojoFailureException.", exception);
        assertEquals("Expected 1 file to process.", 1, processedFiles.size());
        assertTrue("Expected warning.sly to be one of the processed files.", processedFiles.contains(new File(baseDir, WARNING_SLY)));
        assertEquals("Expected compilation warnings.", true, validateMojo.hasWarnings());
    }

    @Test
    public void testDefaultIncludes() throws Exception {
        File baseDir = new File(System.getProperty("basedir"));
        ValidateMojo validateMojo = getMojo(baseDir, DEFAULT_INCLUDES_POM);
        Exception exception = null;
        try {
            validateMojo.execute();
        } catch (MojoFailureException e) {
            exception = e;
        }
        List<File> processedFiles = validateMojo.getProcessedFiles();
        assertNotNull("Expected a MojoFailureException.", exception);
        assertEquals("Expected 2 files to process.", 2, processedFiles.size());
        assertTrue("Expected exclude.html to be one of the processed files.", processedFiles.contains(new File(baseDir, EXCLUDE_HTML)));
        assertTrue("Expected script.html to be one of the processed files.", processedFiles.contains(new File(baseDir, SCRIPT_HTML)));
        assertEquals("Did not expect compilation warnings.", false, validateMojo.hasWarnings());
    }

    private ValidateMojo getMojo(File baseDir, String pomFile) throws Exception {
        SilentLog log = new SilentLog();
        DefaultBuildContext buildContext = new DefaultBuildContext();

        // use lookupConfiguredMojo to also consider default values (https://issues.apache.org/jira/browse/MPLUGINTESTING-23)
        // similar to MojoRule#lookupConfiguredMojo(File, String) but with custom pom file name
        MavenProject project = readMavenProject(baseDir, pomFile);
        MavenSession session = mojoRule.newMavenSession(project);
        MojoExecution execution = mojoRule.newMojoExecution("validate");
        ValidateMojo validateMojo = (ValidateMojo) mojoRule.lookupConfiguredMojo(session, execution);
        validateMojo.setLog(log);
        buildContext.enableLogging(log);
        validateMojo.setBuildContext(buildContext);
        return validateMojo;
    }

    /**
     * Copied from {@code org.apache.maven.plugin.testing.readMavenProject(...)} but customized to allow custom pom names
     */
    private MavenProject readMavenProject(File basedir, String pomFileName) throws Exception {
        File pom = new File(basedir, pomFileName);
        MavenExecutionRequest request = new DefaultMavenExecutionRequest();
        request.setBaseDirectory(basedir);
        ProjectBuildingRequest configuration = request.getProjectBuildingRequest();
        configuration.setRepositorySession(new DefaultRepositorySystemSession());
        MavenProject project = mojoRule.lookup(ProjectBuilder.class).build(pom, configuration).getProject();
        Assert.assertNotNull(project);
        return project;
    }
}
