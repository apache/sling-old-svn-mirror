/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.performance.tests;

import java.math.BigInteger;
import java.security.SecureRandom;

import javax.jcr.Node;
import javax.servlet.http.HttpServletRequest;
import junit.framework.Assert;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.testing.jcr.RepositoryUtil;
import org.apache.sling.performance.AbstractRepositoryTest;
import org.apache.sling.performance.TestHelper;
import org.apache.sling.performance.ResourceResolverTestRequest;
import org.apache.sling.performance.annotation.PerformanceTest;
import org.junit.After;
import org.junit.Before;

public class ResolveNonExistingWithManyAliasTest extends AbstractRepositoryTest {
    
    private static final String PN_SLING_ALIAS = "sling:alias";
    
    private final TestHelper helper;

    private Node mapRoot;

    private ResourceResolver resResolver;
    
    private Node rootNode;
    
    private String rootPath;

    private final int nodeCount;
    
    public ResolveNonExistingWithManyAliasTest(String testInstanceName, TestHelper helper, int nodeCount) {
        super(testInstanceName);
        this.helper = helper;
        this.nodeCount = nodeCount;
    }

    @After
    protected void afterSuite() throws Exception {
        if (helper != null) {
            helper.dispose();
        }

        if (rootNode != null) {
            rootNode.remove();
        }
        if (mapRoot != null) {
            mapRoot.remove();
        }
        session.save();
    }

    @Before
    protected void beforeSuite() throws Exception {
        RepositoryUtil.registerNodeType(getSession(),
                this.getClass().getResourceAsStream("/SLING-INF/nodetypes/folder.cnd"));
        RepositoryUtil.registerNodeType(getSession(),
                this.getClass().getResourceAsStream("/SLING-INF/nodetypes/resource.cnd"));
        RepositoryUtil.registerNodeType(getSession(),
                this.getClass().getResourceAsStream("/SLING-INF/nodetypes/vanitypath.cnd"));
        RepositoryUtil.registerNodeType(getSession(),
                this.getClass().getResourceAsStream("/SLING-INF/nodetypes/mapping.cnd"));

        // test data
        rootPath = "/test" + System.currentTimeMillis();
        rootNode = getSession().getRootNode().addNode(rootPath.substring(1), JcrConstants.NT_UNSTRUCTURED);

        // test mappings
        mapRoot = getSession().getRootNode().addNode("etc", "nt:folder");
        Node map = mapRoot.addNode("map", "sling:Mapping");
        Node http = map.addNode("http", "sling:Mapping");
        http.addNode("localhost.80", "sling:Mapping");
        Node https = map.addNode("https", "sling:Mapping");
        https.addNode("localhost.443", "sling:Mapping");

        // define a vanity path for the rootPath
        SecureRandom random = new SecureRandom();
        // creating <nodeCount> nodes
        for (int j = 0; j < nodeCount; j++) {
            Node content = rootNode.addNode("a" + j, JcrConstants.NT_UNSTRUCTURED);
            String alias = new BigInteger(130, random).toString(32);
            content.setProperty(PN_SLING_ALIAS, alias);

            if (j % 10 == 0) {
                session.save();
            }
        }

        session.save();
        
        helper.init(rootPath, session, getRepository());

        resResolver = helper.getResourceResolver();

    }

    @PerformanceTest
    public void runTest() throws Exception {
        String path = ResourceUtil.normalize(ResourceUtil.getParent(rootPath) + "/" + "testNonExistingAlias"
                + ".print.html");
        HttpServletRequest request = new ResourceResolverTestRequest(path);
        Resource res = resResolver.resolve(request, path);
        Assert.assertNotNull(res);
    }
}
