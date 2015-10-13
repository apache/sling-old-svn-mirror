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
package org.apache.sling.ide.eclipse.core.internal;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.sling.ide.eclipse.core.facet.FacetHelper;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ProjectHelper {

    private static final String[] CONTENT_PACKAGE_STRUCTURE_BASE = new String[] { "/", "/content", "/src/main/content" };

	public static boolean isPotentialBundleProject(IProject project) {

        try {
            return !isBundleProject(project) && !isContentProject(project) && project.getDescription().hasNature(JavaCore.NATURE_ID);
        } catch (CoreException e) {
            Activator.getDefault().getPluginLogger().warn("Failed getting project description", e);
            return false;
        }
	}
	
	public static boolean isPotentialContentProject(IProject project) {

        return !isContentProject(project) && !isBundleProject(project) && getInferredContentProjectContentRoot(project) != null;
	}

    public static IContainer getInferredContentProjectContentRoot(IProject project) {

        for (String base : CONTENT_PACKAGE_STRUCTURE_BASE) {
            IContainer container;
            if ("/".equals(base)) {
                container = project;
            } else {
                container = project.getFolder(base);
            }
            if (container.exists() && hasContentPackageStructure(container)) {
                return container;
            }
        }

        return null;
    }

    public static String validateContentPackageStructure(IContainer base) {
        
        IFile filterXml = base.getFile(Path.fromPortableString("META-INF/vault/filter.xml"));
        IFolder jcrRoot = base.getFolder(Path.fromPortableString("jcr_root"));

        if (!filterXml.exists()) {
            return String.format("Could not find FileVault filter at '%s'", filterXml.getRawLocationURI());
        }

        if (!jcrRoot.exists()) {
            return String.format("Could not find JCR root at '%s'", jcrRoot.getRawLocationURI());
        }

        return null;
    }

    public static boolean hasContentPackageStructure(IContainer jcrRootDir) {

        return validateContentPackageStructure(jcrRootDir) == null;

    }
	
	public static String getMavenProperty(IProject project, String name) {
		try{
			DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			IFile file = project.getFile("pom.xml");
			if (file==null || !file.exists()) {
				return null;
			}
			Document document = docBuilder.parse(file.getContents());
			Element docElement = document.getDocumentElement();
			NodeList children = docElement.getChildNodes();
			for(int i=0; i<children.getLength(); i++) {
				Node aChild = children.item(i);
				if (aChild.getNodeName().equals(name)) {
					Element e = (Element) aChild;
					String text = e.getTextContent();
					return text;
				}
			}
		} catch (ParserConfigurationException e) {
            Activator.getDefault().getPluginLogger().warn("Failed getting maven property for " + project.getName(), e);
			return null;
		} catch (SAXException e) {
            Activator.getDefault().getPluginLogger().warn("Failed getting maven property for " + project.getName(), e);
			return null;
		} catch (IOException e) {
            Activator.getDefault().getPluginLogger().warn("Failed getting maven property for " + project.getName(), e);
			return null;
		} catch (CoreException e) {
            Activator.getDefault().getPluginLogger().warn("Failed getting maven property for " + project.getName(), e);
			return null;
		}
		return null;
	}
	
	public static boolean isBundleProject(IProject project) {
		return FacetHelper.containsFacet(project, SlingBundleModuleFactory.SLING_BUNDLE_FACET_ID);
	}

	public static boolean isContentProject(IProject project) {
		return FacetHelper.containsFacet(project, SlingContentModuleFactory.SLING_CONTENT_FACET_ID);
	}

	public static IJavaProject asJavaProject(IProject project) {
		return JavaCore.create(project);
	}
	
	static IJavaProject[] getAllJavaProjects() {
		IJavaModel model = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
		IJavaProject[] jps;
		try {
			jps = model.getJavaProjects();
			return jps;
		} catch (JavaModelException e) {
			e.printStackTrace();
			return null;
		}
	}

}
