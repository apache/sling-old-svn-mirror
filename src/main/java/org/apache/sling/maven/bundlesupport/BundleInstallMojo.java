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

package org.apache.sling.maven.bundlesupport;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Install an OSGi bundle to a running Sling instance.
 * The plugin places an HTTP POST request to
 * <a href="http://felix.apache.org/documentation/subprojects/apache-felix-web-console/web-console-restful-api.html#post-requests">Felix Web Console</a>.
 * It's also possible to HTTP PUT instead of POST leveraging the <a href="http://sling.apache.org/documentation/development/repository-based-development.html">WebDAV bundle from Sling</a>.
 * Since version 2.1.8 you can also leverage the the <a href="http://sling.apache.org/documentation/bundles/manipulating-content-the-slingpostservlet-servlets-post.html">Sling POST servlet</a>
 * for that The chosen method depends on the parameter {@link #deploymentMethod}.
 * <br>
 * <p><strong>Intermediate Node Creation</strong></p>
 * <p>
 * For all <code>deploymentMethod</code>s except WebDAV the bundle is not directly deployed within the OSGi container,
 * but rather being uploaded to the JCR and from there on being picked up by the
 * <a href="https://sling.apache.org/documentation/bundles/jcr-installer-provider.html">JCR Installer Provider</a> asynchronously, which takes care 
 * of deploying it in the OSGi container. For both other deployment methods, intermediate nodes (i.e. inexisting parent nodes) 
 * are automatically created. The primary type of those intermediate nodes depend on the deployment method.
 * </p>
 * <ul>
 * <li>
 *  WebDAV, uses the configured collection node type, by default <code>sling:Folder</code>
 *  (see also <a href="https://sling.apache.org/documentation/development/repository-based-development.html">WebDAV Configuration</a>)</li>
 * <li>
 *  SlingPostServlet, uses internally <code>ResourceResolverFactory.create(...)</code> without setting any <code>jcr:primaryType</code>.
 *  Therefore the <code>JcrResourceProviderFactory</code> will call <code>Node.addNode(String relPath)</code> which determines a fitting 
 *  node type automatically, depending on the parents node type definition (see <a href="http://www.day.com/specs/jsr170/javadocs/jcr-2.0/javax/jcr/Node.html#addNode%28java.lang.String%29">Javadoc</a>).
 *  So in most of the cases this should be a <code>sling:Folder</code>, as this is the first allowed child node definition in <code>sling:Folder</code>.
 *  This only may differ, if your existing parent node is not of type <code>sling:Folder</code> itself.
 * </li>
 * </ul>
 */
@Mojo(name = "install", defaultPhase = LifecyclePhase.INSTALL)
public class BundleInstallMojo extends AbstractBundleInstallMojo {

    /**
     * Whether to skip this step even though it has been configured in the
     * project to be executed. This property may be set by the
     * <code>sling.install.skip</code> comparable to the <code>maven.test.skip</code>
     * property to prevent running the unit tests.
     */
    @Parameter(property = "sling.install.skip", defaultValue = "false", required = true)
    private boolean skip;
    
    /**
     * The name of the generated JAR file.
     */
    @Parameter(property = "sling.file", defaultValue = "${project.build.directory}/${project.build.finalName}.jar", required = true)
    private String bundleFileName;

    @Override
    public void execute() throws MojoExecutionException {
        // don't do anything, if this step is to be skipped
        if (skip) {
            getLog().debug("Skipping bundle installation as instructed");
            return;
        }

        super.execute();
    }
    
    @Override
    protected String getBundleFileName() {
        return bundleFileName;
    }
}