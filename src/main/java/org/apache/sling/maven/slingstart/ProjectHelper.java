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
package org.apache.sling.maven.slingstart;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.sling.provisioning.model.Artifact;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelUtility;
import org.apache.sling.provisioning.model.ModelUtility.ResolverOptions;
import org.apache.sling.provisioning.model.io.ModelReader;
import org.apache.sling.provisioning.model.io.ModelWriter;

public abstract class ProjectHelper {

    /** The raw local model. */
    private static final String RAW_MODEL_TXT = Model.class.getName() + "/raw.txt";
    private static final String RAW_MODEL_CACHE = Model.class.getName() + "/raw.cache";

    private static final String EFFECTIVE_MODEL_TXT = Model.class.getName() + "/effective.txt";
    private static final String EFFECTIVE_MODEL_CACHE = Model.class.getName() + "/effective.cache";

    private static final String DEPENDENCY_MODEL = Model.class.getName() + "/dependency";

    /**
     * Store all relevant information about the project for plugins to be
     * retrieved
     * @param info The project info
     * @throws IOException If writing fails
     */
    public static void storeProjectInfo(final ModelPreprocessor.ProjectInfo info)
    throws IOException {
        // we have to serialize as the dependency lifecycle participant uses a different class loader (!)
        final StringWriter w1 = new StringWriter();
        ModelWriter.write(w1, info.localModel);
        info.project.setContextValue(RAW_MODEL_TXT, w1.toString());

        final StringWriter w2 = new StringWriter();
        ModelWriter.write(w2, info.model);
        info.project.setContextValue(EFFECTIVE_MODEL_TXT, w2.toString());

        // create map with model dependencies
        final Map<String, String> map = new HashMap<String, String>();
        for(final Map.Entry<Artifact, Model> entry : info.includedModels.entrySet()) {
            final StringWriter w3 = new StringWriter();
            ModelWriter.write(w3, entry.getValue());
            map.put(entry.getKey().toMvnUrl(), w3.toString());
        }
        info.project.setContextValue(DEPENDENCY_MODEL, map);
    }

    /**
     * Get the effective model from the project
     * @param project The maven projet
     * @return The effective model
     * @throws MojoExecutionException If reading fails
     */
    public static Model getEffectiveModel(final MavenProject project, ResolverOptions resolverOptions)
    throws MojoExecutionException {
        Model result = (Model) project.getContextValue(EFFECTIVE_MODEL_CACHE);
        if ( result == null ) {
            try {
                final StringReader r = new StringReader((String)project.getContextValue(EFFECTIVE_MODEL_TXT));
                result = ModelReader.read(r, project.getId());
                result = ModelUtility.getEffectiveModel(result, resolverOptions);
                project.setContextValue(EFFECTIVE_MODEL_CACHE, result);
            } catch ( final IOException ioe) {
                throw new MojoExecutionException(ioe.getMessage(), ioe);
            }
        }
        return result;
    }

    /**
     * Get the raw model from the project
     * @param project The maven projet
     * @return The raw local model
     * @throws MojoExecutionException If reading fails
     */
    public static Model getRawModel(final MavenProject project)
    throws MojoExecutionException {
        Model result = (Model) project.getContextValue(RAW_MODEL_CACHE);
        if ( result == null ) {
            try {
                final StringReader r = new StringReader((String)project.getContextValue(RAW_MODEL_TXT));
                result = ModelReader.read(r, project.getId());
                project.setContextValue(RAW_MODEL_CACHE, result);
            } catch ( final IOException ioe) {
                throw new MojoExecutionException(ioe.getMessage(), ioe);
            }
        }
        return result;
    }

    /**
     * Get the dependency model from the project
     * @param project The maven projet
     * @return The dependency  model
     * @throws MojoExecutionException If reading fails
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> getDependencyModel(final MavenProject project)
    throws MojoExecutionException {
        return (Map<String, String>) project.getContextValue(DEPENDENCY_MODEL);
    }
}
