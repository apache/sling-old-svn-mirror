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
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.Bundle;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.StartLevel;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * Create and attach a karaf feature descriptor XML file.
 * 
 * @goal create-karaf-descriptor
 * @phase package
 * @description create a karaf feature descriptor
 */
public class CreateKarafFeatureDescriptorMojo extends AbstractBundleListMojo {

	private static final String CLASSIFIER = "features";

	private static final String TYPE = "xml";

	/**
	 * @parameter
	 */
	private String[] additionalBundles;

	private Set<String> excludedArtifacts;
	
	/**
	 * @parameter
	 */
	private String[] exclusions;
	
	/**
	 * @parameter default-value="sling"
	 */
	private String featureName;
	
	/**
	 * @parameter default-value="sling-2.0"
	 */
	private String featuresName;

	/**
	 * @parameter default-value="${project.version}"
	 */
	private String featureVersion;

	/**
	 * The output directory.
	 * 
	 * @parameter default-value="${project.build.directory}"
	 */
	private File outputDirectory;

	@Override
	protected void executeWithArtifacts() throws MojoExecutionException,
			MojoFailureException {
		Document doc = new Document();

		Element features = new Element("features");
		doc.setRootElement(features);
		features.setAttribute("name", featuresName);

		Element feature = new Element("feature");
		features.addContent(feature);
		feature.setAttribute("name", featureName);
		feature.setAttribute("version", featureVersion);
		
		excludedArtifacts = new HashSet<String>();
		if (exclusions != null) {
			excludedArtifacts.addAll(Arrays.asList(exclusions));
		}

		try {
			BundleList bundleList = readBundleList();
			for (StartLevel level : bundleList.getStartLevels()) {
				for (Bundle bundle : level.getBundles()) {
					if (include(bundle)) {
						String bundleRef = String.format("mvn:%s/%s/%s", bundle
								.getGroupId(), bundle.getArtifactId(), bundle
								.getVersion());
						feature.addContent(new Element("bundle")
								.setText(bundleRef));
					}
				}
			}
		} catch (IOException e) {
			throw new MojoExecutionException("Unable to read bundle list file",
					e);
		} catch (XmlPullParserException e) {
			throw new MojoExecutionException("Unable to read bundle list file",
					e);
		}

		if (additionalBundles != null) {
			for (String bundleRef : additionalBundles) {
				Element bundle = new Element("bundle");
				bundle.setText(bundleRef);
				feature.addContent(bundle);
			}
		}

		File outputFile = new File(outputDirectory, "features.xml");

		FileOutputStream out = null;
		try {
			out = new FileOutputStream(outputFile);
			new XMLOutputter(Format.getPrettyFormat().setEncoding("UTF-8"))
					.output(doc, out);
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
		projectHelper.attachArtifact(project, TYPE, CLASSIFIER, outputFile);

	}

	/**
	 * Decide if the bundle should be included in the bundle list.
	 * 
	 * @param bundle the bundle
	 * @return true if it should be included
	 */
	private boolean include(Bundle bundle) {
		String ref = bundle.getGroupId() + ":" + bundle.getArtifactId();
		return !excludedArtifacts.contains(ref);
	}

}
