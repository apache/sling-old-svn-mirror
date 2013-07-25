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
package org.apache.sling.ide.impl.resource.filer;

import java.util.List;

import org.apache.sling.ide.filter.Filter;
import org.apache.sling.ide.filter.FilterResult;

public class SimpleFilter implements Filter {

    private final List<String> includedPathPrefixes;

    public SimpleFilter(List<String> includedPathPrefixes) {
        this.includedPathPrefixes = includedPathPrefixes;
    }

    @Override
    public FilterResult filter(String relativeFilePath) {
        if (relativeFilePath.isEmpty() || relativeFilePath.charAt(0) != '/') {
            relativeFilePath = '/' + relativeFilePath;
        }
        System.out.println("SimpleFilter.filter(" + relativeFilePath + ")");

        if (includedPathPrefixes.isEmpty()) {
            System.out.println(" -- no path prefixes -> " + FilterResult.ALLOW);
            return FilterResult.ALLOW;
        }

        for (String includePath : includedPathPrefixes) {
            System.out.println(" -- checking with " + includePath);
            if (relativeFilePath.startsWith(includePath)) {
                System.out.println(" --- found match -> " + FilterResult.ALLOW);
                return FilterResult.ALLOW;
            }
        }

        System.out.println(" -- no match " + FilterResult.DENY);
        return FilterResult.DENY;
    }
}
