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
package org.apache.sling.ide.jcr;

import java.net.URISyntaxException;

import org.apache.jackrabbit.vault.fs.api.RepositoryAddress;
import org.apache.sling.ide.jcr.RepositoryUtils;
import org.junit.Assert;
import org.junit.Test;

public class RepositoryUtilsTest {

    @Test
    public void testWebDavUrlLocationForAEM() throws URISyntaxException {
        // make sure AEM url contains workspace name
        String url = "http://localhost:4502/" + RepositoryUtils.WEBDAV_URL_LOCATIONS[1];
        RepositoryAddress address = new RepositoryAddress(url);
        // make sure the workspace name is correctly extracted
        Assert.assertEquals("crx.default", address.getWorkspace());
    }

    @Test
    public void testWebDavUrlLocationForSling() throws URISyntaxException {
        // make sure AEM url contains workspace name
        String url = "http://localhost:4502/" + RepositoryUtils.WEBDAV_URL_LOCATIONS[0];
        RepositoryAddress address = new RepositoryAddress(url);
        // make sure the workspace name is correctly extracted
        Assert.assertEquals("default", address.getWorkspace());
    }
}
