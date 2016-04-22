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

package org.apache.sling.testing.util;

import java.util.concurrent.atomic.AtomicLong;

/** Generate unique paths, for tests isolation */
public class UniquePaths {

    private static long startTime = System.currentTimeMillis();
    private static AtomicLong counter = new AtomicLong();
    public final static String SEP = "_";
    public final static String U_PATTERN = "_UNIQ_";
    
    /**
     * Return a unique path based on basePath
     * @param nameReference The simple class name of that object is used as part of the 
     *                      generated unique ID
     * @param basePath All occurrences of {@link UniquePaths#U_PATTERN} in basePath are replaced by the generated
     *                 unique ID. If $U$ is not found in basePath, unique ID is added at its end.
     * @return path with a unique value for each call.
     */
    public static String get(Object nameReference, String basePath) {
        if(basePath == null) {
            basePath = "";
        }
        
        final StringBuilder sb = new StringBuilder();
        sb.append(nameReference.getClass().getSimpleName());
        sb.append(SEP);
        sb.append(startTime);
        sb.append(SEP);
        sb.append(counter.incrementAndGet());
        
        if(basePath.contains(U_PATTERN)) {
            return basePath.replaceAll(U_PATTERN, sb.toString());
        } else {
            return basePath + sb.toString();
        }
    }
    
    /**
     * Get a unique ID with no base path
     *
     * @param nameReference The simple class name of that object is used as part of the
     *                      generated unique ID
     * @return path with a unique value for each call
     */
    public static String get(Object nameReference) {
        return get(nameReference, null);
    }
}
