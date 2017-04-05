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
package org.apache.sling.resourceresolver.impl.observation;

import static org.apache.sling.api.resource.observation.ResourceChangeListener.PATHS;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.sling.api.resource.observation.ResourceChange;
import org.apache.sling.api.resource.observation.ResourceChange.ChangeType;
import org.apache.sling.api.resource.observation.ResourceChangeListener;
import org.apache.sling.api.resource.path.Path;
import org.apache.sling.api.resource.path.PathSet;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.osgi.framework.ServiceReference;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class BasicObservationReporterTest {
    
    private static final String[] SEARCH_PATHS = new String[] { "/apps", "/libs" };
    
    @Test
    public void testRootProvider() {
        ResourceChangeListenerInfo allPathListener = resourceChangeListenerInfo("/");
        ResourceChangeListenerInfo appsPathListener = resourceChangeListenerInfo("/apps");
        ResourceChangeListenerInfo appsApp2PathListener = resourceChangeListenerInfo("/apps/app2");
        ResourceChangeListenerInfo globListener = resourceChangeListenerInfo("glob:/apps/**/*.html");
        
        BasicObservationReporter underTest = new BasicObservationReporter(SEARCH_PATHS,
                ImmutableList.of(allPathListener, appsPathListener, appsApp2PathListener, globListener),
                new Path("/"), PathSet.EMPTY_SET);
        
        underTest.reportChanges(changes("/apps/app1/path1.html"), false);
        underTest.reportChanges(changes("/content/path2/jcr:content"), false);
        
        assertListener(allPathListener, "/apps/app1/path1.html", "/content/path2/jcr:content");
        assertListener(appsPathListener, "/apps/app1/path1.html");
        assertListener(appsApp2PathListener);
        assertListener(globListener, "/apps/app1/path1.html");
    }

    @Test
    public void testSpecificPathProvider() {
        ResourceChangeListenerInfo allPathListener = resourceChangeListenerInfo("/");
        ResourceChangeListenerInfo appsPathListener = resourceChangeListenerInfo("/apps");
        ResourceChangeListenerInfo appsApp2PathListener = resourceChangeListenerInfo("/apps/app2");
        ResourceChangeListenerInfo globListener = resourceChangeListenerInfo("glob:/apps/**/*.html");
        
        BasicObservationReporter underTest = new BasicObservationReporter(SEARCH_PATHS,
                ImmutableList.of(allPathListener, appsPathListener, appsApp2PathListener, globListener),
                new Path("/apps/app1"), PathSet.EMPTY_SET);
        
        underTest.reportChanges(changes("/apps/app1/path1.html"), false);
        
        assertListener(allPathListener, "/apps/app1/path1.html");
        assertListener(appsPathListener, "/apps/app1/path1.html");
        assertListener(appsApp2PathListener);
        assertListener(globListener, "/apps/app1/path1.html");
    }

    @SuppressWarnings("unchecked")
    private static ResourceChangeListenerInfo resourceChangeListenerInfo(String... paths) {
        ServiceReference<ResourceChangeListener> ref = mock(ServiceReference.class);
        when(ref.getProperty(PATHS)).thenReturn(paths);
        ResourceChangeListenerInfo info = new ResourceChangeListenerInfo(ref, SEARCH_PATHS);
        info.setListener(mock(ResourceChangeListener.class));
        return info;
    }
    
    private static Iterable<ResourceChange> changes(String... paths) {
        List<ResourceChange> changes = new ArrayList<>();
        for (String path : paths) {
            changes.add(new ResourceChange(ChangeType.ADDED, path, false));
        }
        return changes;
    }
    
    @SuppressWarnings("unchecked")
    private static void assertListener(ResourceChangeListenerInfo info, String... paths) {
        Set<String> expectedPaths = ImmutableSet.copyOf(paths);
        ArgumentCaptor<List<ResourceChange>> argument = (ArgumentCaptor)ArgumentCaptor.forClass(List.class);
        if (paths.length == 0) {
            verifyNoMoreInteractions(info.getListener());
        }
        else {
            verify(info.getListener(), atLeastOnce()).onChange(argument.capture());
            Set<String> actualPaths = new HashSet<>();
            for (List<ResourceChange> changeList : argument.getAllValues()) {
                for (ResourceChange change : changeList) {
                    actualPaths.add(change.getPath());
                }
            }
            assertEquals(expectedPaths, actualPaths);
        }
    }

}
