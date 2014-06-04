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
package org.apache.sling.ide.eclipse.ui.wizards.np;

import java.io.IOException;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.sling.ide.artifacts.EmbeddedArtifact;
import org.apache.sling.ide.artifacts.EmbeddedArtifactLocator;
import org.apache.sling.ide.eclipse.m2e.EmbeddedArchetypeInstaller;
import org.apache.sling.ide.eclipse.m2e.internal.Activator;
import org.apache.sling.ide.eclipse.ui.WhitelabelSupport;
import org.eclipse.core.runtime.CoreException;

public class NewSlingBundleWizard extends AbstractNewMavenBasedSlingApplicationWizard {

	@Override
	public String doGetWindowTitle() {
        return "New " + WhitelabelSupport.getProductName() + " Bundle Project";
	}

	@Override
	public void installArchetypes() {

        EmbeddedArtifactLocator artifactsLocator = Activator.getDefault().getArtifactsLocator();

        // TODO - should we remove the special slingclipse-embedded artifact and simply install our version?
	    EmbeddedArchetypeInstaller archetypeInstaller = new EmbeddedArchetypeInstaller(
	    		"org.apache.sling", "sling-bundle-archetype", "slingclipse-embedded");
	    try {

            EmbeddedArtifact[] archetypeArtifacts = artifactsLocator.loadSlingBundleArchetype();

            archetypeInstaller.addResource("pom", archetypeArtifacts[0].openInputStream());
            archetypeInstaller.addResource("jar", archetypeArtifacts[1].openInputStream());
			
			archetypeInstaller.installArchetype();
		} catch (IOException e) {
            reportError(e);
        } catch (CoreException e) {
            reportError(e);
        }
	}

	@Override
	public boolean acceptsArchetype(Archetype archetype) {
		return (archetype.getGroupId().equals("org.apache.sling") &&
				archetype.getArtifactId().equals("sling-bundle-archetype"));
	}

}
