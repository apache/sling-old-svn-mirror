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
package org.apache.sling.provisioning.model;

import static org.apache.sling.provisioning.model.ModelConstants.DEFAULT_RUN_MODE;
import static org.apache.sling.provisioning.model.ModelConstants.DEFAULT_START_LEVEL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.sling.provisioning.model.io.ModelReader;

/** Test utilities */
public class U {

    public static final String[] TEST_MODEL_FILENAMES =
            new String[] {"boot.txt", "example.txt", "main.txt", "oak.txt"};

    public static void assertArtifact(ArtifactGroup g, String mvnUrl) {
        final Artifact a = Artifact.fromMvnUrl(mvnUrl);
        if(!g.items.contains(a)) {
            fail("Expecting ArtifactGroup to contain '" + mvnUrl + "': " + g);
        }
    }

    /** Read our test model by merging our TEST_MODEL_FILENAMES */
    public static Model readCompleteTestModel() throws Exception {
        return readCompleteTestModel(TEST_MODEL_FILENAMES);
    }

    /** Read the complete model from that names */
    public static Model readCompleteTestModel(final String[] names) throws Exception {
        Model result = null;

        for(final String name : names) {
            final Reader reader = new InputStreamReader(U.class.getResourceAsStream("/" + name), "UTF-8");
            try {
                final Model current = ModelReader.read(reader, name);
                final Map<Traceable, String> errors = ModelUtility.validate(current);
                if (errors != null ) {
                    throw new Exception("Invalid model at " + name + " : " + errors);
                }
                if ( result == null ) {
                    result = current;
                } else {
                    ModelUtility.merge(result, current);
                }
            } finally {
                reader.close();
            }
        }

        final Map<Traceable, String> errors = ModelUtility.validate(result);
        if (errors != null ) {
            throw new Exception("Invalid merged model : " + errors);
        }
        return result;
    }

    public static ArtifactGroup getGroup(Model m, String feature, String runMode, int startLevel) {
        final Feature f = m.getFeature(feature);
        assertNotNull(f);
        final RunMode rm = f.getRunMode(runMode);
        assertNotNull(rm);
        final ArtifactGroup g = rm.getArtifactGroup(startLevel);
        assertNotNull(g);
        return g;
    }

    /** Verify that m matches what we expect after
     *  reading and merging our test files.
     */
    public static void verifyTestModel(Model m, boolean variablesAlreadyResolved) {
        final String [] f = { ":launchpad","example","main","oak" };
        for(String name : f) {
            assertNotNull("Expecting feature to be present:" + name, m.getFeature(name));
        }

        {
            final ArtifactGroup g = getGroup(m, "example", DEFAULT_RUN_MODE, DEFAULT_START_LEVEL);
            U.assertArtifact(g, "mvn:commons-collections/commons-collections/3.2.1/jar");
            U.assertArtifact(g, "mvn:org.example/jar-is-default/1.2/jar");
        }

        {
            final ArtifactGroup g = getGroup(m, "example", "jackrabbit", 15);
            if(variablesAlreadyResolved) {
                U.assertArtifact(g, "mvn:org.apache.sling/org.apache.sling.jcr.jackrabbit.server/2.1.3-SNAPSHOT/jar");
            } else {
                U.assertArtifact(g, "mvn:org.apache.sling/org.apache.sling.jcr.jackrabbit.server/${jackrabbit.version}/jar");
            }
        }

        {
            final ArtifactGroup g = getGroup(m, ":boot", DEFAULT_RUN_MODE, DEFAULT_START_LEVEL);
            if(variablesAlreadyResolved) {
                U.assertArtifact(g, "mvn:org.apache.sling/org.apache.sling.fragment.ws/1.42-from-boot/jar");
            } else {
                U.assertArtifact(g, "mvn:org.apache.sling/org.apache.sling.fragment.ws/${ws.version}/jar");
            }
        }
        final Feature exampleFeature = m.getFeature("example");
        final RunMode defaultExampleRM = exampleFeature.getRunMode();
        final List<Configuration> configs = assertConfigurationsInRunMode(defaultExampleRM, 3);
        final Configuration cfg = assertConfiguration(configs, "org.apache.sling.another.config");
    }

    public static Configuration assertConfiguration(final List<Configuration> configs, final String pid) {
        for(final Configuration c : configs) {
            if ( c.getPid().equals(pid) && c.getFactoryPid() == null ) {
                return c;
            }
        }
        fail("Configuration with PID " + pid + " not found in " + configs);
        return null;
    }

    public static void assertArtifact(final Artifact artifact,
            final String groupId, final String artifactId, final String version, final String type, final String classifier) {
        assertNotNull(artifact);
        assertEquals(groupId, artifact.getGroupId());
        assertEquals(artifactId, artifact.getArtifactId());
        assertEquals(version, artifact.getVersion());
        assertEquals(type, artifact.getType());
        assertEquals(classifier, artifact.getClassifier());
    }

    public static List<Artifact> assertArtifactsInGroup(final ArtifactGroup group, final int size) {
        final List<Artifact> result = new ArrayList<Artifact>();
        if ( size == 0 ) {
            assertTrue("Group should be empty", group.isEmpty());
        } else {
            assertFalse("Group should not be empty", group.isEmpty());
        }
        for(final Artifact a : group) {
            result.add(a);
        }
        assertEquals("Unexpected size of group: ", size, result.size());
        return result;
    }

    public static List<Configuration> assertConfigurationsInRunMode(final RunMode runmode, final int size) {
        final List<Configuration> result = new ArrayList<Configuration>();
        if ( size == 0 ) {
            assertTrue("Configurations should be empty", runmode.getConfigurations().isEmpty());
        } else {
            assertFalse("Configurations should not be empty", runmode.getConfigurations().isEmpty());
        }
        for(final Configuration a : runmode.getConfigurations()) {
            result.add(a);
        }
        assertEquals("Unexpected size of configurations: ", size, result.size());
        return result;
    }
}
