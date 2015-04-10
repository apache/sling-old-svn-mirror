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
package org.apache.sling.commons.testing.osgi;

import java.util.Collections;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

public class MockServiceReference implements ServiceReference {

    private static long serviceIdCounter = 0;

    private Bundle bundle;

    private Dictionary<String, Object> props;

    public MockServiceReference(Bundle bundle) {
        this.bundle = bundle;
        this.props = new Hashtable<String, Object>();

        // mockup a service id
        props.put(Constants.SERVICE_ID, serviceIdCounter++);
    }

    public Bundle getBundle() {
        return bundle;
    }

    public void setProperty(String key, Object value) {
        props.put(key, value);
    }

    public Object getProperty(String key) {
        return props.get(key);
    }

    public String[] getPropertyKeys() {
        return Collections.list(props.keys()).toArray(new String[props.size()]);
    }

    public Bundle[] getUsingBundles() {
        return null;
    }

    public boolean isAssignableTo(Bundle bundle, String className) {
        return false;
    }

    public int compareTo(Object reference) {
        return 0;
    }

}