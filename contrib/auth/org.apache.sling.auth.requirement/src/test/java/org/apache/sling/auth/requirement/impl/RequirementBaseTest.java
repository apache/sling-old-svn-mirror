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
package org.apache.sling.auth.requirement.impl;

import java.util.Arrays;
import javax.annotation.Nonnull;
import javax.jcr.NamespaceRegistry;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;

import org.apache.jackrabbit.oak.AbstractSecurityTest;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.Root;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.commit.JcrConflictHandler;
import org.apache.jackrabbit.oak.plugins.name.ReadWriteNamespaceRegistry;
import org.apache.jackrabbit.oak.plugins.nodetype.NodeTypeConstants;
import org.apache.jackrabbit.oak.plugins.nodetype.TypeEditorProvider;
import org.apache.jackrabbit.oak.plugins.nodetype.write.InitialContent;
import org.apache.jackrabbit.oak.plugins.nodetype.write.ReadWriteNodeTypeManager;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.junit.Rule;

import static com.google.common.base.Preconditions.checkState;

public abstract class RequirementBaseTest extends AbstractSecurityTest {

    @Rule
    public final OsgiContext context = new OsgiContext();

    private Repository repo;
    private Session session;

    @Override
    public void before() throws Exception {
        if (initJcrRepo()) {
            super.before();
            Oak oak = (new Oak()).
                    with(new InitialContent()).
                    with(JcrConflictHandler.createJcrConflictHandler()).
                    with(new TypeEditorProvider()).
                    with(getSecurityProvider());

            withEditors(oak);

            Jcr jcr = new Jcr(oak);
            repo = jcr.createRepository();
            session = repo.login(getAdminCredentials());
        } else {
            super.before();
        }

        registerNamespaces(getNamespaceRegistry());
        registerRequirementTypes(getNodeTypeManager());
    }

    @Override
    public void after() throws Exception {
        if (initJcrRepo()) {
            if (session != null && session.isLive()) {
                session.logout();
            }
            repo = null;
        } else {
            super.after();
        }
    }

    abstract boolean initJcrRepo();

    private NamespaceRegistry getNamespaceRegistry() throws Exception {
        if (initJcrRepo()) {
            return session.getWorkspace().getNamespaceRegistry();
        } else {
            return new ReadWriteNamespaceRegistry(root) {
                @Override
                protected Root getWriteRoot() {
                    return root;
                }
            };
        }
    }

    private NodeTypeManager getNodeTypeManager() throws Exception {
        if (initJcrRepo()) {
            return session.getWorkspace().getNodeTypeManager();
        } else {
            return new ReadWriteNodeTypeManager() {
                @Override
                protected Tree getTypes() {
                    return root.getTree(NodeTypeConstants.NODE_TYPES_PATH);
                }

                @Nonnull
                @Override
                protected Root getWriteRoot() {
                    return root;
                }
            };
        }
    }

    Session getSession() {
        checkState(session != null);
        return session;
    }

    private void registerNamespaces(NamespaceRegistry namespaceRegistry) throws RepositoryException {
        if (!Arrays.asList(namespaceRegistry.getPrefixes()).contains("sling")) {
            namespaceRegistry.registerNamespace("sling", "http://sling.apache.org/jcr/sling/1.0");
        }
    }

    private void registerRequirementTypes(NodeTypeManager ntMgr) throws Exception {
        if (!ntMgr.hasNodeType(Constants.MIX_SLING_AUTHENTICATION_REQUIRED)) {
            // node type definition for sling:AuthenticationRequired mixin
            NodeTypeTemplate tmpl = ntMgr.createNodeTypeTemplate();
            tmpl.setName(Constants.MIX_SLING_AUTHENTICATION_REQUIRED);
            tmpl.setMixin(true);

            PropertyDefinitionTemplate propTmpl = ntMgr.createPropertyDefinitionTemplate();
            propTmpl.setName(Constants.NAME_SLING_LOGIN_PATH);
            propTmpl.setRequiredType(PropertyType.STRING);
            tmpl.getPropertyDefinitionTemplates().add(propTmpl);

            // custom node type definition to have mixin as super type
            NodeTypeTemplate tmpl2 = ntMgr.createNodeTypeTemplate();
            tmpl2.setName("myNodeType");
            tmpl2.setDeclaredSuperTypeNames(new String[]{tmpl.getName(), NodeTypeConstants.NT_OAK_UNSTRUCTURED});

            ntMgr.registerNodeTypes(new NodeTypeDefinition[]{tmpl, tmpl2}, true);
        }
    }

}