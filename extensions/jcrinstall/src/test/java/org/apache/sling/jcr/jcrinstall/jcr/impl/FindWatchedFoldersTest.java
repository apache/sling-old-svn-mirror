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
package org.apache.sling.jcr.jcrinstall.jcr.impl;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.sling.commons.testing.jcr.RepositoryTestBase;
import org.apache.sling.jcr.api.SlingRepository;

/** Test the "find watched folders" feature of the RepositoryObserver */
public class FindWatchedFoldersTest extends RepositoryTestBase {
    
    SlingRepository repo;
    Session session;
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        
        session.getRootNode().getNode("libs").remove();
        session.getRootNode().getNode("apps").remove();
        session.logout();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        repo = getRepository();
        session = repo.loginAdministrative(repo.getDefaultWorkspace());
    }

    @SuppressWarnings("unchecked")
    public void testInitialFind() throws Exception {
        final String [] folders = {
                "libs/foo/bar/install",
                "libs/foo/wii/install",
                "apps/install"
        };
        final String [] foldersToIgnore = {
                "libs/foo/bar/installed",
                "apps/noninstall"
        };
        
        for(String folder : folders) {
            createFolder(folder);
        }
        for(String folder : foldersToIgnore) {
            createFolder(folder);
        }
        
        final FolderNameFilter filter = new FolderNameFilter(".*/install$");
        final RepositoryObserver ro = createRepositoryObserver(filter);
        ro.activate(null);
        
        final Field f = ro.getClass().getDeclaredField("folders");
        f.setAccessible(true);
        final Set<WatchedFolder> wfSet = (Set<WatchedFolder>)f.get(ro);
        assertEquals("activate() must find all watched folders", folders.length, wfSet.size());
        
        final Set<String> paths = new HashSet<String>();
        for(WatchedFolder wf : wfSet) {
            paths.add(wf.getPath());
        }
        for(String folder : folders) {
            assertTrue("Folder " + folder + " must be watched (watched=" + paths + ")", 
                    paths.contains("/" + folder)); 
        }
    }
    
    void createFolder(String path) throws Exception {
        final String [] parts = path.split("/");
        Node n = session.getRootNode();
        for(String part : parts) {
            if(n.hasNode(part)) {
                n = n.getNode(part);
            } else {
                n = n.addNode(part);
            }
        }
        session.save();
    }
    
    RepositoryObserver createRepositoryObserver(FolderNameFilter filter) throws Exception {
        final RepositoryObserver result = new RepositoryObserver();
        setField(result, "repository", repo);
        setField(result, "folderNameFilter", filter);
        return result;
    }
    
    static void setField(Object target, String name, Object value) throws Exception, IllegalAccessException {
        final Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);

    }
}
