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
package org.apache.sling.resourceresolver.impl.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.apache.sling.api.resource.PersistableValueMap;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Test;

@SuppressWarnings("deprecation")
public class RedirectResourceTest {

    @Test public void testRedirectResource() {

        final String path = "/redir/path";
        final String target = "/redir/target";
        final int status = 999;
        final RedirectResource res = new RedirectResource(null, path, target,
            status);

        assertEquals(path, res.getPath());
        assertEquals(RedirectResource.RT_SLING_REDIRECT, res.getResourceType());

        final Map<?, ?> map = res.adaptTo(Map.class);
        assertNotNull("Expected Map adapter", map);
        assertEquals(target, map.get(RedirectResource.PROP_SLING_TARGET));
        assertEquals(status, ((Integer) map.get(RedirectResource.PROP_SLING_STATUS)).intValue());

        final ValueMap valueMap = res.adaptTo(ValueMap.class);
        assertNotNull("Expected ValueMap adapter", valueMap);
        assertEquals(target, valueMap.get(RedirectResource.PROP_SLING_TARGET));
        assertEquals(status, ((Integer) valueMap.get(RedirectResource.PROP_SLING_STATUS)).intValue());
        assertEquals(status, valueMap.get(RedirectResource.PROP_SLING_STATUS, Integer.class).intValue());

        final PersistableValueMap persistableValueMap = res.adaptTo(PersistableValueMap.class);
        assertNull("Unexpected PersistableValueMap adapter",
            persistableValueMap);
    }
}
