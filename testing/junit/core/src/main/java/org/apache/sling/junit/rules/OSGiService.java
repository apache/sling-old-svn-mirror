/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.junit.rules;

/** Improved variant of the Service class that uses generics.
 * 
 *  The {#link TeleporterRule} also provides access to OSGi
 *  services for server-side tests, in a more integrated way. 
 */
public class OSGiService <T> extends Service {

    private final Class<T> clazz;
    
    private OSGiService(Class<T> clazz) {
        super(clazz);
        this.clazz = clazz;
    }
    
    public static <T> OSGiService<T> ofClass(Class<T> clazz) {
        return new OSGiService<T>(clazz);
    }
    
    public T get() {
        return (T)super.getService(clazz);
    }
}
