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

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
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
    protected Resource execute(final List<Modification> changes,
            final Resource source,
            final String destParent,
            String destName,
            final VersioningConfiguration versioningConfiguration)
    throws PersistenceException {
        if (destName == null) {
            destName = source.getName();
        }

        final Resource destParentRsrc = source.getResourceResolver().getResource(destParent);
        final Resource dest = destParentRsrc.getChild(destName);
        if (dest != null ) {
            source.getResourceResolver().delete(dest);
        }

        // ensure we have an item underlying the request's resource
        final Object item = this.jcrSsupport.getItem(source);
        final Object target = this.jcrSsupport.getNode(destParentRsrc);

        if (item == null || target == null ) {
            move(source, destParentRsrc);
        } else {
            this.jcrSsupport.checkoutIfNecessary(source.getParent(), changes, versioningConfiguration);
            this.jcrSsupport.move(item, target, destName);
        }
        final Resource result = destParentRsrc.getChild(destName);
        if ( result != null ) {
            changes.add(Modification.onMoved(source.getPath(), result.getPath()));
        }
        return result;
    }

    /**
     * Move the source as a child resource to the parent
     */
    private void move(final Resource source, final Resource dest)
    throws PersistenceException {
        // first copy
        final ValueMap vm = source.getValueMap();
        final Resource result = source.getResourceResolver().create(dest, source.getName(), vm);
        for(final Resource c : source.getChildren()) {
            move(c, result);
        }
        // then delete
        source.getResourceResolver().delete(source);
    }
}
