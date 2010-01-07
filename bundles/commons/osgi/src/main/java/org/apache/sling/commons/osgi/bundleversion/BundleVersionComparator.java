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
package org.apache.sling.commons.osgi.bundleversion;

import java.util.Comparator;

import org.osgi.framework.Version;

/** Compares BundleVersionInfo */
public class BundleVersionComparator implements Comparator<BundleVersionInfo<?>>{

    private static final int A_GREATER = 1; 
    private static final int B_GREATER = -1;
    private static final int EQUAL = 0;
    
    public int compare(BundleVersionInfo<?> a, BundleVersionInfo<?> b) {
        
        // Handle null values
        if(a == null) {
            throw new IllegalArgumentException("a is null, cannot compare");
        }
        if(b == null) {
            throw new IllegalArgumentException("b is null, cannot compare");
        }
        
        // Handle non-bundles: we don't want them!
        if(!a.isBundle()) {
            throw new IllegalArgumentException("Not a bundle, cannot compare:" + a);
        }
        if(!b.isBundle()) {
            throw new IllegalArgumentException("Not a bundle, cannot compare:" + b);
        }
        
        // First compare symbolic names
        int result = a.getBundleSymbolicName().compareTo(b.getBundleSymbolicName());
        
        // Then compare versions
        if(result == EQUAL) {
            final Version va = a.getVersion();
            final Version vb = b.getVersion();
            if(va == null && vb == null) {
                // result = EQUAL
            } else if(vb == null) {
                result = A_GREATER;
            } else if(va == null) {
                result = B_GREATER;
            } else {
                result = va.compareTo(vb);
            }
            
            // more recent ones must come before older ones
            result = -result;
        }
        
        // Then, if snapshots, compare modification times, more recent comes first
        if(result == EQUAL && a.isSnapshot()) {
            final long ma = a.getBundleLastModified();
            final long mb = b.getBundleLastModified();
            if(ma > mb) {
                result = A_GREATER;
            } else if(mb > ma) {
                result = B_GREATER;
            }
            
            // more recent ones must come before older ones
            result = -result;
        }
        
        return result;
    }
}
