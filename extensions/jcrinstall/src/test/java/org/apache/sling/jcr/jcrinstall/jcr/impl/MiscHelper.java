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
import java.util.Set;

import org.apache.sling.jcr.api.SlingRepository;
import org.apache.sling.jcr.jcrinstall.osgi.OsgiController;

/** Miscellaneous test helper functions */
class MiscHelper {
    static boolean folderIsWatched(RepositoryObserver ro, String path) throws Exception {
        boolean result = false;
        final Set<WatchedFolder> wfSet = getWatchedFolders(ro);
        for(WatchedFolder wf : wfSet) {
            if(wf.getPath().equals("/" + path)) {
                result = true;
                break;
            }
        }
        return result;
    }
    
    @SuppressWarnings("unchecked")
    static Set<WatchedFolder> getWatchedFolders(RepositoryObserver ro) throws Exception {
        final Field f = ro.getClass().getDeclaredField("folders");
        f.setAccessible(true);
        return (Set<WatchedFolder>)f.get(ro);
    }
    
    static RepositoryObserver createRepositoryObserver(SlingRepository repo, OsgiController c) throws Exception {
        final RepositoryObserver result = new RepositoryObserver();
        setField(result, "repository", repo);
        setField(result, "osgiController", c);
        return result;
    }
    
    static void setField(Object target, String name, Object value) throws Exception, IllegalAccessException {
        final Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);

    }

}
