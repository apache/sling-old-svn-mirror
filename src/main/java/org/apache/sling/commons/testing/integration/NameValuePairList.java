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
package org.apache.sling.commons.testing.integration;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * A list of name-value pairs.
 */
public class NameValuePairList implements Iterable<NameValuePair> {

    private final List<NameValuePair> delegate;

    public NameValuePairList() {
        delegate = new ArrayList<NameValuePair>();
    }

    public NameValuePairList(List<NameValuePair> init) {
        delegate = new ArrayList<NameValuePair>(init);
    }

    public NameValuePairList(NameValuePairList clientNodeProperties) {
        this(clientNodeProperties.delegate);
    }

    public NameValuePairList(Map<String, String> clientNodeProperties) {
        this();
        if (clientNodeProperties != null) {
            for (Map.Entry<String, String> e : clientNodeProperties.entrySet()) {
                add(e.getKey(), e.getValue());
            }
        }
    }

    public void add(String name, String value) {
        delegate.add(new NameValuePair(name, value));
    }

    public void addIfNew(String name, String value) {
        boolean found = false;
        for (ListIterator<NameValuePair> li = delegate.listIterator(); li.hasNext();) {
            NameValuePair current = li.next();
            if (current.getName().equals(name)) {
                found = true;
                break;
            }
        }

        if (!found) {
            delegate.add(new NameValuePair(name, value));
        }

    }

    public void addOrReplace(String name, String value) {
        boolean replaced = false;
        for (ListIterator<NameValuePair> li = delegate.listIterator(); li.hasNext();) {
            NameValuePair current = li.next();
            if (current.getName().equals(name)) {
                if (!replaced) {
                    current.setValue(value);
                    replaced = true;
                } else {
                    li.remove();
                }
            }
        }

        if (!replaced) {
            delegate.add(new NameValuePair(name, value));
        }
    }

    public void addOrReplaceAll(NameValuePairList other) {
        for (NameValuePair nvp : other) {
            addOrReplace(nvp.getName(), nvp.getValue());
        }
    }

    public void clear() {
        delegate.clear();
    }

    public Iterator<NameValuePair> iterator() {
        return delegate.iterator();
    }

    public void prependIfNew(String name, String value) {
        boolean found = false;
        for (ListIterator<NameValuePair> li = delegate.listIterator(); li.hasNext();) {
            NameValuePair current = li.next();
            if (current.getName().equals(name)) {
                found = true;
                break;
            }
        }

        if (!found) {
            delegate.add(0, new NameValuePair(name, value));
        }

    }

    public int size() {
        return delegate.size();
    }
}
