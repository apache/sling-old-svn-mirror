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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.jackrabbit.oak.api.Tree;
import org.apache.jackrabbit.oak.plugins.nodetype.TypePredicate;
import org.apache.jackrabbit.oak.plugins.tree.TreeFactory;
import org.apache.jackrabbit.oak.plugins.version.VersionConstants;
import org.apache.jackrabbit.oak.spi.commit.CommitInfo;
import org.apache.jackrabbit.oak.spi.commit.DiffObserver;
import org.apache.jackrabbit.oak.spi.state.DefaultNodeStateDiff;
import org.apache.jackrabbit.oak.spi.state.NodeState;
import org.apache.jackrabbit.oak.spi.state.NodeStateDiff;
import org.apache.jackrabbit.oak.spi.state.NodeStateUtils;
import org.apache.jackrabbit.oak.util.TreeUtil;

import static org.apache.jackrabbit.oak.plugins.memory.EmptyNodeState.MISSING_NODE;

/**
 * {@link org.apache.jackrabbit.oak.spi.commit.Observer} implementation that looks
 * for the following changes made to {@link NodeState}s:
 *
 * <ul></ul>
 * <li>addition of new nodes that are of type {@link #MIX_SLING_AUTHENTICATION_REQUIRED},
 *     either having the mixin type specified or by super type inheritance</li>
 * <li>modification of existing nodes that changes their effective node type adding
 *     or removing {@link #MIX_SLING_AUTHENTICATION_REQUIRED}</li>
 * <li>removal of nodes that have an effective node type including {@link #MIX_SLING_AUTHENTICATION_REQUIRED}</li>
 * <li>addition, modification or removal of the optional {@link #NAME_SLING_LOGIN_PATH} property</li>
 * </ul>
 *
 * These changes are then reported to the {@link RequirementHandler} for further
 * processing. See {@link DefaultRequirementHandler} for the default handling.
 */
class RequirementObserver extends DiffObserver implements Constants {

    private final RequirementHandler requirementHandler;

    private TypePredicate isMixAuthRequired;

    RequirementObserver(@Nonnull RequirementHandler requirementHandler) {
        this.requirementHandler = requirementHandler;
    }

    @Override
    protected NodeStateDiff getRootDiff(@Nonnull NodeState before, @Nonnull NodeState after, @Nullable CommitInfo info) {
        isMixAuthRequired = new TypePredicate(after, MIX_SLING_AUTHENTICATION_REQUIRED);
        return new Diff(TreeFactory.createReadOnlyTree(before), TreeFactory.createReadOnlyTree(after));
    }

    private final class Diff extends DefaultNodeStateDiff implements VersionConstants {

        private final Tree treeBefore;
        private final Tree treeAfter;

        private Diff(@Nonnull Tree treeBefore,
                     @Nonnull Tree treeAfter) {
            this.treeBefore = treeBefore;
            this.treeAfter = treeAfter;
        }

        /**
         * Test if the added node is of type 'sling:AuthenticationRequired' (either
         * by having the corresponding mixin type or by super type inheritance.
         *
         * If it is 'sling:AuthenticationRequired' notify the {@code RequirementHandler}
         * about the new auth-requirement, which may optionally come with a configured
         * login path.
         *
         * @param name The name of the new node
         * @param after The state of the new node.
         * @return {@code true} if the given node is a hidden node otherwise the
         * result of the node state comparison.
         */
        @Override
        public boolean childNodeAdded(String name, NodeState after) {
            if (NodeStateUtils.isHidden(name)) {
                return true;
            }
            Tree treeA = TreeFactory.createReadOnlyTree(treeAfter, name, after);
            if (isMixAuthRequired.apply(treeA)) {
                requirementHandler.requirementAdded(treeA.getPath(), getLoginPath(treeA));
            }
            return after.compareAgainstBaseState(MISSING_NODE, new Diff(TreeFactory.createReadOnlyTree(treeBefore, name, MISSING_NODE), treeA));
        }

        /**
         * Compare the two node states verifying if either the effective node type
         * has changed with respect to 'sling:AuthenticationRequired' or if the
         * configured login path has changed (in case both states are defined to
         * require authentication).
         *
         * @param name The name of the new node
         * @param before The node state before
         * @param after The node state after
         * @return {@code true} if the given node is a hidden node otherwise the
         * result of the node state comparison.
         */
        @Override
        public boolean childNodeChanged(String name, NodeState before, NodeState after) {
            if (NodeStateUtils.isHidden(name)) {
                return true;
            }

            Tree treeB = TreeFactory.createReadOnlyTree(treeBefore, name, before);
            Tree treeA = TreeFactory.createReadOnlyTree(treeAfter, name, after);

            boolean requiredOnA = isMixAuthRequired.apply(treeA);
            boolean requiredOnB = isMixAuthRequired.apply(treeB);

            if (requiredOnA && requiredOnB) {
                // 'sling:AuthenticationRequired' has not changed on the node.
                // verify if just the login-path has been modified, which would
                // result in an update of the login-path only.

                String loginPathBefore = getLoginPath(treeB);
                String loginPathAfter = getLoginPath(treeA);

                if (loginPathBefore != null && loginPathAfter != null) {
                    if (!loginPathBefore.equals(loginPathAfter)) {
                        requirementHandler.loginPathChanged(treeA.getPath(), loginPathBefore, loginPathAfter);
                    } // else: no changes related to login path
                } else if (loginPathBefore != null) {
                    // login path has been removed without removing auth-requirement
                    requirementHandler.loginPathRemoved(treeB.getPath(), loginPathBefore);
                } else if (loginPathAfter != null) {
                    // login path has been added (auth-requirement was already present)
                    requirementHandler.loginPathAdded(treeA.getPath(), loginPathAfter);
                }
            } else if (requiredOnA) {
                // new auth-requirement has been added through mixin types or changes
                // to primary type; it may optionally have a login path defined
                requirementHandler.requirementAdded(treeA.getPath(), getLoginPath(treeA));
            } else if (requiredOnB){
                // auth-requirement has been removed (by changing the mixin types
                // or by changing the primary type; it may have had a login path defined.
                // NOTE: if the mixin gets removed without removing the 'sling:loginPath'
                // that property no longer is defined by the 'sling:AuthenticationRequired'
                // type and thus changes it's semantics.
                requirementHandler.requirementRemoved(treeB.getPath(), getLoginPath(treeB));
            }

            return after.compareAgainstBaseState(before, new Diff(treeB, treeA));
        }

        /**
         * Test if the removed node was of type 'sling:AuthenticationRequired' (either
         * by having had the corresponding mixin type or by super type inheritance.
         *
         * If it used to be 'sling:AuthenticationRequired' notify the {@code RequirementHandler}
         * about the removed auth-requirement, which optionally may be have been
         * associated with a login path.
         *
         * @param name The name of the new node
         * @param after The state of the new node.
         * @return {@code true} if the given before state was a hidden node otherwise the
         * result of the node state comparison.
         */
        @Override
        public boolean childNodeDeleted(String name, NodeState before) {
            if (NodeStateUtils.isHidden(name)) {
                return true;
            }
            Tree treeB = TreeFactory.createReadOnlyTree(treeBefore, name, before);
            if (isMixAuthRequired.apply(treeB)) {
                requirementHandler.requirementRemoved(treeB.getPath(), getLoginPath(treeB));
            }
            return before.compareAgainstBaseState(before, new Diff(treeB, TreeFactory.createReadOnlyTree(treeAfter, name, MISSING_NODE)));
        }

        @CheckForNull
        private String getLoginPath(@Nonnull Tree tree) {
            return TreeUtil.getString(tree, NAME_SLING_LOGIN_PATH);
        }
    }
}