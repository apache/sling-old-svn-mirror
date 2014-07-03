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
package org.apache.sling.ide.transport;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ResourceProxyTest {

    @Test
    public void coveredChildren_firstLevel() {

        ResourceProxy r = new ResourceProxy("/content");
        ResourceProxy child = new ResourceProxy("/content/test");
        child.addProperty("jcr:primaryType", "nt:unstructured");
        r.addChild(child);

        assertThat(r.covers("/content/test"), is(true));
    }

    @Test
    public void coveredChildren_secondLevel() {

        ResourceProxy r = new ResourceProxy("/content");
        ResourceProxy child = new ResourceProxy("/content/test");
        child.addProperty("jcr:primaryType", "nt:unstructured");
        r.addChild(child);

        ResourceProxy grandChild = new ResourceProxy("/content/test/en");
        grandChild.addProperty("jcr:primaryType", "nt:unstructured");
        child.addChild(grandChild);

        assertThat(r.covers("/content/test/en"), is(true));
    }

    @Test
    public void coveredChildren_notCoveredFirstLevel() {

        ResourceProxy r = new ResourceProxy("/content");
        r.addChild(new ResourceProxy("/content/test"));

        assertThat(r.covers("/content/test"), is(false));
    }

    @Test
    public void coveredChildren_notCoveredSecondLevel() {

        ResourceProxy r = new ResourceProxy("/content");
        ResourceProxy child = new ResourceProxy("/content/test");
        child.addProperty("jcr:primaryType", "nt:unstructured");
        r.addChild(child);

        ResourceProxy grandChild = new ResourceProxy("/content/test/en");
        child.addChild(grandChild);

        assertThat(r.covers("/content/test/en"), is(false));
    }
}
