/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.maven.htl.compiler;

import org.apache.sling.scripting.sightly.java.compiler.ClassInfo;
import org.apache.sling.scripting.sightly.java.compiler.JavaEscapeUtils;

public class HTLClassInfo implements ClassInfo {

    private String fqcn;
    private String simpleClassName;
    private String packageName;

    public HTLClassInfo(String script) {
        fqcn = JavaEscapeUtils.makeJavaPackage(script);
    }

    @Override
    public String getSimpleClassName() {
        if (simpleClassName == null) {
            simpleClassName = fqcn.substring(fqcn.lastIndexOf(".") + 1);
        }
        return simpleClassName;
    }

    @Override
    public String getPackageName() {
        if (packageName == null) {
            packageName = fqcn.substring(0, fqcn.lastIndexOf("."));
        }
        return packageName;
    }

    @Override
    public String getFullyQualifiedClassName() {
        return fqcn;
    }
}
