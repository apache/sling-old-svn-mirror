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

package org.apache.sling.resourceaccesssecurity.it;


import org.junit.Test;

public class UnsecuredProviderResourceAccessSecurityTest extends ResourceAccessSecurityTestBase {

    @Test
    public void testDeniedReadAccess() throws Exception {
        String path = "/test/unsecured-provider/read/prov/providergate1-denyread/test.json";

        // can be read anyway
        testRead(getTestUsername(), getTestPassword(), path, 200);

    }

    @Test
    public void testReadNonExistingResource() throws Exception {
        String path = "/test/unsecured-provider/read/nonexisting/test.json";

        testRead(getTestUsername(), getTestPassword(), path, 404);
    }

    @Test
    public void testReadFromNonExistingProvider() throws Exception {
        String path = "/test/nonexisting/test.json";

        testRead(getTestUsername(), getTestPassword(), path, 404);
    }
}
