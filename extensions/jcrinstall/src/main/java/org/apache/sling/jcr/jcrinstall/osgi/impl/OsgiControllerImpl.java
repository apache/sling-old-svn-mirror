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
package org.apache.sling.jcr.jcrinstall.osgi.impl;

import static org.apache.sling.jcr.jcrinstall.osgi.InstallResultCode.IGNORED;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.sling.jcr.jcrinstall.osgi.JcrInstallException;
import org.apache.sling.jcr.jcrinstall.osgi.OsgiController;
import org.apache.sling.jcr.jcrinstall.osgi.OsgiResourceProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsgiControllerImpl implements OsgiController {

    private Storage storage;
    private List<OsgiResourceProcessor> processors;
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    /** Storage key: last modified as a Long */
    public static final String KEY_LAST_MODIFIED = "last.modified";
    
    /** Default value for getLastModified() */
    public static final long LAST_MODIFIED_NOT_FOUND = -1;
    
    public int installOrUpdate(String uri, long lastModified, InputStream data) throws IOException, JcrInstallException {
        int result = IGNORED;
        final OsgiResourceProcessor p = getProcessor(uri);
        if(p != null) {
            try {
                final Map<String, Object> map = storage.getMap(uri);
                result = p.installOrUpdate(uri, map, data);
                if(result != IGNORED) {
                    map.put(KEY_LAST_MODIFIED, new Long(lastModified));
                }
                storage.saveToFile();
            } catch(IOException ioe) {
                throw ioe;
            } catch(Exception e) {
                throw new JcrInstallException("Exception in installOrUpdate", e);
            }
        }
        return result;
    }
    
    public void uninstall(String uri) throws JcrInstallException {
        final OsgiResourceProcessor p = getProcessor(uri);
        if(p != null) {
            try {
                p.uninstall(uri, storage.getMap(uri));
                storage.remove(uri);
                storage.saveToFile();
            } catch(Exception e) {
                throw new JcrInstallException("Exception in uninstall", e);
            }
        }
    }
    
    public Set<String> getInstalledUris() {
        return storage.getKeys();
    }

    /** {@inheritDoc}
     *  @return LAST_MODIFIED_NOT_FOUND if uri not found
     */
    public long getLastModified(String uri) {
        long result = LAST_MODIFIED_NOT_FOUND;
        
        if(storage.contains(uri)) {
            final Map<String, Object> uriData = storage.getMap(uri);
            final Long lastMod = (Long)uriData.get(KEY_LAST_MODIFIED); 
            if(lastMod != null) {
                result = lastMod.longValue();
            }
        }
        return result;
    }
    
    static String getResourceLocation(String uri) {
        return "jcrinstall://" + uri;
    }
    
    /** Return the first processor that accepts given uri, null if not found */
    OsgiResourceProcessor getProcessor(String uri) {
        OsgiResourceProcessor result = null;
        
        if(processors == null) {
            throw new IllegalStateException("Processors are not set");
        }
        
        for(OsgiResourceProcessor p : processors) {
            if(p.canProcess(uri)) {
                result = p;
                break;
            }
        }
        
        if(result == null) {
            log.debug("No processor found for resource {}", uri);
        }
        
        return result;
    }
}