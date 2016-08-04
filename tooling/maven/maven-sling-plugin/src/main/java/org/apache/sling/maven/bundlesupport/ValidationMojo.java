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
import java.util.Iterator;

import org.apache.commons.io.IOUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.util.Validator;
import org.codehaus.plexus.util.DirectoryScanner;

/**
 * Plugin to validate resources:
 * - validate json files
 */
@Mojo(name = "validate", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class ValidationMojo extends AbstractMojo {

    /**
     * The Maven project.
     */
    @Parameter( defaultValue = "${project}", readonly = true )
    private MavenProject project;

    /**
     * Whether to skip the validation
     */
    @Parameter(property = "sling.validation.skip", defaultValue = "false", required = true)
    private boolean skip;

    /**
     * Whether to skip the json validation
     */
    @Parameter(property = "sling.validation.skipJson", defaultValue = "false", required = true)
    private boolean skipJson;

    /**
     * @see org.apache.maven.plugin.AbstractMojo#execute()
     */
    public void execute()
    throws MojoExecutionException {
        if ( this.skip ) {
            getLog().info("Validation is skipped.");
            return;
        }
        @SuppressWarnings("unchecked")
        final Iterator<Resource> rsrcIterator = this.project.getResources().iterator();
        while ( rsrcIterator.hasNext() ) {
            final Resource rsrc = rsrcIterator.next();

            final File directory = new File(rsrc.getDirectory());
            if ( directory.exists() ) {
                getLog().debug("Scanning " + rsrc.getDirectory());
                final DirectoryScanner scanner = new DirectoryScanner();
                scanner.setBasedir( directory );

                if ( rsrc.getExcludes() != null && rsrc.getExcludes().size() > 0 ) {
                    scanner.setExcludes( (String[]) rsrc.getExcludes().toArray(new String[rsrc.getExcludes().size()] ) );
                }
                scanner.addDefaultExcludes();
                if ( rsrc.getIncludes() != null && rsrc.getIncludes().size() > 0 ) {
                    scanner.setIncludes( (String[]) rsrc.getIncludes().toArray(new String[rsrc.getIncludes().size()] ));
                }

                scanner.scan();

                final String[] files = scanner.getIncludedFiles();
                if ( files != null ) {
                    for(int m=0; m<files.length; m++) {
                        this.validate(directory, files[m]);
                    }
                }
            }
        }
    }

    private void validate(final File directory, final String fileName)
    throws MojoExecutionException {
        getLog().debug("Validating " + fileName);
        final File file = new File(directory, fileName);
        if ( file.isFile() ) {
            if ( fileName.endsWith(".json") && !this.skipJson ) {
                getLog().debug("Validation JSON file " + fileName);
                FileInputStream fis = null;
                String json = null;
                try {
                    fis = new FileInputStream(file);
                    json = IOUtils.toString(fis);
                } catch (IOException e) {
                    throw new MojoExecutionException("An Error occured while validating the file '"+fileName+"'", e);
                } finally {
                    IOUtils.closeQuietly(fis);
                }
                // first, let's see if this is a json array
                try {
                    Validator.validate(json);
                } catch (JSONException e) {
                    throw new MojoExecutionException("An Error occured while validating the file '"+fileName+"'", e);
                }
            }
        }
    }
}
