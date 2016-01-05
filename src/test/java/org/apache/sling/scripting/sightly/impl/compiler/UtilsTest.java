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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtilsTest {

    @Test
    public void testGetJavaNameFromPath() throws Exception {
        assertEquals("apps.project.components.A", Utils.getJavaNameFromPath("/apps/project/components/A.java"));
        assertEquals("apps.pro_ject.components.A", Utils.getJavaNameFromPath("/apps/pro-ject/components/A.java"));
        assertEquals("apps._static.project.components.A", Utils.getJavaNameFromPath("/apps/static/project/components/A.java"));
    }

    @Test
    public void testGetPackageNameFromFQCN() throws Exception {
        assertEquals("apps.project.components", Utils.getPackageNameFromFQCN("apps.project.components.A"));
        assertEquals("apps.pro_ject.components", Utils.getPackageNameFromFQCN("apps.pro_ject.components.A"));
        assertEquals("apps._static.project.components", Utils.getPackageNameFromFQCN("apps._static.project.components.A"));
    }
}
