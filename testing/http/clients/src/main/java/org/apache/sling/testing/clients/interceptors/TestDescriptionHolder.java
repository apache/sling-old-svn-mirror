/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.sling.testing.clients.interceptors;

public class TestDescriptionHolder {

    private static final ThreadLocal<String> methodName = new ThreadLocal<String>();
    private static final ThreadLocal<String> className = new ThreadLocal<String>();

    public static String getMethodName() {
        return methodName.get();
    }

    public static void setMethodName(String methodName) {
        TestDescriptionHolder.methodName.set(methodName);
    }

    public static void removeMethodName() {
        TestDescriptionHolder.methodName.remove();
    }

    public static String getClassName() {
        return className.get();
    }

    public static void setClassName(String className) {
        TestDescriptionHolder.className.set(className);
    }

    public static void removeClassName() {
        TestDescriptionHolder.className.remove();
    }
}
