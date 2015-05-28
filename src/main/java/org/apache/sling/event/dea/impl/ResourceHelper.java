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
package org.apache.sling.event.dea.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;

public abstract class ResourceHelper {

    public static final String PROPERTY_MARKER_READ_ERROR_LIST = ResourceHelper.class.getName() + "/ReadErrorList";

    public static Map<String, Object> cloneValueMap(final ValueMap vm) throws InstantiationException {
        List<Exception> hasReadError = null;
        try {
            final Map<String, Object> result = new HashMap<String, Object>(vm);
            for(final Map.Entry<String, Object> entry : result.entrySet()) {
                if ( entry.getValue() instanceof InputStream ) {
                    final Object value = vm.get(entry.getKey(), Serializable.class);
                    if ( value != null ) {
                        entry.setValue(value);
                    } else {
                        if ( hasReadError == null ) {
                            hasReadError = new ArrayList<Exception>();
                        }
                        final int count = hasReadError.size();
                        // let's find out which class might be missing
                        ObjectInputStream ois = null;
                        try {
                            ois = new ObjectInputStream((InputStream)entry.getValue());
                            ois.readObject();
                        } catch (final ClassNotFoundException cnfe) {
                             hasReadError.add(new Exception("Unable to deserialize property '" + entry.getKey() + "'", cnfe));
                        } catch (final IOException ioe) {
                            hasReadError.add(new Exception("Unable to deserialize property '" + entry.getKey() + "'", ioe));
                        } finally {
                            if ( ois != null ) {
                                try {
                                    ois.close();
                                } catch (IOException ignore) {
                                    // ignore
                                }
                            }
                        }
                        if ( hasReadError.size() == count ) {
                            hasReadError.add(new Exception("Unable to deserialize property '" + entry.getKey() + "'"));
                        }
                    }
                }
            }
            if ( hasReadError != null ) {
                result.put(PROPERTY_MARKER_READ_ERROR_LIST, hasReadError);
            }
            return result;
        } catch ( final IllegalArgumentException iae) {
            // the JCR implementation might throw an IAE if something goes wrong
            throw (InstantiationException)new InstantiationException(iae.getMessage()).initCause(iae);
        }
    }

    public static ValueMap getValueMap(final Resource resource) throws InstantiationException {
        final ValueMap vm = ResourceUtil.getValueMap(resource);
        // trigger full loading
        try {
            vm.size();
        } catch ( final IllegalArgumentException iae) {
            // the JCR implementation might throw an IAE if something goes wrong
            throw (InstantiationException)new InstantiationException(iae.getMessage()).initCause(iae);
        }
        return vm;
    }

    /**
     * A batch resource remover deletes resources in batches. Once the batch
     * size (threshold) is reached, an intermediate commit is performed. Resource
     * trees are deleted resource by resource starting with the deepest children first.
     * Once all resources have been passed to the batch resource remover, a final
     * commit needs to be called on the resource resolver.
     */
    public static class BatchResourceRemover {

        private final int max;

        private int count;

        public BatchResourceRemover(final int batchSize) {
            this.max = (batchSize < 1 ? 50 : batchSize);
        }

        public void delete(final Resource rsrc)
        throws PersistenceException {
            final ResourceResolver resolver = rsrc.getResourceResolver();
            for(final Resource child : rsrc.getChildren()) {
                delete(child);
            }
            resolver.delete(rsrc);
            count++;
            if ( count >= max ) {
                resolver.commit();
                count = 0;
            }
        }
    }

    /**
     * Create a batch resource remover.
     * A batch resource remove can be used to delete resources in batches.
     * Once the passed in threshold of deleted resources is reached, an intermediate
     * commit is called on the resource resolver. In addition the batch remover
     * deletes a resource recursively.
     * Once all resources to delete are passed to the remover, a final commit needs
     * to be call on the resource resolver.
     * @param threshold The threshold for the intermediate saves.
     * @return A new batch resource remover.
     */
    public static BatchResourceRemover getBatchResourceRemover(final int threshold) {
        return new BatchResourceRemover(threshold);
    }
}