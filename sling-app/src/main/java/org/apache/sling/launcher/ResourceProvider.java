/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.launcher;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;

/**
 * The <code>ResourceProvider</code> TODO
 */
public abstract class ResourceProvider {

    public abstract Iterator<String> getChildren(String path);
    
    public abstract URL getResource(String path);
    
    public InputStream getResourceAsStream(String path) {
        URL res = getResource(path);
        if (res != null) {
            try {
                return res.openStream();
            } catch (IOException ioe) {
                // ignore this one
            }
        }
        
        // no resource
        return null;
    }
}
