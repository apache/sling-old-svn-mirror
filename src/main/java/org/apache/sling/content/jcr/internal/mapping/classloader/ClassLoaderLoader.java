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
package org.apache.sling.content.jcr.internal.mapping.classloader;

/**
 * The <code>ClassLoaderLoader</code> TODO
 */
public class ClassLoaderLoader implements Loader {

    private ClassLoader classLoader;
    
    ClassLoaderLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
    
    /* (non-Javadoc)
     * @see com.day.sling.jcr.mapping.internal.classloader.Loader#loadClass(java.lang.String)
     */
    public Class loadClass(String name) throws ClassNotFoundException {
        return classLoader.loadClass(name);
    }

    public Object getLoader() {
        return classLoader;
    }
}
