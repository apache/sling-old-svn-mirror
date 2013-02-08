/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.apache.sling.resourceresolver.impl.helper;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test to cover areas not already covered by the URI test from HttpClient.
 */
public class URI2Test {
    
    @Test
    public void testCreate() {
        URI u = new URI("http","//localhost:8080/to/a/resource", "fragment");
        Assert.assertEquals("http://localhost:8080/to/a/resource", u.toString());   
        u = new URI("http://localhost:8080/to/a/r%20e%20s%20o%20u%20r%20c%20e", true, "UTF-8");
        Assert.assertEquals("http://localhost:8080/to/a/r%20e%20s%20o%20u%20r%20c%20e", u.toString());   
        u = new URI("http://localhost:8080/to/a/r e s o u r c e", false, "UTF-8");
        Assert.assertEquals("http://localhost:8080/to/a/r%20e%20s%20o%20u%20r%20c%20e", u.toString());       
        Assert.assertEquals("r%20e%20s%20o%20u%20r%20c%20e", new String(u.getRawName()));       
        Assert.assertEquals("/to/a/r%20e%20s%20o%20u%20r%20c%20e", new String(u.getRawPathQuery()));

    }
    
    @Test
    public void testClone() throws CloneNotSupportedException {
        URI url = new URI("http://jakarta.apache.org", false);
        URI uriClone = (URI) url.clone();
        Assert.assertEquals(url,uriClone);
        Assert.assertEquals(url.hashCode(),uriClone.hashCode());
    }

}
