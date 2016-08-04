/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.utils;

import java.util.Collection;

import org.apache.sling.scripting.sightly.impl.compiler.CompileTimeObjectModel;
import org.junit.Test;

import static org.junit.Assert.*;

public class CompileTimeObjectModelTest {

    @Test
    public void testGetCollectionWithOneElement() {
        String stringObject = "test";
        Integer numberObject = 1;
        Collection stringCollection = CompileTimeObjectModel.toCollection(stringObject);
        assertTrue(stringCollection.size() == 1 && stringCollection.contains(stringObject));
        Collection numberCollection = CompileTimeObjectModel.toCollection(numberObject);
        assertTrue(numberCollection.size() == 1 && numberCollection.contains(numberObject));
    }

}
