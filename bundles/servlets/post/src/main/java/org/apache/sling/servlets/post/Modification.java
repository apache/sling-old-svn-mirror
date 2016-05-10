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

public class Modification {

    private final ModificationType type;

    private final String source;

    private final String destination;

    public Modification(final ModificationType type, final String source,
            final String destination) {
        this.type = type;
        this.source = source;
        this.destination = destination;
    }

    public ModificationType getType() {
        return type;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    /**
     * Records a 'modified' change
     *
     * @param path path of the item that was modified
     */
    public static Modification onModified(String path) {
        return onChange(ModificationType.MODIFY, path);
    }

    /**
     * Records a 'created' change
     *
     * @param path path of the item that was created
     */
    public static Modification onCreated(String path) {
        return onChange(ModificationType.CREATE, path);
    }

    /**
     * Records a 'deleted' change
     *
     * @param path path of the item that was deleted
     */
    public static Modification onDeleted(String path) {
        return onChange(ModificationType.DELETE, path);
    }

    /**
     * Records a 'moved' change.
     * <p/>
     * Note: the moved change only records the basic move command. the implied
     * changes on the moved properties and sub nodes are not recorded.
     *
     * @param srcPath source path of the node that was moved
     * @param dstPath destination path of the node that was moved.
     */
    public static Modification onMoved(String srcPath, String dstPath) {
        return onChange(ModificationType.MOVE, srcPath, dstPath);
    }

    /**
     * Records a 'copied' change.
     * <p/>
     * Note: the copy change only records the basic copy command. the implied
     * changes on the copied properties and sub nodes are not recorded.
     *
     * @param srcPath source path of the node that was copied
     * @param dstPath destination path of the node that was copied.
     */
    public static Modification onCopied(String srcPath, String dstPath) {
        return onChange(ModificationType.COPY, srcPath, dstPath);
    }

    /**
     * Records a 'order' change.
     *
     * @param orderedPath Path of the node that was reordered
     * @param beforeSibling Name of the sibling node before which the source node has
     *            been inserted.
     */
    public static Modification onOrder(String orderedPath, String beforeSibling) {
        return onChange(ModificationType.ORDER, orderedPath, beforeSibling);
    }

    protected static Modification onChange(ModificationType type, String source) {
        return onChange(type, source, null);
    }

    protected static Modification onChange(ModificationType type,
            final String source, final String dest) {
        return new Modification(type, source, dest);
    }

    public static Modification onCheckin(String path) {
        return onChange(ModificationType.CHECKIN, path, null);
    }

    public static Modification onCheckout(String path) {
        return onChange(ModificationType.CHECKOUT, path, null);
    }

    public static Modification onRestore(String path, String version) {
        return onChange(ModificationType.RESTORE, path, version);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Modification[type=").append(type).append(", source=").append(source);
        if (destination != null) {
            builder.append(", dest=").append(destination);
        }
        builder.append("]");
        return builder.toString();
    }
}
