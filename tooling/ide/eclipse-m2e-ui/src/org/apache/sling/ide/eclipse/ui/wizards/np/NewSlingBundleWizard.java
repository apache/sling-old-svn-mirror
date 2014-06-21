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

import org.apache.maven.archetype.catalog.Archetype;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.sling.ide.eclipse.ui.WhitelabelSupport;

public class NewSlingBundleWizard extends AbstractNewMavenBasedSlingApplicationWizard {

	@Override
	public String doGetWindowTitle() {
        return "New " + WhitelabelSupport.getProductName() + " Bundle Project";
	}

	@Override
	public void installArchetypes() {

        // rely on public archetypes only
	}

	@Override
	public boolean acceptsArchetype(Archetype archetype) {

		boolean isSlingBundleArchetype = archetype.getGroupId().equals("org.apache.sling") &&
				archetype.getArtifactId().equals("sling-bundle-archetype");
		
		if ( !isSlingBundleArchetype ) {
		    return false;
		}
		
        DefaultArtifactVersion version = new DefaultArtifactVersion(archetype.getVersion());

        // release 1.0.2 is the first known good release
        if (version.compareTo(new DefaultArtifactVersion("1.0.2")) < 0) {
            return false;
        }

        return true;
		
	}

}
