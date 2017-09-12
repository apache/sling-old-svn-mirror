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
package org.apache.sling.pipes.internal;

import org.apache.commons.collections.IteratorUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.pipes.AbstractPipeTest;
import org.apache.sling.pipes.Pipe;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * testing csv pipe
 */
public class CsvPipeTest extends AbstractPipeTest{

    @Test
    public void getOutput() throws Exception {
        String csvPath = "/content/test/standardTest.csv";
        context.load().binaryFile("/standardTest.csv", csvPath);
        Pipe pipe = plumber.newPipe(context.resourceResolver())
                .csv(csvPath).name("csv")
                .mkdir(PATH_FRUITS + "/csv/${csv.fruit}-${csv.color}-${csv.id}").build();
        Iterator<Resource> output = pipe.getOutput();
        List<Resource> resources = IteratorUtils.toList(output);
        List<String> paths = resources.stream().map( resource -> resource.getPath()).collect(Collectors.toList());
        assertEquals("there should be 3 elements", 3, paths.size());
        assertEquals("first should be /content/fruits/csv/apple-green-1", "/content/fruits/csv/apple-green-1", paths.get(0));
        assertEquals("second should be /content/fruits/csv/banana-yellow-2", "/content/fruits/csv/banana-yellow-2", paths.get(1));
        assertEquals("first should be /content/fruits/csv/plum-purple-3", "/content/fruits/csv/plum-purple-3", paths.get(2));
    }
}