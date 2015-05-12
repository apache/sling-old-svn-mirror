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
package org.apache.sling.i18n.impl;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public class ResourceBundleEnumeration implements Enumeration<String> {

    private final Set<String> keys;
    private Enumeration<String> parentKeys;

    private Iterator<String> keysIterator;
    private String next;
    
    public ResourceBundleEnumeration(Set<String> keys, Enumeration<String> parentKeys) {
        this.keys = keys;
        this.parentKeys = parentKeys;
        this.keysIterator = keys.iterator();
        
        next = seek();
    }
    
    public boolean hasMoreElements() {
        return next != null;
    }

    public String nextElement() {
        if (!hasMoreElements()) {
            throw new NoSuchElementException();
        }
        
        String result = next;
        next = seek();
        return result;
    }

    private String seek() {
        if (keysIterator != null) {
            
            if (keysIterator.hasNext()) {
                return keysIterator.next();
            }
            
            // my keys are exhausted, set iterator to null
            keysIterator = null;
        }
        
        if (parentKeys != null) {
            while (parentKeys.hasMoreElements()) {
                String parentKey = parentKeys.nextElement();
                if (!keys.contains(parentKey)) {
                    return parentKey;
                }
            }
            
            // no more parent keys, set enumeration to null
            parentKeys = null;
        }
        
        // parentKeys are also exhausted, nothing more to return
        return null;
    }
}
