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
package org.apache.sling.ujax;

import java.util.List;
import java.util.LinkedList;

/**
 * Implements a log that records the changes made to the repository during
 * a ujax post request. It can be used to generate a status response.
 */
public class ChangeLog {

    /**
     * list of changes
     */
    private final List<Change> changes = new LinkedList<Change>();

    /**
     * Records a 'modified' change
     * @param path path of the item that was modified
     */
    public void onModified(String path) {
        changes.add(new Change(Change.Type.MODIFIED, path));
    }

    /**
     * Records a 'created' change
     * @param path path of the item that was created
     */
    public void onCreated(String path) {
        changes.add(new Change(Change.Type.CREATED, path));
    }

    /**
     * Records a 'deleted' change
     * @param path path of the item that was deleted
     */
    public void onDeleted(String path) {
        if (path != null) {
            changes.add(new Change(Change.Type.DELETED, path));
        }
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
    public void onMoved(String srcPath, String dstPath) {
        changes.add(new Change(Change.Type.MOVED, srcPath, dstPath));
    }

    /**
     * Dumps the changelog to the given buffer
     * @param out the string buffer
     * @param lf linefeed string. eg. <br/> or "\n"
     */
    public void dump(StringBuffer out, String lf) {
        for (Change c: changes) {
            out.append(c.toString());
            out.append(lf);
        }
    }

    /**
     * Look if item is prepared for deletion.
     *
     * @param path item path
     * @return true if prepared for deletion
     */
    public boolean isDeleted(String path) {
        for (Change c: changes) {
            if (c.getType() == Change.Type.DELETED) {
                if (c.getArguments().length > 0) {
                    String delPath = c.getArguments()[0];
                    if (delPath.equals(path)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}