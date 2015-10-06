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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.Callable;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeExistsException;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.core.TransientRepository;
import org.apache.sling.ide.filter.Filter;
import org.apache.sling.ide.filter.FilterResult;
import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.transport.CommandContext;
import org.apache.sling.ide.transport.ResourceProxy;
import org.junit.Ignore;
import org.junit.Test;

public class AddOrUpdateNodeCommandTest {
    
    private static final CommandContext DEFAULT_CONTEXT = new CommandContext(new Filter() {
        @Override
        public FilterResult filter(String repositoryPath) {
            return FilterResult.ALLOW;
        }
    });

    private static final String PROP_NAME = "jcr:title";

    private Logger logger = new Slf4jLogger();

    @Test
    public void setProperty() throws Exception {

        doPropertyChangeTest(null, "Title");
    }

    private void doPropertyChangeTest(final Object initialPropertyValues, final Object newPropertyValues)
            throws Exception {

        doWithTransientRepository(new CallableWithSession() {
            @Override
            public Void call() throws Exception {
                Node contentNode = session().getRootNode().addNode("content");
                if (initialPropertyValues instanceof String) {
                    contentNode.setProperty(PROP_NAME, (String) initialPropertyValues);
                } else if (initialPropertyValues instanceof String[]) {
                    contentNode.setProperty(PROP_NAME, (String[]) initialPropertyValues);
                }

                session().save();

                ResourceProxy resource = newResource("/content", NodeType.NT_UNSTRUCTURED);
                if (newPropertyValues != null) {
                    resource.addProperty(PROP_NAME, newPropertyValues);
                }

                AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repo(), credentials(), DEFAULT_CONTEXT, null, resource, logger);
                cmd.execute().get();

                session().refresh(false);

                if (newPropertyValues == null) {
                    assertThat(session().getNode("/content").hasProperty(PROP_NAME), equalTo(false));
                    return null;
                }

                Property newProp = session().getNode("/content").getProperty(PROP_NAME);
                if (newPropertyValues instanceof String) {
                    assertThat("property.isMultiple", newProp.isMultiple(), equalTo(Boolean.FALSE));
                    assertThat(newProp.getString(), equalTo((String) newPropertyValues));

                } else {

                    String[] expectedValues = (String[]) newPropertyValues;
                    assertThat("property.isMultiple", newProp.isMultiple(), equalTo(Boolean.TRUE));

                    Value[] values = session().getNode("/content").getProperty(PROP_NAME).getValues();

                    assertThat(values.length, equalTo(expectedValues.length));
                    for (int i = 0; i < values.length; i++) {
                        assertThat(values[i].getString(), equalTo(expectedValues[i]));
                    }

                }

                return null;
            }
        });

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

        doWithTransientRepository(new CallableWithSession() {
            @Override
            public Void call() throws Exception {
                session().getRootNode().addNode("content", "nt:folder");

                session().save();

                ResourceProxy resource = newResource("/content", "sling:Folder");
                resource.getProperties().put("newProperty", "some/value");

                AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repo(), credentials(), DEFAULT_CONTEXT, null, resource, logger);
                cmd.execute().get();

                session().refresh(false);

                Node content = session().getRootNode().getNode("content");
                assertThat(content.getPrimaryNodeType().getName(), equalTo("sling:Folder"));

                return null;
            }
        });
    }

    @Test
    public void changeSlingFolderToNtFolderWithExistingProperty() throws Exception {
        doWithTransientRepository(new CallableWithSession() {
            @Override
            public Void call() throws Exception {
                Node content = session().getRootNode().addNode("content", "sling:Folder");
                content.setProperty("newProperty", "some/value");

                session().save();

                ResourceProxy resource = newResource("/content", "nt:folder");

                AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repo(), credentials(), DEFAULT_CONTEXT, null, resource, logger);
                cmd.execute().get();

                session().refresh(false);

                content = session().getRootNode().getNode("content");
                assertThat(content.getPrimaryNodeType().getName(), equalTo("nt:folder"));

                return null;
            }
        });
    }

    @Test
    @Ignore("SLING-4036")
    public void updateNtUnstructuredToNodeWithRequiredProperty() throws Exception {

        doWithTransientRepository(new CallableWithSession() {
            @Override
            public Void call() throws Exception {
                Node content = session().getRootNode().addNode("content", "nt:unstructured");

                session().save();

                ResourceProxy resource = newResource("/content", "custom");
                resource.getProperties().put("attribute", "some value");

                AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repo(), credentials(), DEFAULT_CONTEXT, null, resource, logger);
                cmd.execute().get();

                session().refresh(false);

                content = session().getRootNode().getNode("content");
                assertThat(content.getPrimaryNodeType().getName(), equalTo("custom"));

                return null;
            }
        });
    }
    
    @Test
    public void nodeNotPresentButOutsideOfFilterIsNotRemoved() throws Exception {

        final CommandContext context = new CommandContext(new Filter() {
            
            @Override
            public FilterResult filter(String repositoryPath) {
                if ( repositoryPath.equals("/content/not-included-child")) {
                    return FilterResult.DENY;
                }
                
                return FilterResult.ALLOW;
            }
        });
        
        doWithTransientRepository(new CallableWithSession() {
            @Override
            public Void call() throws Exception {
                Node content = session().getRootNode().addNode("content", "nt:unstructured");
                content.addNode("included-child");
                content.addNode("not-included-child");
                
                session().save();

                ResourceProxy resource = newResource("/content", "nt:unstructured");
                resource.addChild(newResource("/content/included-child", "nt:unstructured"));

                AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repo(), credentials(), context, null, resource, logger);
                cmd.execute().get();

                session().refresh(false);

                content = session().getRootNode().getNode("content");
                content.getNode("included-child");
                content.getNode("not-included-child");
                return null;
            }
        });
        
    }

    private ResourceProxy newResource(String path, String primaryType) {

        ResourceProxy resource = new ResourceProxy(path);
        resource.addProperty("jcr:primaryType", primaryType);
        return resource;
    }

    @Test
    public void createIfRequiredFlagSkipsExistingResources() throws Exception {

        doWithTransientRepository(new CallableWithSession() {
            @Override
            public Void call() throws Exception {
                Node content = session().getRootNode().addNode("content", "nt:folder");

                session().save();

                ResourceProxy resource = newResource("/content", "nt:unstructured");

                AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repo(), credentials(), DEFAULT_CONTEXT, null, resource, logger,
                        CREATE_ONLY_WHEN_MISSING);
                cmd.execute().get();

                session().refresh(false);

                content = session().getRootNode().getNode("content");
                assertThat(content.getPrimaryNodeType().getName(), equalTo("nt:folder"));

                return null;
            }
        });
    }

    @Test
    public void createIfRequiredFlagCreatesNeededResources() throws Exception {

        doWithTransientRepository(new CallableWithSession() {
            @Override
            public Void call() throws Exception {
                ResourceProxy resource = newResource("/content", "nt:unstructured");

                AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repo(), credentials(), DEFAULT_CONTEXT, null, resource, logger,
                        CREATE_ONLY_WHEN_MISSING);
                cmd.execute().get();

                session().refresh(false);

                Node content = session().getRootNode().getNode("content");
                assertThat(content.getPrimaryNodeType().getName(), equalTo("nt:unstructured"));

                return null;
            }
        });
    }

    @Test
    public void createIfRequiredFlagCreatesNeededResourcesEvenWhenPrimaryTypeIsMissing() throws Exception {

        doWithTransientRepository(new CallableWithSession() {
            @Override
            public Void call() throws Exception {
                ResourceProxy resource = new ResourceProxy("/content");

                AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repo(), credentials(), DEFAULT_CONTEXT, null, resource, logger,
                        CREATE_ONLY_WHEN_MISSING);
                cmd.execute().get();

                session().refresh(false);

                Node content = session().getRootNode().getNode("content");
                assertThat(content.getPrimaryNodeType().getName(), equalTo("nt:unstructured"));

                return null;
            }
        });
    }

    @Test
    public void autoCreatedPropertiesAreNotRemoved() throws Exception {

        doWithTransientRepository(new CallableWithSession() {
            @Override
            public Void call() throws Exception {
                Node content = session().getRootNode().addNode("content", "nt:folder");

                session().save();

                ResourceProxy resource = newResource("/content", "nt:folder");
                resource.addProperty("jcr:mixinTypes", "mix:lastModified");

                AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repo(), credentials(), DEFAULT_CONTEXT, null, resource, logger);
                cmd.execute().get();
                cmd.execute().get(); // second time since mixins are processed after properties so we need two
                                     // executions to
                                     // expose the problem

                session().refresh(false);

                content = session().getRootNode().getNode("content");
                assertThat("jcr:lastModified property not present", content.hasProperty("jcr:lastModified"),
                        equalTo(true));
                assertThat("jcr:lastModifiedBy property not present", content.hasProperty("jcr:lastModifiedBy"),
                        equalTo(true));

                return null;
            }
        });
    }

    @Test
    public void autoCreatedPropertiesAreUpdatedIfPresent() throws Exception {

        doWithTransientRepository(new CallableWithSession() {
            @Override
            public Void call() throws Exception {
                Node content = session().getRootNode().addNode("content", "nt:folder");

                session().save();

                ResourceProxy resource = newResource("/content", "nt:folder");
                resource.addProperty("jcr:mixinTypes", "mix:lastModified");
                resource.addProperty("jcr:lastModifiedBy", "admin2");

                AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repo(), credentials(), DEFAULT_CONTEXT, null, resource, logger);
                cmd.execute().get();
                cmd.execute().get(); // second time since mixins are processed after properties so we need two
                                     // executions to
                                     // expose the problem

                session().refresh(false);

                content = session().getRootNode().getNode("content");
                assertThat("jcr:lastModifiedBy property not modified", content.getProperty("jcr:lastModifiedBy")
                        .getString(), equalTo("admin2"));

                return null;
            }
        });
    }

    private void doWithTransientRepository(CallableWithSession callable) throws Exception {

        File out = new File(new File("target"), "jackrabbit");
        TransientRepository repo = new TransientRepository(new File(out, "repository.xml"), new File(out, "repository"));
        SimpleCredentials credentials = new SimpleCredentials("admin", "admin".toCharArray());
        Session session = repo.login(credentials);

        importNodeTypeDefinitions(session, "test-definitions.cnd");
        importNodeTypeDefinitions(session, "folder.cnd");

        try {
            callable.setCredentials(credentials);
            callable.setSession(session);
            callable.call();
        } finally {
            if (session.itemExists("/content"))
                session.removeItem("/content");
            session.save();
            session.logout();
        }

    }

    private void importNodeTypeDefinitions(Session session, String cndFile) throws InvalidNodeTypeDefinitionException,
            NodeTypeExistsException, UnsupportedRepositoryOperationException, ParseException, RepositoryException,
            IOException {
        try ( InputStream cndInput = getClass().getResourceAsStream(cndFile) ) {
            if (cndInput == null) {
                throw new IllegalArgumentException("Unable to read classpath resource " + cndFile);
            }
            CndImporter.registerNodeTypes(new InputStreamReader(cndInput), session);
        }
    }

    private static abstract class CallableWithSession implements Callable<Void> {

        private Session session;
        private Credentials credentials;

        public void setSession(Session session) {

            this.session = session;
        }

        public void setCredentials(Credentials credentials) {

            this.credentials = credentials;
        }

        protected Session session() {

            if (session == null)
                throw new IllegalStateException("session is null");

            return session;
        }

        protected Credentials credentials() {

            if (credentials == null)
                throw new IllegalStateException("credentials is null");

            return credentials;
        }

        protected Repository repo() {

            return session().getRepository();
        }
    }
}
