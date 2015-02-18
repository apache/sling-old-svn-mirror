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
package org.apache.sling.ide.impl.vlt;

import static org.apache.sling.ide.transport.Repository.CommandExecutionFlag.CREATE_ONLY_WHEN_MISSING;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import org.apache.commons.io.IOUtils;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.transport.ResourceProxy;
import org.junit.Ignore;
import org.junit.Test;

public class AddOrUpdateNodeCommandTest {

    private static final String PROP_NAME = "jcr:title";

    private Logger logger = new Slf4jLogger();

    @Test
    public void setProperty() throws Exception {

        doPropertyChangeTest(null, "Title");
    }

    private void doPropertyChangeTest(Object initialPropertyValues, Object newPropertyValues)
            throws RepositoryException, org.apache.sling.ide.transport.RepositoryException {

        File out = new File(new File("target"), "jackrabbit");
        TransientRepository repo = new TransientRepository(new File(out, "repository.xml"), new File(out, "repository"));
        SimpleCredentials credentials = new SimpleCredentials("admin", "admin".toCharArray());
        Session session = repo.login(credentials);
        try {
            Node contentNode = session.getRootNode().addNode("content");
            if (initialPropertyValues instanceof String) {
                contentNode.setProperty(PROP_NAME, (String) initialPropertyValues);
            } else if (initialPropertyValues instanceof String[]) {
                contentNode.setProperty(PROP_NAME, (String[]) initialPropertyValues);
            }

            session.save();

            ResourceProxy resource = newResource("/content", NodeType.NT_UNSTRUCTURED);
            if (newPropertyValues != null) {
                resource.addProperty(PROP_NAME, newPropertyValues);
            }

            AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repo, credentials, null, resource, logger);
            cmd.execute().get();

            session.refresh(false);

            if (newPropertyValues == null) {
                assertThat(session.getNode("/content").hasProperty(PROP_NAME), equalTo(false));
                return;
            }

            Property newProp = session.getNode("/content").getProperty(PROP_NAME);
            if (newPropertyValues instanceof String) {
                assertThat("property.isMultiple", newProp.isMultiple(), equalTo(Boolean.FALSE));
                assertThat(newProp.getString(), equalTo((String) newPropertyValues));

            } else {

                String[] expectedValues = (String[]) newPropertyValues;
                assertThat("property.isMultiple", newProp.isMultiple(), equalTo(Boolean.TRUE));

                Value[] values = session.getNode("/content").getProperty(PROP_NAME).getValues();

                assertThat(values.length, equalTo(expectedValues.length));
                for (int i = 0; i < values.length; i++) {
                    assertThat(values[i].getString(), equalTo(expectedValues[i]));
                }

            }

        } finally {
            session.removeItem("/content");
            session.save();
            session.logout();
        }
    }

    @Test
    public void removeProperty() throws Exception {

        doPropertyChangeTest("Title", null);
    }

    @Test
    public void singlePropertyToMultiValued() throws Exception {

        doPropertyChangeTest("Title", new String[] { "Title", "Title 2" });
    }

    @Test
    public void multiValuesPropertyToSingle() throws Exception {

        doPropertyChangeTest(new String[] { "Title", "Title 2" }, "Title");
    }

    @Test
    public void changeNtFolderToSlingFolderWithAddedProperty() throws Exception {

        File out = new File(new File("target"), "jackrabbit");
        TransientRepository repo = new TransientRepository(new File(out, "repository.xml"), new File(out, "repository"));
        SimpleCredentials credentials = new SimpleCredentials("admin", "admin".toCharArray());
        Session session = repo.login(credentials);

        InputStream cndInput = getClass().getResourceAsStream("folder.cnd");
        CndImporter.registerNodeTypes(new InputStreamReader(cndInput), session);

        try {
            session.getRootNode().addNode("content", "nt:folder");

            session.save();

            ResourceProxy resource = newResource("/content", "sling:Folder");
            resource.getProperties().put("newProperty", "some/value");

            AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repo, credentials, null, resource, logger);
            cmd.execute().get();

            session.refresh(false);

            Node content = session.getRootNode().getNode("content");
            assertThat(content.getPrimaryNodeType().getName(), equalTo("sling:Folder"));

        } finally {
            session.removeItem("/content");
            session.save();
            session.logout();

            IOUtils.closeQuietly(cndInput);
        }
    }

    @Test
    public void changeSlingFolderToNtFolderWithExistingProperty() throws Exception {

        File out = new File(new File("target"), "jackrabbit");
        TransientRepository repo = new TransientRepository(new File(out, "repository.xml"), new File(out, "repository"));
        SimpleCredentials credentials = new SimpleCredentials("admin", "admin".toCharArray());
        Session session = repo.login(credentials);

        InputStream cndInput = getClass().getResourceAsStream("folder.cnd");
        CndImporter.registerNodeTypes(new InputStreamReader(cndInput), session);

        try {
            Node content = session.getRootNode().addNode("content", "sling:Folder");
            content.setProperty("newProperty", "some/value");

            session.save();

            ResourceProxy resource = newResource("/content", "nt:folder");

            AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repo, credentials, null, resource, logger);
            cmd.execute().get();

            session.refresh(false);

            content = session.getRootNode().getNode("content");
            assertThat(content.getPrimaryNodeType().getName(), equalTo("nt:folder"));

        } finally {
            session.removeItem("/content");
            session.save();
            session.logout();

            IOUtils.closeQuietly(cndInput);
        }
    }

    @Test
    @Ignore("SLING-4036")
    public void updateNtUnstructuredToNodeWithRequiredProperty() throws Exception {

        File out = new File(new File("target"), "jackrabbit");
        TransientRepository repo = new TransientRepository(new File(out, "repository.xml"), new File(out, "repository"));
        SimpleCredentials credentials = new SimpleCredentials("admin", "admin".toCharArray());
        Session session = repo.login(credentials);

        InputStream cndInput = getClass().getResourceAsStream("mandatory.cnd"); // TODO - should be test-definitions.cnd
        CndImporter.registerNodeTypes(new InputStreamReader(cndInput), session);

        try {
            Node content = session.getRootNode().addNode("content", "nt:unstructured");

            session.save();

            ResourceProxy resource = newResource("/content", "custom");
            resource.getProperties().put("attribute", "some value");

            AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repo, credentials, null, resource, logger);
            cmd.execute().get();

            session.refresh(false);

            content = session.getRootNode().getNode("content");
            assertThat(content.getPrimaryNodeType().getName(), equalTo("custom"));

        } finally {
            session.removeItem("/content");
            session.save();
            session.logout();

            IOUtils.closeQuietly(cndInput);
        }
    }

    private ResourceProxy newResource(String path, String primaryType) {

        ResourceProxy resource = new ResourceProxy(path);
        resource.addProperty("jcr:primaryType", primaryType);
        return resource;
    }

    @Test
    public void createIfRequiredFlagSkipsExistingResources() throws Exception {

        File out = new File(new File("target"), "jackrabbit");
        TransientRepository repo = new TransientRepository(new File(out, "repository.xml"), new File(out, "repository"));
        SimpleCredentials credentials = new SimpleCredentials("admin", "admin".toCharArray());
        Session session = repo.login(credentials);

        try {
            Node content = session.getRootNode().addNode("content", "nt:folder");

            session.save();

            ResourceProxy resource = newResource("/content", "nt:unstructured");

            AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repo, credentials, null, resource, logger,
                    CREATE_ONLY_WHEN_MISSING);
            cmd.execute().get();

            session.refresh(false);

            content = session.getRootNode().getNode("content");
            assertThat(content.getPrimaryNodeType().getName(), equalTo("nt:folder"));

        } finally {
            session.removeItem("/content");
            session.save();
            session.logout();
        }
    }

    @Test
    public void createIfRequiredFlagCreatesNeededResources() throws Exception {

        File out = new File(new File("target"), "jackrabbit");
        TransientRepository repo = new TransientRepository(new File(out, "repository.xml"), new File(out, "repository"));
        SimpleCredentials credentials = new SimpleCredentials("admin", "admin".toCharArray());
        Session session = repo.login(credentials);

        try {
            ResourceProxy resource = newResource("/content", "nt:unstructured");

            AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repo, credentials, null, resource, logger,
                    CREATE_ONLY_WHEN_MISSING);
            cmd.execute().get();

            session.refresh(false);

            Node content = session.getRootNode().getNode("content");
            assertThat(content.getPrimaryNodeType().getName(), equalTo("nt:unstructured"));

        } finally {
            session.removeItem("/content");
            session.save();
            session.logout();
        }
    }

    @Test
    public void createIfRequiredFlagCreatesNeededResourcesEvenWhenPrimaryTypeIsMissing() throws Exception {

        File out = new File(new File("target"), "jackrabbit");
        TransientRepository repo = new TransientRepository(new File(out, "repository.xml"), new File(out, "repository"));
        SimpleCredentials credentials = new SimpleCredentials("admin", "admin".toCharArray());
        Session session = repo.login(credentials);

        try {
            ResourceProxy resource = new ResourceProxy("/content");

            AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repo, credentials, null, resource, logger,
                    CREATE_ONLY_WHEN_MISSING);
            cmd.execute().get();

            session.refresh(false);

            Node content = session.getRootNode().getNode("content");
            assertThat(content.getPrimaryNodeType().getName(), equalTo("nt:unstructured"));

        } finally {
            if (session.itemExists("/content"))
                session.removeItem("/content");
            session.save();
            session.logout();
        }
    }
}
