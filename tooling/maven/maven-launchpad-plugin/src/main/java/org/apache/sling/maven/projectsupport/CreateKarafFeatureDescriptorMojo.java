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
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.Bundle;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.StartLevel;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Create and attach a karaf feature descriptor XML file.
 */
@Mojo(name = "create-karaf-descriptor", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.TEST)
public class CreateKarafFeatureDescriptorMojo extends AbstractUsingBundleListMojo {

    private static final String CLASSIFIER = "features";

    private static final String TYPE = "xml";

    @Parameter(defaultValue = "sling")
    private String featureName;

    @Parameter(defaultValue = "sling-2.0")
    private String featuresName;

    @Parameter(defaultValue = "${project.version}")
    private String featureVersion;

    @Parameter(defaultValue = "{project.build.directory}/features.xml")
    private File outputFile;

    @Override
    protected void executeWithArtifacts() throws MojoExecutionException, MojoFailureException {
        Document doc = new Document();

        Element features = new Element("features");
        doc.setRootElement(features);
        features.setAttribute("name", featuresName);

        Element feature = new Element("feature");
        features.addContent(feature);
        feature.setAttribute("name", featureName);
        feature.setAttribute("version", featureVersion);

        BundleList bundleList = getInitializedBundleList();
        for (StartLevel level : bundleList.getStartLevels()) {
            for (Bundle bundle : level.getBundles()) {
                String bundleRef = String.format("mvn:%s/%s/%s", bundle.getGroupId(), bundle.getArtifactId(), bundle
                        .getVersion());
                feature.addContent(new Element("bundle").setText(bundleRef));
            }
        }

        FileOutputStream out = null;
        try {
            out = new FileOutputStream(outputFile);
            new XMLOutputter(Format.getPrettyFormat().setEncoding("UTF-8")).output(doc, out);
            projectHelper.attachArtifact(project, TYPE, CLASSIFIER, outputFile);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to write features.xml", e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        }

    }

}
