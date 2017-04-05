/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.testing.clients.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ResourceUtil {

    /**
     * We must get the Resource as a stream from the ContextClassLoader and not from the normal classLoader
     * acquired by using getClass.getClassLoader, since we must be able to load resources from different threads
     * e.g. running in ant.
     *
     * @param resourcePath path to the resource
     * @return resource as InputStream
     */
    public static InputStream getResourceAsStream(String resourcePath) {
        return Thread.currentThread().getContextClassLoader().getClass().getResourceAsStream(resourcePath);
    }

    /**
     * Helper method to read a resource from class using {@link Class#getResourceAsStream(String)}
     * and convert into a String.
     *
     * @param resource The resource to read.
     * @return The requested resource as String, resolved using
     *         {@link Class#getResourceAsStream(String)}, or {@code null}
     *         if the requested resource cannot be resolved for some reason
     * @throws IOException if the Resource Stream cannot be read
     */
    public static String readResourceAsString(String resource) throws IOException {
        InputStream resourceAsStream = ResourceUtil.getResourceAsStream(resource);
        if (resourceAsStream != null) {
            StringBuilder sb = new StringBuilder();
            String line;
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(resourceAsStream, "UTF-8"));
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            } finally {
                resourceAsStream.close();
            }
            return sb.toString();
        }
        return null;
    }

}
