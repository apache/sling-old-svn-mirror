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
 * The <code>CopyOperation</code> class implements the
 * {@link org.apache.sling.servlets.post.SlingPostConstants#OPERATION_COPY copy}
 * operation for the Sling default POST servlet.
 */
public class CopyOperation extends AbstractCopyMoveOperation {

    @Override
    protected String getOperationName() {
        return "copy";
    }

    @Override
    protected Resource execute(final List<Modification> changes,
            final Resource source,
            final String destParent,
            final String destName,
            final VersioningConfiguration versioningConfiguration)
    throws PersistenceException {
        final Resource parentRsrc = source.getResourceResolver().getResource(destParent);
        // check if the item is backed by JCR
        final Object item = this.jcrSsupport.getItem(source);
        final Object parentItem = this.jcrSsupport.getNode(parentRsrc);
        if ( item == null || parentItem == null ) {
            // no JCR, copy via resources
            final Resource result = copy(source, parentRsrc);
            changes.add(Modification.onCopied(source.getPath(), result.getPath()));
            return result;
        } else {
            final String dest = this.jcrSsupport.copy(item, parentItem, destName);
            changes.add(Modification.onCopied(source.getPath(), dest));
            log.debug("copy {} to {}", source, dest);
            return source.getResourceResolver().getResource(dest);
        }
    }

    /**
     * Copy the source as a child resource to the parent
     */
    private Resource copy(final Resource source, final Resource dest)
    throws PersistenceException {
        final ValueMap vm = source.getValueMap();
        final Resource result = source.getResourceResolver().create(dest, source.getName(), vm);
        for(final Resource c : source.getChildren()) {
            copy(c, result);
        }
        return result;
    }
}
