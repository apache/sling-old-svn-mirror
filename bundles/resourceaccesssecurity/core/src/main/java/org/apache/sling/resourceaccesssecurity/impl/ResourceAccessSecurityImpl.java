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
package org.apache.sling.resourceaccesssecurity.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.security.AccessSecurityException;
import org.apache.sling.api.security.ResourceAccessSecurity;
import org.apache.sling.resourceaccesssecurity.ResourceAccessGate;
import org.apache.sling.resourceaccesssecurity.ResourceAccessGate.GateResult;
import org.osgi.framework.ServiceReference;

public abstract class ResourceAccessSecurityImpl implements ResourceAccessSecurity {

    private List<ResourceAccessGateHandler> allHandlers = Collections.emptyList();

    private final boolean defaultAllow;

    public ResourceAccessSecurityImpl(final boolean defaultAllow) {
        this.defaultAllow = defaultAllow;
    }

    /**
     * This method returns either an iterator delivering the matching handlers
     * or <code>null</code>.
     */
    private Iterator<ResourceAccessGateHandler> getMatchingResourceAccessGateHandlerIterator(
            final String path, final ResourceAccessGate.Operation operation) {
        //
        // TODO: maybe caching some frequent paths with read operation would be
        // a good idea
        //
        final List<ResourceAccessGateHandler> handlers = allHandlers;
        if (handlers.size() > 0) {

            final Iterator<ResourceAccessGateHandler> iter = handlers.iterator();
            return new Iterator<ResourceAccessGateHandler>() {

                private ResourceAccessGateHandler next;

                {
                    peek();
                }

                private void peek() {
                    this.next = null;
                    while ( iter.hasNext() && next == null ) {
                        final ResourceAccessGateHandler handler = iter.next();
                        if (handler.matches(path, operation)) {
                            next = handler;
                        }
                    }
                }

                @Override
                public boolean hasNext() {
                    return next != null;
                }

                @Override
                public ResourceAccessGateHandler next() {
                    if ( next == null ) {
                        throw new NoSuchElementException();
                    }
                    final ResourceAccessGateHandler handler = this.next;
                    peek();
                    return handler;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        return null;
    }

    @Override
    public Resource getReadableResource(final Resource resource) {
        Resource returnValue = (this.defaultAllow ? resource : null);

        final Iterator<ResourceAccessGateHandler> accessGateHandlers = getMatchingResourceAccessGateHandlerIterator(
                resource.getPath(), ResourceAccessGate.Operation.READ);

        GateResult finalGateResult = null;
        List<ResourceAccessGate> accessGatesForReadValues = null;
        boolean canReadAllValues = false;


        if ( accessGateHandlers != null ) {

            while ( accessGateHandlers.hasNext() ) {
                final ResourceAccessGateHandler resourceAccessGateHandler  = accessGateHandlers.next();

                final GateResult gateResult = resourceAccessGateHandler.getResourceAccessGate().canRead(resource);
                if (!canReadAllValues && gateResult == GateResult.GRANTED) {
                    if (resourceAccessGateHandler.getResourceAccessGate().canReadAllValues(resource)) {
                        canReadAllValues = true;
                        accessGatesForReadValues = null;
                    } else {
                        if (accessGatesForReadValues == null) {
                            accessGatesForReadValues = new ArrayList<ResourceAccessGate>();
                        }
                        accessGatesForReadValues.add(resourceAccessGateHandler.getResourceAccessGate());
                    }
                }
                if (finalGateResult == null) {
                    finalGateResult = gateResult;
                } else if (finalGateResult != GateResult.GRANTED && gateResult != GateResult.DONTCARE) {
                    finalGateResult = gateResult;
                }
                // stop checking if the operation is final and the result not GateResult.DONTCARE
                if (gateResult != GateResult.DONTCARE  && resourceAccessGateHandler.isFinalOperation(ResourceAccessGate.Operation.READ)) {
                    break;
                }
            }


            // return null if access is denied or no ResourceAccessGate is present
            if (finalGateResult == null || finalGateResult == GateResult.DENIED) {
                returnValue = null;
            } else if (finalGateResult == GateResult.DONTCARE) {
                returnValue = (this.defaultAllow ? resource : null);
            } else if (finalGateResult == GateResult.GRANTED ) {
                returnValue = resource;
            }
        }

        boolean canUpdateResource = canUpdate(resource);

        // wrap Resource if read access is not or partly (values) not granted
        if (returnValue != null) {
            if( !canReadAllValues || !canUpdateResource ) {
                returnValue = new AccessGateResourceWrapper(returnValue,
                        accessGatesForReadValues,
                        canUpdateResource);
            }
        }

        return returnValue;
    }

    @Override
    public boolean canCreate(final String path,
            final ResourceResolver resolver) {
        final Iterator<ResourceAccessGateHandler> handlers = getMatchingResourceAccessGateHandlerIterator(
                path, ResourceAccessGate.Operation.CREATE);
        boolean result = this.defaultAllow;
        if ( handlers != null ) {
            GateResult finalGateResult = null;

            while ( handlers.hasNext() ) {
                final ResourceAccessGateHandler resourceAccessGateHandler  = handlers.next();

                final GateResult gateResult = resourceAccessGateHandler.getResourceAccessGate().canCreate(path, resolver);
                if (finalGateResult == null) {
                    finalGateResult = gateResult;
                } else if (finalGateResult != GateResult.GRANTED && gateResult != GateResult.DONTCARE) {
                    finalGateResult = gateResult;
                }
                if (finalGateResult == GateResult.GRANTED || gateResult != GateResult.DONTCARE && 
                        resourceAccessGateHandler.isFinalOperation(ResourceAccessGate.Operation.CREATE)) {
                    break;
                }
            }

            if ( finalGateResult == GateResult.GRANTED ) {
                result = true;
            } else if ( finalGateResult == GateResult.DENIED ) {
                result = false;
            }
        }
        return result;
    }

    @Override
    public boolean canUpdate(final Resource resource) {
        final Iterator<ResourceAccessGateHandler> handlers = getMatchingResourceAccessGateHandlerIterator(
                resource.getPath(), ResourceAccessGate.Operation.UPDATE);
        boolean result = this.defaultAllow;
        if ( handlers != null ) {
            GateResult finalGateResult = null;

            while ( handlers.hasNext() ) {
                final ResourceAccessGateHandler resourceAccessGateHandler  = handlers.next();

                final GateResult gateResult = resourceAccessGateHandler.getResourceAccessGate().canUpdate(resource);
                if (finalGateResult == null) {
                    finalGateResult = gateResult;
                } else if (finalGateResult != GateResult.GRANTED && gateResult != GateResult.DONTCARE) {
                    finalGateResult = gateResult;
                }
                if (finalGateResult == GateResult.GRANTED || gateResult != GateResult.DONTCARE && 
                        resourceAccessGateHandler.isFinalOperation(ResourceAccessGate.Operation.UPDATE)) {
                    break;
                }
            }

            if ( finalGateResult == GateResult.GRANTED ) {
                result = true;
            } else if ( finalGateResult == GateResult.DENIED ) {
                result = false;
            }
        }
        return result;
    }

    @Override
    public boolean canDelete(final Resource resource) {
        final Iterator<ResourceAccessGateHandler> handlers = getMatchingResourceAccessGateHandlerIterator(
                resource.getPath(), ResourceAccessGate.Operation.DELETE);
        boolean result = this.defaultAllow;
        if ( handlers != null ) {
            GateResult finalGateResult = null;

            while ( handlers.hasNext() ) {
                final ResourceAccessGateHandler resourceAccessGateHandler  = handlers.next();

                final GateResult gateResult = resourceAccessGateHandler.getResourceAccessGate().canDelete(resource);
                if (finalGateResult == null) {
                    finalGateResult = gateResult;
                } else if (finalGateResult != GateResult.GRANTED && gateResult != GateResult.DONTCARE) {
                    finalGateResult = gateResult;
                }
                if (finalGateResult == GateResult.GRANTED || gateResult != GateResult.DONTCARE && 
                        resourceAccessGateHandler.isFinalOperation(ResourceAccessGate.Operation.DELETE)) {
                    break;
                }
            }

            if ( finalGateResult == GateResult.GRANTED ) {
                result = true;
            } else if ( finalGateResult == GateResult.DENIED ) {
                result = false;
            }
        }
        return result;
    }

    @Override
    public boolean canExecute(final Resource resource) {
        final Iterator<ResourceAccessGateHandler> handlers = getMatchingResourceAccessGateHandlerIterator(
                resource.getPath(), ResourceAccessGate.Operation.EXECUTE);
        boolean result = this.defaultAllow;
        if ( handlers != null ) {
            GateResult finalGateResult = null;

            while ( handlers.hasNext() ) {
                final ResourceAccessGateHandler resourceAccessGateHandler  = handlers.next();

                final GateResult gateResult = resourceAccessGateHandler.getResourceAccessGate().canExecute(resource);
                if (finalGateResult == null) {
                    finalGateResult = gateResult;
                } else if (finalGateResult != GateResult.GRANTED && gateResult != GateResult.DONTCARE) {
                    finalGateResult = gateResult;
                }
                if (finalGateResult == GateResult.GRANTED || gateResult != GateResult.DONTCARE && resourceAccessGateHandler.isFinalOperation(ResourceAccessGate.Operation.EXECUTE)) {
                    break;
                }
            }

            if ( finalGateResult == GateResult.GRANTED ) {
                result = true;
            } else if ( finalGateResult == GateResult.DENIED ) {
                result = false;
            }
        }
        return result;
    }

    @Override
    public boolean canReadValue(final Resource resource, final String valueName) {
        // TODO Auto-generated method stub
        return this.defaultAllow;
    }

    @Override
    public boolean canSetValue(final Resource resource, final String valueName) {
        // TODO Auto-generated method stub
        return this.defaultAllow;
    }

    @Override
    public boolean canDeleteValue(final Resource resource, final String valueName) {
        // TODO Auto-generated method stub
        return this.defaultAllow;
    }

    @Override
    public String transformQuery(final String query,
            final String language,
            final ResourceResolver resourceResolver)
    throws AccessSecurityException {
        return query;
    }

    /**
     * Add a new resource access gate
     */
    protected void bindResourceAccessGate(final ServiceReference ref) {
        synchronized ( this ) {
            final List<ResourceAccessGateHandler> newList = new ArrayList<ResourceAccessGateHandler>(this.allHandlers);

            final ResourceAccessGateHandler h = new ResourceAccessGateHandler(ref);
            newList.add(h);
            Collections.sort(newList);
            this.allHandlers = newList;
        }
    }

    /**
     * Remove a resource access gate
     */
    protected void unbindResourceAccessGate(final ServiceReference ref) {
        synchronized ( this ) {
            final List<ResourceAccessGateHandler> newList = new ArrayList<ResourceAccessGateHandler>(this.allHandlers);

            final ResourceAccessGateHandler h = new ResourceAccessGateHandler(ref);
            newList.remove(h);
            this.allHandlers = newList;
        }
    }
}
