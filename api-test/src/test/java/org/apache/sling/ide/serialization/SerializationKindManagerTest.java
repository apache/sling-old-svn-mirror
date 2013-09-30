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
package org.apache.sling.ide.serialization;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collections;

import org.apache.sling.ide.transport.RepositoryException;
import org.junit.Before;
import org.junit.Test;

public class SerializationKindManagerTest {

    private SerializationKindManager skm;

    @Before
    public void init() throws RepositoryException {

        skm = new SerializationKindManager();
        skm.init(new StubRepository());
    }

    @Test
    public void ntFile() {

        assertThat(skm.getSerializationKind("nt:file", Collections.<String> emptyList()), is(SerializationKind.FILE));
    }

    @Test
    public void ntFolder() {

        assertThat(skm.getSerializationKind("nt:folder", Collections.<String> emptyList()), is(SerializationKind.FOLDER));
    }

    @Test
    public void ntUnstructured() {

        assertThat(skm.getSerializationKind("nt:unstructured", Collections.<String> emptyList()), is(SerializationKind.METADATA_PARTIAL));
    }

    @Test
    public void osgiConfig() {

        assertThat(skm.getSerializationKind("sling:OsgiConfig", Collections.<String> emptyList()), is(SerializationKind.METADATA_FULL));
    }

    @Test
    public void slingFolder() {

        assertThat(skm.getSerializationKind("sling:Folder", Collections.<String> emptyList()), is(SerializationKind.FOLDER));
    }

    @Test
    public void slingFolderWithFullCoverageMixin() {

        assertThat(skm.getSerializationKind("sling:Folder", Collections.singletonList("vlt:FullCoverage")),
                is(SerializationKind.METADATA_FULL));
    }
}
