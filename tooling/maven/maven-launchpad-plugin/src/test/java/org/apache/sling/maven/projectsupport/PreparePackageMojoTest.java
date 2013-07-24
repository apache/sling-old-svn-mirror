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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.Test;

/**
 * Tests of PreparePageMojo
 */
public class PreparePackageMojoTest {

	@Test
	public void testInitArtifactDefinitionsAllDefaults() throws Exception {
		PreparePackageMojo mojo = new PreparePackageMojo();
		invokeInitArtifactDefinitions(mojo);

		makeArtifactAssertions(mojo, "base", "org.apache.sling",
				"org.apache.sling.launchpad.base", null, "jar", null, 0);

		//makeArtifactAssertions(mojo, "defaultBundles", "org.apache.sling",
		//		"org.apache.sling.launchpad", "RELEASE", "jar", "bundles", 0);

		makeArtifactAssertions(mojo, "defaultBundleList", "org.apache.sling",
		        "org.apache.sling.launchpad", "RELEASE", "xml", "bundlelist", 0);

		makeArtifactAssertions(mojo, "jarWebSupport", "org.apache.felix",
				"org.apache.felix.http.jetty", "RELEASE", "jar", null, -1);

	}

	private void makeArtifactAssertions(PreparePackageMojo mojo, String name,
			String groupId, String artifactId, String version, String type,
			String classifier, int startLevel) throws Exception {
		ArtifactDefinition def = getArtifactDefinition(mojo, name);

		assertNotNull(def);
		assertEquals(groupId, def.getGroupId());
		assertEquals(artifactId, def.getArtifactId());
		assertEquals(version, def.getVersion());
		assertEquals(type, def.getType());
		assertEquals(classifier, def.getClassifier());
		assertEquals(startLevel, def.getStartLevel());
	}

	private void invokeInitArtifactDefinitions(PreparePackageMojo mojo)
			throws Exception {
		Method method = findMethod(mojo.getClass(), "initArtifactDefinitions");
		method.setAccessible(true);
		method.invoke(mojo);
		method.setAccessible(false);
	}

	private Method findMethod(Class<?> clazz, String name, Class<?>... args)
			throws NoSuchMethodException {
		while (clazz != Object.class) {
			try {
				return clazz.getDeclaredMethod(name, args);
			} catch (SecurityException e) {
			} catch (NoSuchMethodException e) {
			}
			clazz = clazz.getSuperclass();
		}

		throw new NoSuchMethodException("Could not find method " + name);
	}

	private Field findField(Class<?> clazz, String name)
			throws NoSuchFieldException {
		while (clazz != Object.class) {
			try {
				return clazz.getDeclaredField(name);
			} catch (SecurityException e) {
			} catch (NoSuchFieldException e) {
			}
			clazz = clazz.getSuperclass();
		}

		throw new NoSuchFieldException("Could not find field " + name);
	}

	private ArtifactDefinition getArtifactDefinition(PreparePackageMojo mojo,
			String name) throws Exception {
		Field field = findField(mojo.getClass(), name);
		field.setAccessible(true);
		ArtifactDefinition def = (ArtifactDefinition) field.get(mojo);
		field.setAccessible(false);
		return def;
	}

}
