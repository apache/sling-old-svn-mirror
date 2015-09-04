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
package org.apache.sling.servlets.post;

public enum ModificationType {

    /**
     * Content has been created or updated. The source path provides the path of
     * the modified Item.
     */
    MODIFY,

    /**
     * An Item has been deleted. The source path provides the path of the
     * deleted Item.
     */
    DELETE,

    /**
     * An Item has been moved to a new location. The source provides the
     * original path of the Item, the destination provides the new path of the
     * Item.
     */
    MOVE,

    /**
     * An Item has been copied to a new location. The source path provides the
     * path of the copied Item, the destination path provides the path of the
     * new Item.
     */
    COPY,

    /**
     * A Node has been created. The source path provides the path of the newly
     * created Node.
     */
    CREATE,

    /**
     * A child Node has been reordered. The source path provides the path of the
     * node, which has been reordered. The destination path provides the name of
     * the sibbling node before which the source Node has been ordered. which
     * the
     */
    ORDER,

    /**
     * A Node has been checked out. The source path provides the path of the node.
     */
    CHECKOUT,

    /**
     * A Node has been checked in. The source path provides the path of the node.
     */
    CHECKIN,
    
    /**
     * A Node has been restored to a given version. The soruce path provides the
     * path of the node and the destination describes the target version.
     */
    RESTORE
}
