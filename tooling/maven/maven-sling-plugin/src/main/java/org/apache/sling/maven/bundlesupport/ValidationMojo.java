/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.maven.bundlesupport;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.JsonException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Scanner;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * The <code>validate</code> goal checks the JSON code of a bundle.
 */
@Mojo(name = "validate", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class ValidationMojo extends AbstractMojo {

    private static final Pattern LINE_NUMBER_PATTERN = Pattern.compile("lineNumber=(\\d+),");
    private static final Pattern COLUMN_NUMBER_PATTERN = Pattern.compile("columnNumber=(\\d+),");
    private static final Pattern MESSAGE_CLEANUP_PATTERN = Pattern.compile("^(.*) on \\[lineNumber=\\d+, columnNumber=\\d+, streamOffset=\\d+\\](.*)$", Pattern.DOTALL);
    
    /**
     * The Maven project.
     */
    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;

    /**
     * Whether to skip the validation. 
     */
    @Parameter(property = "sling.validation.skip", defaultValue = "false", required = true)
    private boolean skip;

    /**
     * Whether to skip the json validation.
     * At the time, there's no difference between <code>skip</code> and <code>skipJson</code> because only JSON files will be validated by now.
     */
    @Parameter(property = "sling.validation.skipJson", defaultValue = "false", required = true)
    private boolean skipJson;

    /**
     * Whether to accept quote ticks in JSON files or not. 
     */
    @Parameter(property = "sling.validation.jsonQuoteTick", defaultValue = "false", required = false)
    private boolean jsonQuoteTick;

    @Component
    private BuildContext buildContext;
    
    /**
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        if ( this.skip ) {
            getLog().info("Validation is skipped.");
            return;
        }
        
        final Iterator<Resource> rsrcIterator = this.project.getResources().iterator();
        while ( rsrcIterator.hasNext() ) {
            final Resource rsrc = rsrcIterator.next();

            final File directory = new File(rsrc.getDirectory());
            if ( directory.exists() ) {

                if (!buildContext.hasDelta(directory)) {
                    getLog().debug("No files found to validate, skipping.");
                    return;
                }
                
                getLog().debug("Scanning " + rsrc.getDirectory());
                final Scanner scanner = buildContext.newScanner(directory);

                if ( rsrc.getExcludes() != null && rsrc.getExcludes().size() > 0 ) {
                    scanner.setExcludes( (String[]) rsrc.getExcludes().toArray(new String[rsrc.getExcludes().size()] ) );
                }
                scanner.addDefaultExcludes();
                if ( rsrc.getIncludes() != null && rsrc.getIncludes().size() > 0 ) {
                    scanner.setIncludes( (String[]) rsrc.getIncludes().toArray(new String[rsrc.getIncludes().size()] ));
                }

                scanner.scan();

                final String[] files = scanner.getIncludedFiles();
                int countProcessed = 0;
                List<Exception> failures = new ArrayList<>();
                if ( files != null ) {
                    for(int m=0; m<files.length; m++) {
                        final File file = new File(directory, files[m]);
                        buildContext.removeMessages(file);
                        try {
                            this.validate(file);
                        }
                        catch (Exception ex) {
                            failures.add(ex);
                            buildContext.addMessage(file,
                                    parseLineNumber(ex.getMessage()),
                                    parseColumnNumber(ex.getMessage()), 
                                    cleanupMessage(ex.getMessage()),
                                    BuildContext.SEVERITY_ERROR,
                                    ex.getCause());
                        }
                        countProcessed++;
                    }
                }
                
                if (!failures.isEmpty()) {
                    if (!buildContext.isIncremental()) {
                        throw new MojoFailureException("Validated " + countProcessed + " file(s), found " + failures.size() + " failures.");
                    }
                }
                else {
                    getLog().info("Validated " + countProcessed + " file(s).");
                }
            }
        }
    }

    private void validate(final File file) throws MojoExecutionException {
        getLog().debug("Validating " + file.getPath());
        if ( file.isFile() ) {
            if ( file.getName().endsWith(".json") && !this.skipJson ) {
                getLog().debug("Validation JSON file " + file.getPath());
                FileInputStream fis = null;
                String json = null;
                try {
                    fis = new FileInputStream(file);
                    json = IOUtils.toString(fis, CharEncoding.UTF_8);
                } catch (IOException ex) {
                    throw new MojoExecutionException(ex.getMessage(), ex);
                } finally {
                    IOUtils.closeQuietly(fis);
                }
                // validate JSON
                try {
                    JsonSupport.validateJsonStructure(json, jsonQuoteTick);
                } catch (JsonException ex) {
                    throw new MojoExecutionException("Invalid JSON: " + ex.getMessage());
                }
            }
        }
    }
    
    static int parseLineNumber(String message) {
        return parseNumber(message, LINE_NUMBER_PATTERN);
    }
    
    static int parseColumnNumber(String message) {
        return parseNumber(message, COLUMN_NUMBER_PATTERN);
    }

    static int parseNumber(String message, Pattern pattern) {
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return NumberUtils.toInt(matcher.group(1));
        }
        else {
            return 0;
        }
    }
    
    static String cleanupMessage(String message) {
        String result;
        Matcher matcher = MESSAGE_CLEANUP_PATTERN.matcher(message);
        if (matcher.matches()) {
            result = matcher.group(1) + matcher.group(2);
        }
        else {
            result = message;
        }
        result = StringUtils.replace(result, "\n", "\\n");
        result = StringUtils.replace(result, "\r", "");
        return result;
    }

}
