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

import org.apache.sling.api.resource.Resource;
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
        final ValueMap vm = resource.getValueMap();
        // trigger full loading
        try {
            vm.size();
        } catch ( final IllegalArgumentException iae) {
            // the JCR implementation might throw an IAE if something goes wrong
            throw (InstantiationException)new InstantiationException(iae.getMessage()).initCause(iae);
        }
        return vm;
    }
}