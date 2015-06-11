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
package org.apache.sling.servlets.post.impl.operations;

import java.util.List;

import javax.jcr.Item;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.VersioningConfiguration;

/**
 * The <code>MoveOperation</code> class implements the
 * {@link org.apache.sling.servlets.post.SlingPostConstants#OPERATION_MOVE move}
 * operation for the Sling default POST servlet.
 */
public class MoveOperation extends AbstractCopyMoveOperation {

    @Override
    protected String getOperationName() {
        return "move";
    }

    @Override
    protected Item execute(List<Modification> changes, Item source,
            String destParent, String destName,
            VersioningConfiguration versioningConfiguration) throws RepositoryException {

        if (destName == null) {
            destName = source.getName();
        }

        String sourcePath = source.getPath();
        if (destParent.equals("/")) {
            destParent = "";
        }
        String destPath = destParent + "/" + destName;
        Session session = source.getSession();
        
        checkoutIfNecessary(source.getParent(), changes, versioningConfiguration);

        if (session.itemExists(destPath)) {
            session.getItem(destPath).remove();
        }
        
        session.move(sourcePath, destPath);
        changes.add(Modification.onMoved(sourcePath, destPath));
        return session.getItem(destPath);
    }

}
