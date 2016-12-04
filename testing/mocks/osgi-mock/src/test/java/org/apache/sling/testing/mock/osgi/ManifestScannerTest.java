/*
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
 */
package org.apache.sling.testing.mock.osgi;

import static org.junit.Assert.assertTrue;

import java.util.Collection;

import org.junit.Test;
import org.osgi.framework.Constants;

public class ManifestScannerTest {

    /**
     * Test some MANIFEST entries from commons-io:commons-io:2.4
     */
    @Test
    public void testGetValues() {
        Collection<String> bundleSymbolicNames = ManifestScanner.getValues(Constants.BUNDLE_SYMBOLICNAME);
        assertTrue(bundleSymbolicNames.contains("org.apache.commons.io"));

        Collection<String> includeResource = ManifestScanner.getValues("Include-Resource");
        assertTrue(includeResource.contains("META-INF/LICENSE.txt=LICENSE.txt"));
        assertTrue(includeResource.contains("META-INF/NOTICE.txt=NOTICE.txt"));
    }

}
