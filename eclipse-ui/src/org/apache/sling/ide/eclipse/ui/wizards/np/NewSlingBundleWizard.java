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
import java.net.URL;

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.sling.ide.eclipse.core.EmbeddedArchetypeInstaller;
import org.apache.sling.ide.eclipse.ui.internal.Activator;
import org.apache.sling.ide.eclipse.ui.internal.SharedImages;
import org.eclipse.jface.resource.ImageDescriptor;

public class NewSlingBundleWizard extends AbstractNewSlingApplicationWizard {

	@Override
	public ImageDescriptor getLogo() {
		return SharedImages.SLING_LOG;
	}

	@Override
	public String doGetWindowTitle() {
		return "New Sling Bundle Project";
	}

	@Override
	public void installArchetypes() {
		// embedding the 1.0.1-SNAPSHOT cos the 1.0.0 doesn't include the pom
	    EmbeddedArchetypeInstaller archetypeInstaller = new EmbeddedArchetypeInstaller(
	    		"org.apache.sling", "sling-bundle-archetype", "slingclipse-embedded");
	    try {
	    	URL jarUrl = Activator.getDefault().getBundle().getResource(
	    			"target/archetypes/sling-bundle-archetype-1.0.1-SNAPSHOT.jar");
			archetypeInstaller.addResource("jar", jarUrl);
			URL pomUrl = Activator.getDefault().getBundle().getResource(
					"target/archetypes/sling-bundle-archetype-1.0.1-SNAPSHOT.pom");
			archetypeInstaller.addResource("pom", pomUrl);
			
			archetypeInstaller.installArchetype();
		} catch (IOException e) {
			// TODO proper logging
			e.printStackTrace();
		}
	}

	@Override
	public boolean acceptsArchetype(Archetype archetype) {
		return (archetype.getGroupId().equals("org.apache.sling") &&
				archetype.getArtifactId().equals("sling-bundle-archetype"));
	}

}
