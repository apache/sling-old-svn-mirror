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
package org.apache.sling.resourcemerger.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.iterators.IteratorIterable;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.NonExistingResource;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.hamcrest.ResourceMatchers;
import org.apache.sling.resourcemerger.spi.MergedResourcePicker2;
import org.apache.sling.spi.resource.provider.ResolveContext;
import org.apache.sling.spi.resource.provider.ResourceContext;
import org.apache.sling.testing.resourceresolver.MockHelper;
import org.apache.sling.testing.resourceresolver.MockResourceResolverFactory;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests common merging behaviour (independent of actual picker implementation)
 *
 */
public class CommonMergedResourceProviderTest {

    private ResourceResolver resolver;

    private MergingResourceProvider provider;

    private Resource base;
    private Resource overlay;

    private ResolveContext<Void> ctx;

    /**
     * A very simple resource picker which will just merge two different resources (base and overlay) directly on the mount point.
     * Supports also merging of arbitrary child resources below the mount point.
     */
    private final class SimpleMergedResourcePicker implements MergedResourcePicker2 {

        private final List<Resource> rootResourcesToMerge;

        public SimpleMergedResourcePicker() {
            rootResourcesToMerge = new ArrayList<Resource>();
            rootResourcesToMerge.add(base);
            rootResourcesToMerge.add(overlay);
        }

        @Override
        public List<Resource> pickResources(ResourceResolver resolver, String relativePath, Resource relatedResource) {
            // merging in the root path is always successfull
            if (relativePath.isEmpty()) {
                return rootResourcesToMerge;
            } else {
                List<Resource> resourcesToMerge = new ArrayList<Resource>();
                // if deeper merging is wanted, check in both locations if the relative path is available at all
                Resource resourceToPick = base.getChild(relativePath);
                if (resourceToPick != null) {
                    resourcesToMerge.add(resourceToPick); 
                } else {
                    resourcesToMerge.add(new NonExistingResource(resolver, base.getPath() + "/" + relativePath));
                }
                resourceToPick = overlay.getChild(relativePath);
                if (resourceToPick != null) {
                    resourcesToMerge.add(resourceToPick); 
                } else {
                    resourcesToMerge.add(new NonExistingResource(resolver, overlay.getPath() + "/" + relativePath));
                }
                return resourcesToMerge;
            }
        }
    }

    @Before
    public void setup() throws LoginException, PersistenceException {
        final ResourceResolverFactory factory = new MockResourceResolverFactory();
        this.resolver = factory.getResourceResolver(null);
        this.ctx = new BasicResolveContext<Void>(resolver);
        MockHelper.create(this.resolver)
        .resource("/apps").resource("base")
        .resource("/apps/overlay").commit();
        
        base = this.resolver.getResource("/apps/base");
        overlay = this.resolver.getResource("/apps/overlay");
        
        this.provider = new CRUDMergingResourceProvider("/merged", new SimpleMergedResourcePicker(), false);
    }

    @Test
    public void testHideChildren() throws PersistenceException {
        // create new child nodes below base and overlay
        MockHelper.create(this.resolver)
            .resource("/apps/base/child1").p("property1", "frombase")
            .resource("/apps/base/child2").p("property1", "frombase")
            .resource("/apps/overlay/child1").p("property1", "fromoverlay")
            .resource("/apps/overlay/child3").p("property1", "fromoverlay")
            .commit();
        
        ModifiableValueMap properties = overlay.adaptTo(ModifiableValueMap.class);
        properties.put(MergedResourceConstants.PN_HIDE_CHILDREN, "*");
        resolver.commit();
        
        Resource mergedResource = this.provider.getResource(ctx, "/merged", ResourceContext.EMPTY_CONTEXT, null);
        
        // convert the iterator returned by list children into an iterable (to be able to perform some tests)
        IteratorIterable<Resource> iterable = new IteratorIterable<Resource>(provider.listChildren(ctx, mergedResource), true);
        
        // all overlay resource are still exposed, because hiding children by wildcard only hides children from underlying resources
        Assert.assertThat(iterable, Matchers.containsInAnyOrder(
                ResourceMatchers.withNameAndProps("child1", Collections.singletonMap("property1", (Object)"fromoverlay")),
                ResourceMatchers.withNameAndProps("child3", Collections.singletonMap("property1", (Object)"fromoverlay"))
        ));
        
        // now hide by explicit value
        properties.put(MergedResourceConstants.PN_HIDE_CHILDREN, "child1");
        resolver.commit();
        
        // child1 is no longer exposed from overlay, because hiding children by name hides children from underlying as well as from local resources, child2 is exposed from base
        iterable = new IteratorIterable<Resource>(provider.listChildren(ctx, mergedResource), true);
        Assert.assertThat(iterable, Matchers.containsInAnyOrder(
                ResourceMatchers.withName("child2"),
                ResourceMatchers.withName("child3")));
        
        // now hide by negated value (hide all underlying children except for the one with name child2)
        properties.put(MergedResourceConstants.PN_HIDE_CHILDREN, new String[]{"!child2", "*", "child3"});
        resolver.commit();
        
        iterable = new IteratorIterable<Resource>(provider.listChildren(ctx, mergedResource), true);
        Assert.assertThat(iterable, Matchers.containsInAnyOrder(
                ResourceMatchers.withName("child2"), 
                ResourceMatchers.withNameAndProps("child1", Collections.singletonMap("property1", (Object)"fromoverlay"))
        ));
    }

    @Test
    public void testHideChildrenWithResourceNamesStartingWithExclamationMark() throws PersistenceException {
        // create new child nodes below base and overlay
        MockHelper.create(this.resolver)
            .resource("/apps/base/!child1").p("property1", "frombase")
            .resource("/apps/overlay/!child1").p("property1", "fromoverlay")
            .resource("/apps/overlay/!child3").p("property1", "fromoverlay")
            .commit();
        
        ModifiableValueMap properties = overlay.adaptTo(ModifiableValueMap.class);
        properties.put(MergedResourceConstants.PN_HIDE_CHILDREN, "!!child3"); // escape the resource name with another exclamation mark
        resolver.commit();
        
        Resource mergedResource = this.provider.getResource(ctx, "/merged", ResourceContext.EMPTY_CONTEXT, null);
        
        // convert the iterator returned by list children into an iterable (to be able to perform some tests)
        IteratorIterable<Resource> iterable = new IteratorIterable<Resource>(provider.listChildren(ctx, mergedResource), true);
        
        // the resource named "!child3" should be hidden
        Assert.assertThat(iterable, Matchers.contains(ResourceMatchers.withNameAndProps("!child1", Collections.singletonMap("property1", (Object)"fromoverlay"))));
    }

    @Test
    public void testHideChildrenBeingSetOnParent() throws PersistenceException {
        // create new child nodes below base and overlay
        MockHelper.create(this.resolver)
            .resource("/apps/base/child").p("property1", "frombase").resource("grandchild").p("property1", "frombase")
            .resource("/apps/base/child2").p("property1", "frombase")
            .resource("/apps/overlay/child").p("property1", "fromoverlay")
            .commit();
        
        ModifiableValueMap properties = overlay.adaptTo(ModifiableValueMap.class);
        properties.put(MergedResourceConstants.PN_HIDE_CHILDREN, "*");
        resolver.commit();
        
        Resource mergedResource = this.provider.getResource(ctx, "/merged", ResourceContext.EMPTY_CONTEXT, null);
        
        // the child was hidden on the parent (but only for the underlying resource), the local child from the overlay is still exposed
        Assert.assertThat(provider.getResource(ctx, "/merged/child", ResourceContext.EMPTY_CONTEXT, mergedResource), ResourceMatchers.withNameAndProps("child", Collections.singletonMap("property1", (Object)"fromoverlay")));
    }

    @Test
    public void testHideProperties() {
        ModifiableValueMap properties = base.adaptTo(ModifiableValueMap.class);
        properties.put("property1", "frombase");
        properties.put("property2", "frombase");
        properties.put(MergedResourceConstants.PN_HIDE_CHILDREN, "some invalid resource");
        
        // hide with wildcard
        ModifiableValueMap overlayProperties = overlay.adaptTo(ModifiableValueMap.class);
        overlayProperties.put(MergedResourceConstants.PN_HIDE_PROPERTIES, "*");
        Map<String, Object> expectedProperties = new HashMap<String, Object>();
        expectedProperties.put("property1", "fromoverlay");
        expectedProperties.put("property3", "fromoverlay");
        overlayProperties.putAll(expectedProperties);
        this.provider = new CRUDMergingResourceProvider("/merged", new SimpleMergedResourcePicker(), false);
        
        Resource mergedResource = this.provider.getResource(ctx, "/merged", ResourceContext.EMPTY_CONTEXT, null);
        // property1 is still exposed from overlay, because hiding properties by wildcard only hides children from underlying resources
        Assert.assertThat(mergedResource, ResourceMatchers.withProps(expectedProperties));
        // all properties from underlying resource are hidden!
        Assert.assertThat(mergedResource, Matchers.not(ResourceMatchers.withProps(properties)));
        // make sure no special properties are exposed
        Assert.assertFalse(mergedResource.getValueMap().containsKey(MergedResourceConstants.PN_HIDE_CHILDREN));
        Assert.assertFalse(mergedResource.getValueMap().containsKey(MergedResourceConstants.PN_HIDE_PROPERTIES));
        
        // hide by value
        overlayProperties.put(MergedResourceConstants.PN_HIDE_PROPERTIES, new String[]{"property1"});
        mergedResource = this.provider.getResource(ctx, "/merged", ResourceContext.EMPTY_CONTEXT, null);
        expectedProperties.put("property2", "frombase");
        expectedProperties.remove("property1");
        // property2 and property 3 are still exposed
        Assert.assertThat(mergedResource, ResourceMatchers.withProps(expectedProperties));
        // property1 is no longer exposed from overlay nor base, because hiding properties by name also hides local properties
        Assert.assertThat(mergedResource, Matchers.not(ResourceMatchers.withProps(Collections.singletonMap("property1", (Object)"fromoverlay"))));
        
        // make sure no special properties are exposed
        Assert.assertFalse(mergedResource.getValueMap().containsKey(MergedResourceConstants.PN_HIDE_CHILDREN));
        Assert.assertFalse(mergedResource.getValueMap().containsKey(MergedResourceConstants.PN_HIDE_PROPERTIES));
    }

    @Test
    public void testOrderOfPartiallyOverwrittenChildren() throws PersistenceException {
        // see https://issues.apache.org/jira/browse/SLING-4915
        
        // create new child nodes below base and overlay
        MockHelper.create(this.resolver)
            .resource("/apps/base/child1")
            .resource("/apps/base/child2")
            .resource("/apps/base/child3")
            .resource("/apps/overlay/child4")
            .resource("/apps/overlay/child2")
            .resource("/apps/overlay/child3")
            .commit();
        
        Resource mergedResource = this.provider.getResource(ctx, "/merged", ResourceContext.EMPTY_CONTEXT, null);
        // convert the iterator returned by list children into an iterable (to be able to perform some tests)
        IteratorIterable<Resource> iterable = new IteratorIterable<Resource>(provider.listChildren(ctx, mergedResource), true);
        
        Assert.assertThat(iterable, Matchers.contains(ResourceMatchers.withName("child1"),ResourceMatchers.withName("child4"), ResourceMatchers.withName("child2"), ResourceMatchers.withName("child3")));
    }

    @Test
    public void testOrderOfNonOverlappingChildren() throws PersistenceException {
     // create new child nodes below base and overlay
        MockHelper.create(this.resolver)
            .resource("/apps/base/child1")
            .resource("/apps/base/child2")
            .resource("/apps/overlay/child3")
            .resource("/apps/overlay/child4")
            .commit();
        Resource mergedResource = this.provider.getResource(ctx, "/merged", ResourceContext.EMPTY_CONTEXT, null);
        // convert the iterator returned by list children into an iterable (to be able to perform some tests)
        IteratorIterable<Resource> iterable = new IteratorIterable<Resource>(provider.listChildren(ctx, mergedResource), true);
        
        Assert.assertThat(iterable, Matchers.contains(ResourceMatchers.withName("child1"),ResourceMatchers.withName("child2"), ResourceMatchers.withName("child3"), ResourceMatchers.withName("child4")));
    }

    @Test
    public void testOrderBeingModifiedThroughOrderBefore() throws PersistenceException {
     // create new child nodes below base and overlay
        MockHelper.create(this.resolver)
            .resource("/apps/base/child1")
            .resource("/apps/base/child2")
            .resource("/apps/overlay/child3").p(MergedResourceConstants.PN_ORDER_BEFORE, "child2")
            .commit();
        Resource mergedResource = this.provider.getResource(ctx, "/merged", ResourceContext.EMPTY_CONTEXT, null);
        // convert the iterator returned by list children into an iterable (to be able to perform some tests)
        IteratorIterable<Resource> iterable = new IteratorIterable<Resource>(provider.listChildren(ctx, mergedResource), true);
        
        Assert.assertThat(iterable, Matchers.contains(ResourceMatchers.withName("child1"),ResourceMatchers.withName("child3"), ResourceMatchers.withName("child2")));
    }
}
