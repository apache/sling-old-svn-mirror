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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import org.apache.sling.jcr.jcrinstall.osgi.OsgiResourceProcessor;

class Utilities {
    
    static File getTestFile() throws IOException {
        final File result = File.createTempFile(Utilities.class.getName(), null);
        result.deleteOnExit();
        return result;
    }
    
    static void setProcessors(OsgiControllerImpl c, OsgiResourceProcessor ... processors) throws Exception {
        final List<OsgiResourceProcessor> list = new LinkedList<OsgiResourceProcessor>();
        for(OsgiResourceProcessor p : processors) {
            list.add(p);
        }
        setField(c, "processors", list);
    }
    
    static void setStorage(OsgiControllerImpl c, Storage s) throws Exception {
        final Field f = c.getClass().getDeclaredField("storage");
        f.setAccessible(true);
        f.set(c, s);
    }
    
    static void setField(Object o, String name, Object value) throws Exception {
        final Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(o, value);
    }

}
