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
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableList;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.oak.Oak;
import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.api.Type;
import org.apache.jackrabbit.oak.plugins.memory.MemoryNodeStore;
import org.apache.jackrabbit.oak.plugins.nodetype.NodeTypeConstants;
import org.apache.jackrabbit.oak.spi.commit.Observer;
import org.apache.jackrabbit.oak.spi.state.NodeBuilder;
import org.apache.jackrabbit.oak.spi.state.NodeState;
import org.apache.jackrabbit.oak.spi.state.NodeStateDiff;
import org.apache.jackrabbit.oak.spi.whiteboard.Whiteboard;
import org.apache.jackrabbit.oak.util.NodeUtil;
import org.apache.jackrabbit.oak.util.TreeUtil;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RequirementObserverTest extends RequirementBaseTest {

    private TestHandler testHandler = new TestHandler();
    private RequirementObserver requirementObserver = new RequirementObserver(testHandler);

    private Tree testTree;
    private String testPath;

    @Override
    public void before() throws Exception {
        super.before();

        testTree = new NodeUtil(root.getTree("/")).addChild("test", NodeTypeConstants.NT_OAK_UNSTRUCTURED).getTree();
        testPath = testTree.getPath();
        root.commit();
    }

    @Override
    public void after() throws Exception {
        try {
            root.refresh();
            Tree t = root.getTree(testPath);
            if (t.exists()) {
                t.remove();
                root.commit();
            }
        } finally {
            super.after();
        }
    }

    @Override
    boolean initJcrRepo() {
        return false;
    }

    @Override
    protected Oak withEditors(Oak oak) {

        Whiteboard whiteboard = oak.getWhiteboard();
        whiteboard.register(Observer.class, requirementObserver, Collections.emptyMap());

        return oak;
    }

    private void setupAuthRequirement(@Nonnull Tree tree, @CheckForNull String loginPath) throws Exception {
        TreeUtil.addMixin(tree, Constants.MIX_SLING_AUTHENTICATION_REQUIRED, root.getTree(NodeTypeConstants.NODE_TYPES_PATH), null);
        if (loginPath != null) {
            tree.setProperty(Constants.NAME_SLING_LOGIN_PATH, loginPath);
        }
        root.commit();
    }

    private void setupLoginPath(@Nonnull Tree tree, @Nonnull String loginPath) throws Exception {
        tree.setProperty(Constants.NAME_SLING_LOGIN_PATH, loginPath);
        root.commit();
    }

    private void assertExecution(@Nonnull String... args) {
        assertEquals(1, testHandler.calls.size());
        assertArrayEquals(args, testHandler.calls.iterator().next());
    }

    private void assertExecution(@Nonnull Set<String[]> expected) {
        assertEquals(expected.size(), testHandler.calls.size());

        for (String[] expectedArgs : expected) {
            boolean found = false;
            for (String[] args : testHandler.calls) {
                if (Arrays.equals(expectedArgs, args)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                fail("No expected entry found (was: " + Arrays.toString(expectedArgs));
            }
        }
    }

    @Test
    public void addMixin() throws Exception {
        setupAuthRequirement(testTree, null);

        assertExecution("requirementAdded", testPath, null);
    }

    @Test
    public void addMixinWithLoginPath() throws Exception {
        setupAuthRequirement(testTree, "/login/path");

        assertExecution("requirementAdded", testPath, "/login/path");
    }

    @Test
    public void addNodeAndMixin() throws Exception {
        Tree newTree = new NodeUtil(testTree).addChild("child", NodeTypeConstants.NT_OAK_UNSTRUCTURED).getTree();
        setupAuthRequirement(newTree, null);

        assertExecution("requirementAdded", newTree.getPath(), null);
    }

    @Test
    public void addNodeAndMixinWithLoginPath() throws Exception {
        Tree newTree = new NodeUtil(testTree).addChild("child", NodeTypeConstants.NT_OAK_UNSTRUCTURED).getTree();
        setupAuthRequirement(newTree, "/login/path");

        assertExecution("requirementAdded", newTree.getPath(), "/login/path");
    }

    @Test
    public void addNodeWithLoginPath() throws Exception {
        Tree newTree = new NodeUtil(testTree).addChild("child", NodeTypeConstants.NT_OAK_UNSTRUCTURED).getTree();
        setupLoginPath(newTree, "/login/path");

        assertTrue(testHandler.calls.isEmpty());
    }

    @Test
    public void addNodeWithTypeInheritance() throws Exception {
        Tree newTree = new NodeUtil(testTree).addChild("child", "myNodeType").getTree();
        root.commit();

        assertExecution("requirementAdded", newTree.getPath(), null);
    }

    @Test
    public void removeMixinProperty() throws Exception {
        setupAuthRequirement(testTree, null);
        testHandler.calls.clear();

        testTree.removeProperty(NodeTypeConstants.JCR_MIXINTYPES);
        root.commit();

        assertExecution("requirementRemoved", testPath, null);
    }

    @Test
    public void removeMixinPropertyWithLoginPath() throws Exception {
        setupAuthRequirement(testTree, "/login/path");
        testHandler.calls.clear();

        testTree.removeProperty(NodeTypeConstants.JCR_MIXINTYPES);
        root.commit();

        assertExecution("requirementRemoved", testPath, "/login/path");
    }

    @Test
    public void clearMixins() throws Exception {
        setupAuthRequirement(testTree, null);
        testHandler.calls.clear();

        new NodeUtil(testTree).setNames(NodeTypeConstants.JCR_MIXINTYPES);
        root.commit();

        assertExecution("requirementRemoved", testPath, null);
    }

    @Test
    public void removeNodeAndMixin() throws Exception {
        setupAuthRequirement(testTree, null);
        testHandler.calls.clear();

        testTree.remove();
        root.commit();

        assertExecution("requirementRemoved", testPath, null);
    }

    @Test
    public void removeNodeAndMixinWithLoginPath() throws Exception {
        setupAuthRequirement(testTree, "/login/path");
        testHandler.calls.clear();

        testTree.remove();
        root.commit();

        assertExecution("requirementRemoved", testPath, "/login/path");
    }

    @Test
    public void removeNodeWithLoginPath() throws Exception {
        setupLoginPath(testTree, "/login/path");
        testHandler.calls.clear();

        testTree.remove();
        root.commit();

        assertTrue(testHandler.calls.isEmpty());
    }

    @Test
    public void addLoginPathNoMixin() throws Exception {
        setupLoginPath(testTree, "/login/path");
        assertTrue(testHandler.calls.isEmpty());
    }

    @Test
    public void addLoginPathExistingMixin() throws Exception {
        setupAuthRequirement(testTree, null);
        testHandler.calls.clear();

        setupLoginPath(testTree, "/login/path");
        assertExecution("loginPathAdded", testPath, "/login/path");
    }

    @Test
    public void modifyLoginPathNoMixin() throws Exception {
        setupLoginPath(testTree, "/login/path");
        testHandler.calls.clear();

        testTree.setProperty(Constants.NAME_SLING_LOGIN_PATH, "/changed/login/path");
        root.commit();

        assertTrue(testHandler.calls.isEmpty());
    }

    @Test
    public void modifyLoginPathExistingMixin() throws Exception {
        setupAuthRequirement(testTree, "/login/path");
        testHandler.calls.clear();

        testTree.setProperty(Constants.NAME_SLING_LOGIN_PATH, "/changed/login/path");
        root.commit();

        assertExecution("loginPathChanged", testPath, "/login/path", "/changed/login/path");
    }

    @Test
    public void removeLoginPathNoMixin() throws Exception {
        setupLoginPath(testTree, "/login/path");
        testHandler.calls.clear();

        testTree.removeProperty(Constants.NAME_SLING_LOGIN_PATH);
        root.commit();
        assertTrue(testHandler.calls.isEmpty());
    }

    @Test
    public void removeLoginPathExistingMixin() throws Exception {
        setupAuthRequirement(testTree, "/login/path");
        testHandler.calls.clear();

        testTree.removeProperty(Constants.NAME_SLING_LOGIN_PATH);
        root.commit();
        assertExecution("loginPathRemoved", testPath, "/login/path");

    }

    @Test
    public void changeNodeWithMixin() throws Exception {
        setupAuthRequirement(testTree, null);
        testHandler.calls.clear();

        testTree.setProperty("anyProperty", "value");
        root.commit();

        assertTrue(testHandler.calls.isEmpty());
    }

    @Test
    public void changeNodeWithMixinAndLoginPath() throws Exception {
        setupAuthRequirement(testTree, "/login/path");
        testHandler.calls.clear();

        testTree.setProperty("anyProperty", "value");
        root.commit();

        assertTrue(testHandler.calls.isEmpty());
    }

    @Test
    public void addHiddenNode() throws Exception {
        MemoryNodeStore store = new MemoryNodeStore();
        NodeState root = store.getRoot();
        NodeBuilder builder = root.builder();
        NodeBuilder test = builder.child("test");
        NodeBuilder hidden = test.child(":hidden");
        hidden.setProperty(JcrConstants.JCR_MIXINTYPES, ImmutableList.of(Constants.MIX_SLING_AUTHENTICATION_REQUIRED), Type.NAMES);

        NodeStateDiff diff = requirementObserver.getRootDiff(root, builder.getNodeState(), null);
        diff.childNodeAdded("test", test.getNodeState());
        diff.childNodeAdded(":hidden", hidden.getNodeState());

        assertTrue(testHandler.calls.isEmpty());

        diff.childNodeChanged(":hidden", hidden.getNodeState(), hidden.setProperty(Constants.NAME_SLING_LOGIN_PATH, "/login/path").getNodeState());
        assertTrue(testHandler.calls.isEmpty());
    }

    @Test
    public void changeHiddenNode() throws Exception {
        MemoryNodeStore store = new MemoryNodeStore();
        NodeState root = store.getRoot();
        NodeBuilder builder = root.builder();
        NodeBuilder test = builder.child("test");
        NodeBuilder hidden = test.child(":hidden");
        hidden.setProperty(JcrConstants.JCR_MIXINTYPES, ImmutableList.of(Constants.MIX_SLING_AUTHENTICATION_REQUIRED), Type.NAMES);

        NodeStateDiff diff = requirementObserver.getRootDiff(root, builder.getNodeState(), null);
        diff.childNodeChanged(":hidden", hidden.getNodeState(), hidden.setProperty(Constants.NAME_SLING_LOGIN_PATH, "/login/path").getNodeState());

        assertTrue(testHandler.calls.isEmpty());
    }

    @Test
    public void removeHiddenNode() throws Exception {
        MemoryNodeStore store = new MemoryNodeStore();
        NodeState root = store.getRoot();
        NodeBuilder builder = root.builder();
        NodeBuilder test = builder.child("test");
        NodeBuilder hidden = test.child(":hidden");
        hidden.setProperty(JcrConstants.JCR_MIXINTYPES, ImmutableList.of(Constants.MIX_SLING_AUTHENTICATION_REQUIRED), Type.NAMES);

        NodeStateDiff diff = requirementObserver.getRootDiff(root, builder.getNodeState(), null);
        diff.childNodeDeleted("test", test.getNodeState());
        diff.childNodeDeleted(":hidden", hidden.getNodeState());

        assertTrue(testHandler.calls.isEmpty());
    }

    @Test
    public void multipleRequirements() throws Exception {
        setupAuthRequirement(testTree, null);
        testHandler.calls.clear();

        Set<String[]> expected = new HashSet<String[]>();

        Tree child = new NodeUtil(testTree).addChild("child", NodeTypeConstants.NT_OAK_UNSTRUCTURED).getTree();
        TreeUtil.addMixin(child, Constants.MIX_SLING_AUTHENTICATION_REQUIRED, root.getTree(NodeTypeConstants.NODE_TYPES_PATH), null);
        child.setProperty(Constants.NAME_SLING_LOGIN_PATH, "/login/path");
        expected.add(new String[] {"requirementAdded", child.getPath(), "/login/path"});

        Tree child2 = new NodeUtil(testTree).addChild("child2", "myNodeType").getTree();
        expected.add(new String[] {"requirementAdded", child2.getPath(), null});

        testTree.setProperty(Constants.NAME_SLING_LOGIN_PATH, "abc");
        expected.add(new String[] {"loginPathAdded", testTree.getPath(), "abc"});

        root.commit();

        assertExecution(expected);
    }

    @Test
    public void multipleRequirements2() throws Exception {
        setupAuthRequirement(testTree, "/login/path");

        Tree child = new NodeUtil(testTree).addChild("child", NodeTypeConstants.NT_OAK_UNSTRUCTURED).getTree();
        setupAuthRequirement(child, "/login/path");

        Tree child2 = new NodeUtil(testTree).addChild("child2", "myNodeType").getTree();
        root.commit();

        testHandler.calls.clear();

        Set<String[]> expected = new HashSet<String[]>();

        child2.remove();
        expected.add(new String[] {"requirementRemoved", child2.getPath(), null});

        child.setProperty(Constants.NAME_SLING_LOGIN_PATH, "a/b/c");
        expected.add(new String[] {"loginPathChanged", child.getPath(), "/login/path", "a/b/c"});

        testTree.removeProperty(JcrConstants.JCR_MIXINTYPES);
        expected.add(new String[] {"requirementRemoved", testTree.getPath(), "/login/path"});

        root.commit();

        assertExecution(expected);
    }
}