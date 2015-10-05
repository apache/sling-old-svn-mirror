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
package org.apache.sling.jcr.contentloader.internal;

import junitx.util.PrivateAccessor;
import org.apache.sling.commons.testing.jcr.RepositoryUtil;
import org.apache.sling.testing.mock.osgi.MockBundle;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;

import javax.jcr.*;
import java.util.*;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;
import static org.apache.sling.jcr.contentloader.internal.ContentLoaderService.*;

public class ContentLoaderServiceTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ContentLoaderService underTest;
    private BundleContentLoader contentLoader;
    private Session session;

    @Before
    public void setup() throws Exception {
        // whiteboard which holds readers
        context.registerInjectActivateService(new ContentReaderWhiteboard());

        // TODO - SlingRepository should be registered out of the box, not after calling context.resourceResolver()
        // TODO - sling node types should _always_ be registered
        session = context.resourceResolver().adaptTo(Session.class);
        RepositoryUtil.registerSlingNodeTypes(session);

        // register the content loader service
        underTest = context.registerInjectActivateService(new ContentLoaderService());
        contentLoader = (BundleContentLoader) PrivateAccessor.getField(underTest, "bundleContentLoader");
    }

    //-------ContentLoaderService#bundleChanged(BundleEvent)-------//
    //I'm not very sure how to test this method, it looks like side effect of this method goes very deep
    //And more affects BundleContentLoader than ContentLoaderService

    @Test
    public void testBundleResolvedBundleChanged() throws NoSuchFieldException, RepositoryException {
        final Bundle bundle = createNewBundle();
        final List<Bundle> delayedBundles = (List<Bundle>) PrivateAccessor.getField(contentLoader, "delayedBundles");
        final Set<String> updatedBundles = (Set<String>) PrivateAccessor.getField(underTest, "updatedBundles");

        updatedBundles.add(bundle.getSymbolicName());
        int updatedBundlesCurrentAmout = updatedBundles.size();
        underTest.bundleChanged(new BundleEvent(BundleEvent.RESOLVED, bundle));
        //Below we check that this bundle was removed from updatedBundles set
        assertEquals(updatedBundlesCurrentAmout-1, delayedBundles.size());

        updatedBundlesCurrentAmout = updatedBundles.size();
        underTest.bundleChanged(new BundleEvent(BundleEvent.UPDATED, bundle));
        assertEquals(updatedBundlesCurrentAmout+1, updatedBundles.size());

        //Is it ok just to call this method to check that no exception occurs?
        underTest.bundleChanged(new BundleEvent(BundleEvent.UNINSTALLED, bundle));
    }

    //-------ContentLoaderService#bundleChanged(BundleEvent)-------//

    @Test
    public void getContentInfoFromLockedNode() throws RepositoryException {
        final Bundle bundle = createNewBundle();
        final Node bcNode = (Node)session.getItem(ContentLoaderService.BUNDLE_CONTENT_NODE);
        bcNode.addNode(bundle.getSymbolicName()).addMixin("mix:lockable");
        session.save();
        bcNode.getNode(bundle.getSymbolicName()).lock(false, true);

        assertNull(underTest.getBundleContentInfo(session, bundle, false));
    }

    @Test
    public void getContentInfoFromNotLockableNode() throws RepositoryException {
        final Bundle bundle = createNewBundle();
        final Node bcNode = (Node)session.getItem(ContentLoaderService.BUNDLE_CONTENT_NODE);
        bcNode.addNode(bundle.getSymbolicName()); //Node without lockable mixin
        session.save();

        assertNull(underTest.getBundleContentInfo(session, bundle, false));
    }

    @Test
    public void getContentInfo() throws RepositoryException {
        final Bundle bundle = createNewBundle();
        final Node bcNode = (Node)session.getItem(ContentLoaderService.BUNDLE_CONTENT_NODE);
        final Node bundleContent = bcNode.addNode(bundle.getSymbolicName());
        bundleContent.addMixin("mix:lockable");


        Calendar now = Calendar.getInstance();
        bundleContent.setProperty(PROPERTY_CONTENT_LOADED_AT, now);

        final Boolean isLoaded = true;
        bundleContent.setProperty(PROPERTY_CONTENT_LOADED, isLoaded);

        final String[] paths = {"foo", "bar"};
        bundleContent.setProperty(PROPERTY_UNINSTALL_PATHS, paths);
        session.save();

        Map<String, Object> props = underTest.getBundleContentInfo(session, bundle, false);
        assertThat("ContentInfo should be provided", props, notNullValue());
        assertEquals(isLoaded, props.get(PROPERTY_CONTENT_LOADED));
        assertTrue(props.containsKey(PROPERTY_CONTENT_LOADED_AT));
        assertTrue(props.containsKey(PROPERTY_UNINSTALL_PATHS));
    }

    //-------ContentLoaderService#contentIsUninstalled(Session, Bundle)-------//

    @Test
    public void testContentIsUninstalled() throws RepositoryException {
        final Bundle bundle = createNewBundle();
        final Node bcNode = session.getNode(BUNDLE_CONTENT_NODE).addNode(bundle.getSymbolicName());

        underTest.contentIsUninstalled(session, bundle);

        assertTrue(bcNode.hasProperty("content-unloaded-by"));
        assertTrue(bcNode.hasProperty("content-unload-time"));
        assertFalse(bcNode.hasProperty(PROPERTY_UNINSTALL_PATHS));
        assertFalse(bcNode.getProperty(PROPERTY_CONTENT_LOADED).getBoolean());
    }

    //-------ContentLoaderService#getMimeType(String)-------//

    @Test
    public void testMimeTypeService(){
        assertEquals("audio/mpeg", underTest.getMimeType("test.mp3"));
    }

    //-------ContentLoaderService#getMimeType(String)-------//

    @Test
    public void getSessionForWorkspace() throws RepositoryException {
        assertNotNull(underTest.getSession(null));
    }

    private Bundle createNewBundle(){
        MockBundle b = new MockBundle(context.bundleContext());
        b.setSymbolicName(uniqueId());
        return b;
    }

    private final String uniqueId() {
        return getClass().getSimpleName() + UUID.randomUUID();
    }
}
