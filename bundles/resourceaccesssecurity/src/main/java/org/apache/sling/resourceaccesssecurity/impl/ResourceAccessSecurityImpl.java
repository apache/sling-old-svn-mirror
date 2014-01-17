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

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.ReferencePolicyOption;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.security.AccessSecurityException;
import org.apache.sling.api.security.ResourceAccessSecurity;
import org.apache.sling.resourceaccesssecurity.ResourceAccessGate;
import org.apache.sling.resourceaccesssecurity.ResourceAccessGate.GateResult;
import org.osgi.framework.ServiceReference;

@Reference(policyOption=ReferencePolicyOption.GREEDY,
cardinality=ReferenceCardinality.OPTIONAL_UNARY,
policy=ReferencePolicy.DYNAMIC,
target="(" + ResourceAccessSecurity.CONTEXT + "=" + ResourceAccessSecurity.PROVIDER_CONTEXT + ")")

public class ResourceAccessSecurityImpl implements ResourceAccessSecurity {

    private List<ResourceAccessGateHandler> allHandlers = Collections.emptyList();

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
        Resource returnValue = resource;

        final Iterator<ResourceAccessGateHandler> accessGateHandlers = getMatchingResourceAccessGateHandlerIterator(
                resource.getPath(), ResourceAccessGate.Operation.READ);
        if ( accessGateHandlers != null ) {
            GateResult finalGateResult = null;
            boolean canReadAllValues = false;
            List<ResourceAccessGate> accessGatesForValues = null;

            while ( accessGateHandlers.hasNext() ) {
                final ResourceAccessGateHandler resourceAccessGateHandler  = accessGateHandlers.next();

                final GateResult gateResult = resourceAccessGateHandler.getResourceAccessGate().canRead(resource);
                if (!canReadAllValues && gateResult == GateResult.GRANTED) {
                    if (resourceAccessGateHandler.getResourceAccessGate().canReadAllValues(resource)) {
                        canReadAllValues = true;
                        accessGatesForValues = null;
                    } else {
                        if (accessGatesForValues == null) {
                            accessGatesForValues = new ArrayList<ResourceAccessGate>();
                        }
                        accessGatesForValues.add(resourceAccessGateHandler.getResourceAccessGate());
                    }
                }
                if (finalGateResult == null) {
                    finalGateResult = gateResult;
                } else if (finalGateResult == GateResult.DENIED) {
                    finalGateResult = gateResult;
                }
                if (resourceAccessGateHandler.isFinalOperation(ResourceAccessGate.Operation.READ)) {
                    break;
                }
            }

            // return NonExistingResource if access is denied or no
            // ResourceAccessGate is present
            if (finalGateResult == null || finalGateResult == GateResult.DENIED) {
                returnValue = null;
            } else if (finalGateResult == GateResult.DONTCARE) {
                returnValue = resource;
            }
            // wrap Resource if read access is not or partly (values) not granted
            else if (!canReadAllValues) {
                returnValue = new AccessGateResourceWrapper(resource,
                        accessGatesForValues);
            }
        }
        return returnValue;
    }

    @Override
    public boolean canCreate(final String path,
            final ResourceResolver resolver) {
        final Iterator<ResourceAccessGateHandler> handlers = getMatchingResourceAccessGateHandlerIterator(
                path, ResourceAccessGate.Operation.CREATE);
        boolean result = true;
        if ( handlers != null ) {
            GateResult finalGateResult = null;

            while ( handlers.hasNext() ) {
                final ResourceAccessGateHandler resourceAccessGateHandler  = handlers.next();

                final GateResult gateResult = resourceAccessGateHandler.getResourceAccessGate().canCreate(path, resolver);
                if ( gateResult == GateResult.GRANTED || gateResult == GateResult.DENIED ) {
                    finalGateResult = gateResult;
                    if (resourceAccessGateHandler.isFinalOperation(ResourceAccessGate.Operation.CREATE)) {
                        break;
                    }
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
        boolean result = true;
        if ( handlers != null ) {
            GateResult finalGateResult = null;

            while ( handlers.hasNext() ) {
                final ResourceAccessGateHandler resourceAccessGateHandler  = handlers.next();

                final GateResult gateResult = resourceAccessGateHandler.getResourceAccessGate().canUpdate(resource);
                if ( gateResult == GateResult.GRANTED || gateResult == GateResult.DENIED ) {
                    finalGateResult = gateResult;
                    if (resourceAccessGateHandler.isFinalOperation(ResourceAccessGate.Operation.UPDATE)) {
                        break;
                    }
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
        boolean result = true;
        if ( handlers != null ) {
            GateResult finalGateResult = null;

            while ( handlers.hasNext() ) {
                final ResourceAccessGateHandler resourceAccessGateHandler  = handlers.next();

                final GateResult gateResult = resourceAccessGateHandler.getResourceAccessGate().canDelete(resource);
                if ( gateResult == GateResult.GRANTED || gateResult == GateResult.DENIED ) {
                    finalGateResult = gateResult;
                    if (resourceAccessGateHandler.isFinalOperation(ResourceAccessGate.Operation.DELETE)) {
                        break;
                    }
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
        boolean result = true;
        if ( handlers != null ) {
            GateResult finalGateResult = null;

            while ( handlers.hasNext() ) {
                final ResourceAccessGateHandler resourceAccessGateHandler  = handlers.next();

                final GateResult gateResult = resourceAccessGateHandler.getResourceAccessGate().canExecute(resource);
                if ( gateResult == GateResult.GRANTED || gateResult == GateResult.DENIED ) {
                    finalGateResult = gateResult;
                    if (resourceAccessGateHandler.isFinalOperation(ResourceAccessGate.Operation.EXECUTE)) {
                        break;
                    }
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
        return false;
    }

    @Override
    public boolean canSetValue(final Resource resource, final String valueName) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean canDeleteValue(final Resource resource, final String valueName) {
        // TODO Auto-generated method stub
        return false;
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
