/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.jcr.contentloader;

import aQute.bnd.annotation.ConsumerType;

/**
 * Listener interface to provide callbacks for all imported updates
 * for interested parties.  This is primarily used to record
 * the modifications during the "import" post operation.
 */
@ConsumerType
public interface ContentImportListener {

    /**
     * Content has been updated. The source path provides the path of
     * the modified Item.
     */
	void onModify(String srcPath);

    /**
     * An Item has been deleted. The source path provides the path of the
     * deleted Item.
     */
	void onDelete(String srcPath);
	
    /**
     * An Item has been moved to a new location. The source provides the
     * original path of the Item, the destination provides the new path of the
     * Item.
     */
	void onMove(String srcPath, String destPath);

    /**
     * An Item has been copied to a new location. The source path provides the
     * path of the copied Item, the destination path provides the path of the
     * new Item.
     */
	void onCopy(String srcPath, String destPath);

    /**
     * A Node has been created. The source path provides the path of the newly
     * created Node.
     */
	void onCreate(String srcPath);

    /**
     * A child Node has been reordered. The orderedPath provides the path of the
     * node, which has been reordered. ThebeforeSibbling provides the name of
     * the sibling node before which the source Node has been ordered. 
     */
	void onReorder(String orderedPath, String beforeSibbling);
	
    /**
     * A versionable Node has been checked in. The source path provides the path of the 
     * newly checked in Node.
     * @since 2.1.4
     */
	void onCheckin(String srcPath);

	/**
     * A versionable Node has been checked out. The source path provides the path of the 
     * newly checked out Node.
     * @since 2.1.4
     */
	void onCheckout(String srcPath);
}
