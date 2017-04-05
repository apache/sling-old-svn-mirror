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
package org.apache.sling.ide.transport;

public interface Repository {
	
	public static String JCR_PRIMARY_TYPE= "jcr:primaryType";
	public static String NT_FILE= "nt:file";
	public static String NT_FOLDER= "nt:folder";
	public static String JCR_ROOT= "jcr:root";
	public static String NT_RESOURCE= "nt:resource";
	public static String JCR_CREATED= "jcr:created";
	public static String JCR_CREATED_BY= "jcr:createdBy";
	public static String JCR_ETAG= "jcr:etag";
	public static String JCR_LIFECYCLE_POLICY= "jcr:lifecyclePolicy";
	public static String JCR_CURRENT_LIFECYCLE_STATE= "jcr:currentLifecycleState";
	public static String JCR_LOCK_OWNER= "jcr:lockOwner";
	public static String JCR_LOCK_IS_DEEP= "jcr:lockIsDeep";
	public static String JCR_UUID= "jcr:uuid";
	public static String JCR_IS_CHECKED_OUT= "jcr:isCheckedOut";
	public static String JCR_VERSION_HISTORY= "jcr:versionHistory";
	public static String JCR_BASE_VERSION= "jcr:baseVersion";
	public static String JCR_PREDECESSORS= "jcr:predecessors";
	public static String JCR_MERGE_FAILED= "jcr:mergeFailed";
	public static String JCR_ACTIVITY= "jcr:activity";
	public static String JCR_CONFIGURATION= "jcr:configuration";
	public static String JCR_ACTIVITY_TITLE= "jcr:activityTitle";
	public static String JCR_MIXIN_TYPES= "jcr:mixinTypes";
	public static String JCR_NAME= "jcr:name";
	public static String JCR_AUTO_CREATED= "jcr:autoCreated";
	public static String JCR_MANDATORY= "jcr:mandatory";
	public static String JCR_ON_PARENT_VERSION= "jcr:onParentVersion";
	public static String JCR_PROTECTED= "jcr:protected";	
	public static String JCR_REQUIRED_PRIMARY_TYPES= "jcr:requiredPrimaryTypes";
	public static String JCR_DEFAULT_PRIMARY_TYPE= "jcr:defaultPrimaryType";
	public static String JCR_SAME_NAME_SIBLINGS= "jcr:sameNameSiblings";
	public static String JCR_FROZEN_PRIMARY_TYPE= "jcr:frozenPrimaryType";
	public static String JCR_FROZEN_MIXIN_TYPES= "jcr:frozenMixinTypes";
	public static String JCR_FROZEN_UUID= "jcr:frozenUuid";
	public static String JCR_NODE_TYPE_NAME= "jcr:nodeTypeName";
	public static String JCR_SUPER_TYPES= "jcr:supertypes";
	public static String JCR_IS_ABSTRACT= "jcr:isAbstract";
	public static String JCR_IS_QUERYABLE= "jcr:isQueryable";
	public static String JCR_IS_MIXIN= "jcr:isMixin";
	public static String JCR_HAS_ORDERABLE_CHILD_NODES= "jcr:hasOrderableChildNodes";
	public static String JCR_PRIMARY_ITEM_NAME= "jcr:primaryItemName";
	public static String JCR_PROPERTY_DEFINITION= "jcr:propertyDefinition";
	public static String JCR_CHILD_NODE_DEFINITION= "jcr:childNodeDefinition";
	public static String JCR_REQUIRED_TYPE= "jcr:requiredType";
	public static String JCR_VALUE_CONSTRAINTS= "jcr:valueConstraints";
	public static String JCR_DEFAULT_VALUES= "jcr:defaultValues";
	public static String JCR_MULTIPLE= "jcr:multiple";
	public static String JCR_AVAILABLE_QUERY_OPERATORS= "jcr:availableQueryOperators";
	public static String JCR_IS_FULL_TEXT_SEARCHABLE= "jcr:isFullTextSearchable";
	public static String JCR_IS_QUERY_ORDERABLE= "jcr:isQueryOrderable";
	public static String JCR_SUCCESSORS= "jcr:successors";
	public static String JCR_FROZENNODE= "jcr:frozenNode";
	public static String JCR_VERSIONABLE_UUID= "jcr:versionableUuid";
	public static String JCR_COPIED_FROM= "jcr:copiedFrom";
	public static String JCR_ROOT_VERSION= "jcr:rootVersion";
	public static String JCR_VERSION_LABELS= "jcr:versionLabels";
	public static String JCR_CHILD_VERSION_HISTORY= "jcr:childVersionHistory";

    public enum CommandExecutionFlag {

        /**
         * Signal the command to only create the nodes when they are missing
         * 
         * <p>
         * If nodes exist, they will not be touched
         */
        CREATE_ONLY_WHEN_MISSING;
    }
 	
    RepositoryInfo getRepositoryInfo();

    Command<Void> newAddOrUpdateNodeCommand(CommandContext context, FileInfo fileInfo, ResourceProxy resourceProxy,
            CommandExecutionFlag... flags);

    /**
     * Reorder the child nodes under the specified resource
     * 
     * <p>
     * Only the first-level child nodes are typically ordered, but if child nodes are completely covered they will be
     * ordered recursively.
     * </p>
     * 
     * @param resourceProxy
     * @return
     */
    Command<Void> newReorderChildNodesCommand(ResourceProxy resourceProxy);
	
    Command<Void> newDeleteNodeCommand(String path);
 
    /**
     * Retrieves information about the resource located at <tt>path</tt> and its direct descendants
     * 
     * @param path
     * @return a <tt>ResourceProxy</tt> rooted at <tt>path</tt> and its direct descendants
     */
    Command<ResourceProxy> newListChildrenNodeCommand(String path);
 	
    /**
     * Retrieves all properties of a resource located at <tt>path</tt>
     * 
     * @param path
     * @return all properties for the resource located at <tt>path</tt>
     */
    Command<ResourceProxy> newGetNodeContentCommand(String path);

	Command<byte[]> newGetNodeCommand(String path);
	
	/**
	 * Returns the node type registry - when the underlying server is started -
	 * or null when the server is not started at the moment.
	 * @return the node type registry - when the underlying server is started -
     * or null when the server is not started at the moment
	 */
	NodeTypeRegistry getNodeTypeRegistry();

}
