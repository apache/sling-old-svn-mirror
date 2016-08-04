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
package org.apache.sling.ide.util;

public class PathUtil {

    public static String join(String first, String second) {

        boolean repoUrlHasTrailingSlash = first.endsWith("/");
        boolean relativePathHasLeadingSlash = !second.isEmpty() && second.charAt(0) == '/';

        if (repoUrlHasTrailingSlash ^ relativePathHasLeadingSlash)
            return first + second;
        if (!repoUrlHasTrailingSlash && !relativePathHasLeadingSlash)
            return first + '/' + second;
        if (repoUrlHasTrailingSlash && relativePathHasLeadingSlash)
            return first + second.substring(1);

        throw new AssertionError("unreachable");
    }

    public static String getName(String path) {

        return path.substring(path.lastIndexOf('/') + 1);
    }

    public static String getParent(String path) {

        if (path == null || path.length() == 0 || path.charAt(0) != '/' || path.indexOf('/') == -1) {
            throw new IllegalArgumentException("No a valid or absolut path: " + path);
        }

        if (path.equals("/")) {
            return null;
        }
        

        if (path.lastIndexOf('/') == 0) {
            return "/";
        }

        return path.substring(0, path.lastIndexOf('/'));

    }
    
    public static boolean isAncestor(String ancestor, String child) {
        
        while ( (child = getParent(child)) != null ) {
            if ( child.equals(ancestor)) {
                return true;
            }
        }
        
        return false;
    }
}
