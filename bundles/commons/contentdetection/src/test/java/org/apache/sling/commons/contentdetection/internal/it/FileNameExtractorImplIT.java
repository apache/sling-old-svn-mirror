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

package org.apache.sling.commons.contentdetection.internal.it;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.apache.sling.commons.contentdetection.FileNameExtractor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
public class FileNameExtractorImplIT {

    @Inject
    private FileNameExtractor fileNameExtractor;

    @Test
    public void testFileNameExtractor(){
        String rawPath = "http://midches.com/images/uploads/default/demo.jpg#anchor?query=test";
        String expectedFileName = "demo.jpg";
        assertEquals(expectedFileName, fileNameExtractor.extract(rawPath));
    }

    @org.ops4j.pax.exam.Configuration
    public Option[] config() {
        return U.paxConfig();
    }
}
