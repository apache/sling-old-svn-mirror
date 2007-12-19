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
package sun.misc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * The <code>Service</code> class is a primitive stub of the original
 * <code>sun.misc.Service</code> class used by the
 * <code>javax.script.ScriptEngineManager</code> to find script engine
 * factories in factory service files.
 * <p>
 * This stub is provided because the original class is not part of the official
 * Java API and may not be available on all platforms. In addition even if the
 * class would be available on the Java platform, it may not be visible inside
 * the OSGi framework. Finally, the <em>org.apache.sling.scripting.resolver</em>
 * bundle implements its own resolution of script engine factories and thus the
 * <code>Service</code> method is not used.
 */
public class Service {

    private static final String PREFIX = "META-INF/services/";

    /** Returns an empty iterator */
    public static <ProviderType> Iterator<ProviderType> providers(Class<ProviderType> type, ClassLoader loader) throws IOException {
        if (loader != null) {
            try {
                String name = PREFIX + type.getName();
                Enumeration<?> files = loader.getResources(name);
                return new NameIterator<ProviderType>(loader, files);
            } catch (IOException ignore) {
            }
        }

        return Collections.<ProviderType> emptyList().iterator();
    }

    private static class NameIterator<ProviderType> implements Iterator<ProviderType> {

        private final ClassLoader loader;

        private final Enumeration<?> files;

        private Iterator<String> currentFile;

        private ProviderType nextProvider;

        public NameIterator(ClassLoader loader, Enumeration<?> files) {
            this.loader = loader;
            this.files = files;
            seek();
        }

        public boolean hasNext() {
            return nextProvider != null;
        }

        public ProviderType next() {
            if (nextProvider == null) {
                throw new NoSuchElementException();
            }

            ProviderType result = nextProvider;
            seek();
            return result;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private void seek() {
            if (currentFile == null || !currentFile.hasNext()) {
                currentFile = getNames();
            }

            nextProvider = getClass(currentFile);
        }

        private Iterator<String> getNames() {
            while (files.hasMoreElements()) {
                URL fileUrl = (URL) files.nextElement();
                InputStream ins = null;
                try {
                    ArrayList<String> names = new ArrayList<String>();
                    ins = fileUrl.openStream();
                    BufferedReader br = new BufferedReader(new InputStreamReader(ins));
                    String name;
                    while ( (name = br.readLine()) != null) {
                        int hash = name.indexOf('#');
                        if (hash >= 0) {
                            name = name.substring(0, hash);
                        }
                        name = name.trim();

                        if (name.length() > 0) {
                            names.add(name);
                        }
                    }

                    return names.iterator();
                } catch (IOException ioe) {

                } finally {
                    if (ins != null) {
                        try {
                            ins.close();
                        } catch (IOException ignore) {
                        }
                    }
                }
            }

            // exhausted search
            return null;
        }

        @SuppressWarnings("unchecked")
        private ProviderType getClass(Iterator<String> currentFile) {
            if (currentFile != null && currentFile.hasNext()) {
                String name = currentFile.next();
                try {
                    Class<?> clazz = Class.forName(name, true, loader);
                    return (ProviderType) clazz.newInstance();
                } catch (Throwable t) {
                    //
                }
            }

            return null;
        }
    }
}
