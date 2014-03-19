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

public class SecuredProviderResourceAccessSecurityTest extends ResourceAccessSecurityTestBase {

    public void testNonExistingResource() throws Exception {
        String path = "/test/secured-provider/noresource.json";

        testRead(getServerUsername(), getServerPassword(), path, 404);
        testRead(getTestUsername(), getTestPassword(), path, 404);
    }

    @Test
    public void testAllowedReadAccess() throws Exception {
        String path = "/test/secured-provider/read/prov/providergate1-allowread_providergate2-denyread/test.json";

        testRead(getServerUsername(), getServerPassword(), path, 200);
        testRead(getTestUsername(), getTestPassword(), path, 200);
        testRead(null, null, path, 200);
    }

    @Test
    public void testDeniedReadAccessFromNonModifiableProvider() throws Exception {
        String path = "/test/secured-provider/read/prov/providergate1-denyread/test.json";

        testRead(getServerUsername(), getServerPassword(), path, 404);
        testRead(getTestUsername(), getTestPassword(), path, 404);
        testRead(null, null, path, 404);
    }

    @Test
    public void testDeniedReadAccessFromModifiableProvider() throws Exception {
        String path = "/test/secured-provider/read-update/prov/providergate1-denyread/test.json";

        testRead(getServerUsername(), getServerPassword(), path, 404);
        testRead(getTestUsername(), getTestPassword(), path, 404);
    }


    @Test
    public void testNotDefinedReadAccess() throws Exception {
        String path = "/test/secured-provider/read-update/prov/providergate2-denyupdate/test.json";

        testRead(getServerUsername(), getServerPassword(), path, 404);
        testRead(getTestUsername(), getTestPassword(), path, 404);
        testRead(null, null, path, 404);
    }


    @Test
    public void testAllowedReadAndUpdate() throws Exception {
        String path = "/test/secured-provider/read-update/prov/providergate2-allowupdate_providergate1-allowread/test.json";

        testRead(getTestUsername(), getTestPassword(), path, 200);
        testUpdate(getTestUsername(), getTestPassword(), path, 200);
    }


    @Test
    public void testUpdateAllowedUpdateAllowedRead() throws Exception {
        String allowPath = "/test/secured-provider/read-update/prov/providergate1-allowread_providergate1-allowupdate/test.json";

        testUpdate(getTestUsername(), getTestPassword(), allowPath, 200);
    }

    @Test
    public void testUpdateAllowedUpdateDeniedRead() throws Exception {
        String path = "/test/secured-provider/read-update/prov/providergate2-allowupdate_providergate1-denyread/test.json";

        testRead(getTestUsername(), getTestPassword(), path, 404);
        testUpdate(getTestUsername(), getTestPassword(), path, 500, "UnsupportedOperationException");
    }

    @Test
    public void testUpdateDeniedUpdateDeniedRead() throws Exception {
        String path = "/test/secured-provider/read-update/prov/providergate2-denyupdate_providergate1-denyread/test.json";

        testRead(getTestUsername(), getTestPassword(), path, 404);
        testUpdate(getTestUsername(), getTestPassword(), path, 500, "UnsupportedOperationException");
    }

    @Test
    public void testUpdateDeniedUpdateAllowedRead() throws Exception {
        String path = "/test/secured-provider/read-update/prov/providergate2-denyupdate_providergate1-allowread/test.json";

        testRead(getTestUsername(), getTestPassword(), path, 200);
        testUpdate(getTestUsername(), getTestPassword(), path, 500, "is not modifiable");
    }
    
    @Test
    public void testReadOnlyApplicationAccessGatePresent() throws Exception {
        String path = "/test/secured-provider/read/app/appgate1_allowread/test.json";

        testRead(getTestUsername(), getTestPassword(), path, 404);
    }
    
    @Test
    public void testReadMixedAccessGatesPresent() throws Exception {
        String path1 = "/test/secured-provider/read/mixed/appgate1-allowread_providergate1-denyread/test.json";
        String path2 = "/test/secured-provider/read/mixed/appgate1-denyread_providergate1-allowread/test.json";
        String path3 = "/test/secured-provider/read/mixed/appgate1-allowread_providergate1-allowread/test.json";

        testRead(getTestUsername(), getTestPassword(), path1, 404);
        testRead(getTestUsername(), getTestPassword(), path2, 404);
        testRead(getTestUsername(), getTestPassword(), path3, 200);
    }
    
    @Test
    public void testReadNoAccessGatePresent() throws Exception {
        String path = "/test/secured-provider/read/nogate/test.json";

        testRead(getTestUsername(), getTestPassword(), path, 404);
    }
    
    @Test
    public void testUpdateNoAccessGatePresent() throws Exception {
        String path = "/test/secured-provider/read-update/nogate/test.json";

        testUpdate(getTestUsername(), getTestPassword(), path, 500, "UnsupportedOperationException");
    }
    
}
