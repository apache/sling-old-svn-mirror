/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.maven.sightly;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.sling.scripting.sightly.compiler.CompilationResult;
import org.apache.sling.scripting.sightly.compiler.CompilationUnit;
import org.apache.sling.scripting.sightly.compiler.CompilerMessage;
import org.apache.sling.scripting.sightly.compiler.SightlyCompiler;
import org.codehaus.plexus.util.FileUtils;
import org.codehaus.plexus.util.StringUtils;

/**
 * This goal validates Sightly scripts syntax.
 */
@Mojo(
        name = "validate",
        defaultPhase = LifecyclePhase.COMPILE,
        threadSafe = true
)
public class ValidateMojo extends AbstractMojo {

    private static final String DEFAULT_INCLUDES = "**/*.html";
    private static final String DEFAULT_EXCLUDES = "";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    /**
     * Defines the root folder where this Mojo expects to find Sightly scripts to validate.
     */
    @Parameter(property = "sourceDirectory", defaultValue = "${project.build.sourceDirectory}")
    private File sourceDirectory;

    /**
     * List of files to include. Specified as fileset patterns which are relative to the input directory whose contents will be scanned
     * (see the sourceDirectory configuration option).
     */
    @Parameter
    private String[] includes;

    /**
     * List of files to exclude. Specified as fileset patterns which are relative to the input directory whose contents will be scanned
     * (see the sourceDirectory configuration option).
     */
    @Parameter
    private String[] excludes;

    /**
     * If set to "true" it will fail the build on compiler warnings.
     */
    @Parameter(property = "failOnWarnings", defaultValue = "false")
    private boolean failOnWarnings;

    private boolean hasWarnings = false;
    private boolean hasErrors = false;
    private String processedIncludes = null;
    private String processedExcludes = null;
    private List<File> processedFiles = Collections.emptyList();

    private int sourceDirectoryLength = 0;

    public void execute() throws MojoExecutionException, MojoFailureException {
        if (!sourceDirectory.isAbsolute()) {
            sourceDirectory = new File(project.getBasedir(), sourceDirectory.getPath());
        }
        if (!sourceDirectory.exists()) {
            throw new MojoExecutionException(
                    String.format("Configured sourceDirectory={%s} does not exist.", sourceDirectory.getAbsolutePath()));
        }
        if (!sourceDirectory.isDirectory()) {
            throw new MojoExecutionException(
                    String.format("Configured sourceDirectory={%s} is not a directory.", sourceDirectory.getAbsolutePath()));
        }
        sourceDirectoryLength = sourceDirectory.getAbsolutePath().length();
        processedIncludes = processIncludes();
        processedExcludes = processExcludes();
        try {
            SightlyCompiler compiler = new SightlyCompiler();
            processedFiles = FileUtils.getFiles(sourceDirectory, processedIncludes, processedExcludes);
            Map<String, CompilationResult> compilationResults = new HashMap<>();
            for (File script : processedFiles) {
                compilationResults.put(script.getAbsolutePath(), compiler.compile(getCompilationUnit(script)));
            }
            Log log = getLog();
            for (Map.Entry<String, CompilationResult> entry : compilationResults.entrySet()) {
                String script = entry.getKey();
                CompilationResult result = entry.getValue();
                if (result.getWarnings().size() > 0) {
                    for (CompilerMessage message : result.getWarnings()) {
                        log.warn(String.format("%s:[%d,%d] %s", script, message.getLine(), message.getColumn(), message.getMessage()));
                    }
                    hasWarnings = true;
                }
                if (result.getErrors().size() > 0) {
                    for (CompilerMessage message : result.getErrors()) {
                        String messageString = message.getMessage().replaceAll(System.lineSeparator(), "");
                        log.error(String.format("%s:[%d,%d] %s", script, message.getLine(), message.getColumn(), messageString));
                    }
                    hasErrors = true;
                }
            }
            if (hasWarnings && failOnWarnings) {
                throw new MojoFailureException("Compilation warnings were configured to fail the build.");
            }
            if (hasErrors) {
                throw new MojoFailureException("Please check the reported syntax errors.");
            }
        } catch (IOException e) {
            throw new MojoExecutionException(String.format("Cannot filter files from {%s} with includes {%s} and excludes {%s}.",
                    sourceDirectory.getAbsolutePath(), processedIncludes, processedExcludes), e);
        }

    }

    public File getSourceDirectory() {
        return sourceDirectory;
    }

    public boolean shouldFailOnWarnings() {
        return failOnWarnings;
    }

    public boolean hasWarnings() {
        return hasWarnings;
    }

    public boolean hasErrors() {
        return hasErrors;
    }

    public String getIncludes() {
        return processedIncludes;
    }

    public String getExcludes() {
        return processedExcludes;
    }

    public List<File> getProcessedFiles() {
        return processedFiles;
    }

    private String processIncludes() {
        if (includes == null) {
            return DEFAULT_INCLUDES;
        }
        return join(includes, ',');
    }

    private String processExcludes() {
        if (excludes == null) {
            return DEFAULT_EXCLUDES;
        }
        return join(excludes, ',');
    }

    private String join(String[] array, char joinChar) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int index = 0; index < array.length; index++) {
            stringBuilder.append(StringUtils.trim(array[index]));
            if (index < array.length - 1) {
                stringBuilder.append(joinChar);
            }
        }
        return stringBuilder.toString();
    }

    private CompilationUnit getCompilationUnit(final File file) throws FileNotFoundException {
        final Reader reader = new FileReader(file);
        return new CompilationUnit() {
            public String getScriptName() {
                return file.getAbsolutePath().substring(sourceDirectoryLength);
            }

            public Reader getScriptReader() {
                return reader;
            }
        };
    }
}
