/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.compiler;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.scripting.sightly.impl.utils.JavaEscapeUtils;

public class Utils {

    public static String getJavaNameFromPath(String path) {
        if (path.endsWith(".java")) {
            path = path.substring(0, path.length() - 5);
        }
        String[] parts = path.split("/");
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (StringUtils.isNotEmpty(part)) {
                stringBuilder.append(JavaEscapeUtils.getEscapedToken(parts[i]));
                if (i != parts.length - 1) {
                    stringBuilder.append(".");
                }
            }
        }
        return stringBuilder.toString();
    }

    public static String getPackageNameFromFQCN(String fqcn) {
        if (StringUtils.isNotEmpty(fqcn)) {
            return fqcn.substring(0, fqcn.lastIndexOf("."));
        }
        return null;
    }

}
