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

import static java.util.Collections.singletonMap;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import javax.jcr.Session;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.testing.jcr.RepositoryUtil;
import org.apache.sling.jcr.contentloader.internal.readers.JsonReader;
import org.apache.sling.jcr.contentloader.internal.readers.XmlReader;
import org.apache.sling.jcr.contentloader.internal.readers.ZipReader;
import org.apache.sling.testing.mock.osgi.MockBundle;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.osgi.framework.Bundle;

public class BundleContentLoaderTest {

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    private BundleContentLoader contentLoader;

    @Before
    public void prepareContentLoader() throws Exception {
        // prepare content readers
        context.registerInjectActivateService(new JsonReader());
        context.registerInjectActivateService(new XmlReader());
        context.registerInjectActivateService(new ZipReader());
        
        // whiteboard which holds readers
        context.registerInjectActivateService(new ContentReaderWhiteboard());
        
        // TODO - SlingRepository should be registered out of the box, not after calling context.resourceResolver()
        // TODO - sling node types should _always_ be registered
        Session session = context.resourceResolver().adaptTo(Session.class);
        RepositoryUtil.registerSlingNodeTypes(session);
        
        // register the content loader service
        BundleHelper bundleHelper = context.registerInjectActivateService(new ContentLoaderService());
        
        ContentReaderWhiteboard whiteboard = context.getService(ContentReaderWhiteboard.class);
        
        contentLoader = new BundleContentLoader(bundleHelper, whiteboard);        
    }
    
    
    @Test
    public void loadContentWithSpecificPath() throws Exception {

        Bundle mockBundle = newBundleWithInitialContent("SLING-INF/libs/app;path:=/libs/app");
        
        contentLoader.registerBundle(context.resourceResolver().adaptTo(Session.class), mockBundle, false);
        
        Resource imported = context.resourceResolver().getResource("/libs/app");
        
        assertThat("Resource was not imported", imported, notNullValue());
        assertThat("sling:resourceType was not properly set", imported.getResourceType(), equalTo("sling:Folder"));
    }

    @Test
    public void loadContentWithRootPath() throws Exception {
        
        Bundle mockBundle = newBundleWithInitialContent("SLING-INF/");
        
        contentLoader.registerBundle(context.resourceResolver().adaptTo(Session.class), mockBundle, false);
        
        Resource imported = context.resourceResolver().getResource("/libs/app");
        
        assertThat("Resource was not imported", imported, notNullValue());
        assertThat("sling:resourceType was not properly set", imported.getResourceType(), equalTo("sling:Folder"));
    }
    
    @Test
    @Ignore("TODO - unregister or somehow ignore the XmlReader component for this test")
    public void loadXmlAsIs() throws Exception {

        dumpRepo("/", 2);
        
        Bundle mockBundle = newBundleWithInitialContent("SLING-INF/libs/app;path:=/libs/app;ignoreImportProviders:=xml");
        
        contentLoader.registerBundle(context.resourceResolver().adaptTo(Session.class), mockBundle, false);
        
        Resource imported = context.resourceResolver().getResource("/libs/app");
        
        assertThat("Resource was not imported", imported, notNullValue());
        assertThat("sling:resourceType was not properly set", imported.getResourceType(), equalTo("sling:Folder"));
        
        Resource xmlFile = context.resourceResolver().getResource("/libs/app.xml");
        
        dumpRepo("/", 2);
        
        assertThat("XML file was was not imported", xmlFile, notNullValue());

    }

    private MockBundle newBundleWithInitialContent(String initialContentHeader) {
        
        MockBundle mockBundle = new MockBundle(context.bundleContext());
        mockBundle.setHeaders(singletonMap("Sling-Initial-Content", initialContentHeader));
        return mockBundle;
    }


    private void dumpRepo(String startPath, int maxDepth) {
        
        dumpRepo0(startPath, maxDepth, 0);
    }


    private void dumpRepo0(String startPath, int maxDepth, int currentDepth) {
        Resource resource = context.resourceResolver().getResource(startPath);
        StringBuilder format = new StringBuilder();
        for ( int i = 0 ;i  < currentDepth ; i++) {
            format.append("  ");
        }
        format.append("%s [%s]%n");
        String name = resource.getName().length() == 0  ? "/" : resource.getName();
        System.out.format(format.toString(), name, resource.getResourceType());
        currentDepth++;
        if ( currentDepth > maxDepth) {
            return;
        }
        for ( Resource child : resource.getChildren() ) {
            dumpRepo0(child.getPath(), maxDepth, currentDepth);
        }
    }
}
