/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.resourceresolver.impl.observation;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

public class ResourceChangeListenerInfoTest {

    @Test
    public void testGetExpandedRelativePaths() {
        ServiceReference reference = mock(ServiceReference.class);
        when(reference.getProperty(ResourceChangeListener.PATHS)).thenReturn(new String[] {"project/components/page/page.html"});
        final ResourceChangeListenerInfo rcli = new ResourceChangeListenerInfo(reference, new String[] {"/apps/", "/libs/"});
        Set<String> paths = rcli.getPaths().toStringSet();
        assertTrue("PathSet " + paths.toString() + " does not contain /apps/project/components/page/page.html.",
                paths.contains("/apps/project/components/page/page.html"));
        assertTrue("PathSet " + paths.toString() + " does not contain /libs/project/components/page/page.html.",
                paths.contains("/libs/project/components/page/page.html"));
    }

    @Test
    public void testDotPathConfig() {
        ServiceReference reference = mock(ServiceReference.class);
        when(reference.getProperty(ResourceChangeListener.PATHS)).thenReturn(new String[] {"."});
        final ResourceChangeListenerInfo rcli = new ResourceChangeListenerInfo(reference, new String[] {"/apps/", "/libs/"});
        Set<String> paths = rcli.getPaths().toStringSet();
        assertTrue("PathSet " + paths.toString() + " does not contain /apps/", paths.contains("/apps"));
        assertTrue("PathSet " + paths.toString() + " does not contain /libs/.", paths.contains("/libs"));
    }

    @Test
    public void testGlobPatternExpansion() {
        ServiceReference reference = mock(ServiceReference.class);
        when(reference.getProperty(ResourceChangeListener.PATHS)).thenReturn(new String[] {"glob:./**/*.html"});
        final ResourceChangeListenerInfo rcli = new ResourceChangeListenerInfo(reference, new String[] {"/apps/", "/libs/"});
        Set<String> paths = rcli.getPaths().toStringSet();
        assertTrue("PathSet " + paths.toString() + " does not contain glob:/apps/**/*.html.", paths.contains("glob:/apps/**/*.html"));
        assertTrue("PathSet " + paths.toString() + " does not contain glob:/libs/**/*.html.", paths.contains("glob:/libs/**/*.html"));
    }
}
