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

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Storage for the OSGi controller, stores Maps for the resources
 *  managed by the controller.
 */
class Storage {
    private final File dataFile;
    private final Map<String, Map<String, Object>> data;
    
    private final Logger log = LoggerFactory.getLogger(this.getClass());
    
    /** Create Storage that uses dataFile for persistence, and
     *  read the current status from that file */
    @SuppressWarnings("unchecked")
    Storage(File dataFile) throws IOException {
        this.dataFile = dataFile;
        ObjectInputStream ois = null;
        Map<String, Map<String, Object>> loadedData = null;
        Throwable loadException = null;
        
        try {
            ois = new ObjectInputStream(new FileInputStream(dataFile));
            loadedData = (Map<String, Map<String, Object>>)ois.readObject();
        } catch(EOFException eof) {
            loadException = eof;
        } catch(ClassNotFoundException cnfe) {
            loadException = cnfe;
        } finally {
            if(ois != null) {
                ois.close();
            }
        }
        
        if(loadException != null) {
            log.debug("Unable to retrieve data from data file, will use empty data", loadException);
            loadedData = new HashMap<String, Map<String, Object>>();
        }
        
        data = loadedData;
    }
    
    /** Persist our data to our data file */
    protected void saveToFile() throws IOException {
        ObjectOutputStream oos = null;
        try {
            synchronized(data) {
                oos = new ObjectOutputStream(new FileOutputStream(dataFile));
                oos.writeObject(data);
            }
        } finally {
            if(oos != null) {
                oos.flush();
                oos.close();
            }
        }
    }
    
    /** True if our data contains give key */
    boolean contains(String key) {
        return data.containsKey(key);
    }
    
    /** Get the data map for given key.
     *  If we don't have it yet, and empty map is
     *  created, but saveData is not called.  
     */
    Map<String, Object>getMap(String key) {
        Map<String, Object> result = data.get(key);
        synchronized(data) {
            if(result == null) {
                result = new HashMap<String, Object>();
                data.put(key, result);
            }
        }
        return result;
    }
    
    /** Remove given key from our storage */
    void remove(String key) {
        synchronized(data) {
            data.remove(key);
        }
    }
    
    /** Get the Set of of keys in our data map */
    Set<String> getKeys() {
        return data.keySet();
    }
}
