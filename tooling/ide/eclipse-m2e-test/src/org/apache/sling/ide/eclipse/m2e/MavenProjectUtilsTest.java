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
package org.apache.sling.ide.eclipse.m2e;

import static org.hamcrest.CoreMatchers.equalTo;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.sling.ide.eclipse.core.ProjectUtil;
import org.apache.sling.ide.eclipse.m2e.impl.helpers.MavenProjectAdapter;
import org.apache.sling.ide.test.impl.helpers.Poller;
import org.apache.sling.ide.test.impl.helpers.TemporaryProject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.junit.Rule;
import org.junit.Test;

public class MavenProjectUtilsTest {

    @Rule
    public TemporaryProject projectRule = new TemporaryProject();
	
	@Test
	public void inferDefaultProvisioningModelDirectories() throws Exception {

		IPath modelsDir = Path.fromPortableString("src/main/provisioning");
		
		// create project
        final IProject launchpadProject = projectRule.getProject();

        MavenProjectAdapter project = new MavenProjectAdapter(launchpadProject);
        project.createOrUpdateFile(Path.fromPortableString("pom.xml"), getClass().getResourceAsStream("slingstart-simple-pom.xml"));
		project.ensureDirectoryExists(modelsDir);
        project.convertToMavenProject();

        // conversion should enable the slingstart configurator and set the provisioning model path
        new Poller(TimeUnit.MINUTES.toMillis(1)).pollUntil(new Callable<IPath>() {
			@Override
			public IPath call() throws Exception {
				return ProjectUtil.getProvisioningModelPath(launchpadProject);
			}
		}, equalTo(modelsDir));
        
	}
}
