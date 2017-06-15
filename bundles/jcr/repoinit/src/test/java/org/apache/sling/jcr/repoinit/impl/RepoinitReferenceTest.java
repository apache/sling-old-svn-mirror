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
package org.apache.sling.jcr.repoinit.impl;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.sling.jcr.repoinit.impl.RepoinitTextProvider.Reference;
import org.apache.sling.jcr.repoinit.impl.RepoinitTextProvider.TextFormat;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Test the RepoinitTextProvider references parsing */
@RunWith(Parameterized.class)
public class RepoinitReferenceTest {
    
    @Parameters(name="{0}")
    public static Collection<Object[]> data() {
        final List<Object []> result = new ArrayList<Object[]>();
        
        // Valid references
        result.add(new Object[] { "model@foo:uri:1234", TextFormat.model, "foo", "uri:1234", null }); 
        result.add(new Object[] { "model:uri:2345", TextFormat.model, "repoinit", "uri:2345", null }); 
        result.add(new Object[] { "raw:uri:4567", TextFormat.raw , null, "uri:4567", null }); 
        result.add(new Object[] { "raw:uri@5678", TextFormat.raw, null, "uri@5678", null });

        // Invalid references
        result.add(new Object[] { "model@foo", null, null, null, IllegalArgumentException.class });
        result.add(new Object[] { "model#foo:url", TextFormat.model, "repoinit", "url", IllegalArgumentException.class });
        result.add(new Object[] { "", null, null, null, IllegalArgumentException.class });
        result.add(new Object[] { null, null, null, null, IllegalArgumentException.class });
        result.add(new Object[] { "foo:url", null, null, null, IllegalArgumentException.class });
        
        // foo is ignored, by design
        result.add(new Object[] { "raw@foo:url", TextFormat.raw, null, "url", null });
        
        return result;
    }
    
    private final String input;
    private final RepoinitTextProvider.TextFormat format;
    private final String modelSection;
    private final String url;
    private final Class<?> expectedException;
    
    public RepoinitReferenceTest(String input, TextFormat format, String modelSection, String url, Class<? >expectedException) {
        this.input = input;
        this.format = format;
        this.modelSection = modelSection;
        this.url = url;
        this.expectedException = expectedException;
    }
    
    @Test
    public void testParsing() {
        try {
            final Reference ref = new Reference(input);
            if(expectedException != null) {
                fail("Expected a " + expectedException.getName());
            }
            assertEquals(format, ref.format);
            assertEquals(modelSection, ref.modelSection);
            assertEquals(url, ref.url);
        } catch(Exception e) {
            if(expectedException != null) {
                assertEquals(expectedException, e.getClass());
            } else {
                fail("Unexpected " + e);
            }
        }
    }
}