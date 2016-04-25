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
package org.apache.sling.testing.rules.quickstart.util;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class ContextPathAllocator {

    private static Set<String> allocatedContextPaths;

    static {
        allocatedContextPaths = new HashSet<String>();
    }

    public String allocateContextPath() {
        while (true) {
            String contextPath = generateContextPath();

            boolean contextPathAdded = checkAndAdd(contextPath);

            if (contextPathAdded) {
                return "/" + contextPath;
            }
        }
    }

    public String generateContextPath() {
        int start = (int) 'a', end = (int) 'z';

        String contextPath = "";

        Random random = new Random();

        for (int i = 0; i < 8; i++) {
            int next = start + random.nextInt(end - start);
            contextPath += Character.toString((char) next);
        }

        return contextPath;
    }

    public synchronized boolean checkAndAdd(String contextPath) {
        return allocatedContextPaths.add(contextPath);
    }

}
