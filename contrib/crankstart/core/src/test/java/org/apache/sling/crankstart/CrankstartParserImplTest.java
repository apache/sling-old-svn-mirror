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
package org.apache.sling.crankstart;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Dictionary;
import java.util.Iterator;

import org.apache.sling.crankstart.api.CrankstartCommandLine;
import org.apache.sling.crankstart.api.CrankstartParser;
import org.apache.sling.crankstart.core.CrankstartParserImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class CrankstartParserImplTest {
    private CrankstartParser parser;
    private Reader input;
    public static final String TEST_PATH = "/parser-test.txt";
    
    @Before
    public void setup() throws IOException {
        parser = new CrankstartParserImpl() {

            @Override
            protected String getVariable(String name) {
                if(name.startsWith("ok.")) {
                    return name.toUpperCase() + "_" + name.length();
                }
                
                return super.getVariable(name);
            }
            
        };
        final InputStream is = getClass().getResourceAsStream(TEST_PATH);
        assertNotNull("Expecting test resource to be found:" + TEST_PATH, is);
        input = new InputStreamReader(is);
    }
    
    @After
    public void cleanup() throws IOException {
        if(input != null) {
            input.close();
            input = null;
        }
    }
    
    private void assertCommand(String verb, String qualifier, CrankstartCommandLine cmd) {
        assertEquals("Expecting the correct verb", verb, cmd.getVerb());
        assertEquals("Expecting the correct qualifier", qualifier, cmd.getQualifier());
    }
    
    @Test
    public void parserTest() throws IOException {
        final Iterator<CrankstartCommandLine> it = parser.parse(input);
        
        assertCommand("verb", "qualifier with several words", it.next());
        assertCommand("verb2", "single_qualifier", it.next());
        
        final CrankstartCommandLine config = it.next();
        assertCommand("config", "the.pid.goes.here", config);
        final Dictionary<String, Object> props = config.getProperties();
        assertEquals("Expecting 4 properties", 4, props.size());
        assertEquals("Expecting correct foo value", "bar", props.get("foo"));
        final Object o = props.get("array");
        assertTrue("Expecting array property", o instanceof String[]);
        final String [] a = (String[])o;
        assertEquals("Expecting two entries in array", 2, a.length);
        assertEquals("Expecting correct first array value", "one that has a OK.VAR1_7 variable", a[0]);
        assertEquals("Expecting correct second array value", "two has OK.ONE_6 and OK.OTHER_8 variables", a[1]);
        assertEquals("Expecting correct another value", "property with several words", props.get("another"));
        assertEquals("Expecting correct variable value", "This is OK.VARB_7 now", props.get("OK.VARA_7"));
        
        assertCommand("another", "command", it.next());
        assertCommand("last.command", "", it.next());
        
        assertCommand("var1", "this is CRANKSTART_VAR_NOT_FOUND(some.var) here", it.next());
        assertCommand("var2", "and now OK.VAR2_7 here", it.next());
        assertCommand("var3", "using underscores in OK.UNDER_SCORE_14 variable", it.next());
        
        assertCommand("esc1", "this ${ok.esc} is escaped", it.next());
        assertCommand("esc2", "this OK.ESC_6 is not escaped", it.next());
        assertCommand("esc3", "this $${ok.esc} is triple-escaped", it.next());
        
        assertFalse("Expecting no more commands", it.hasNext());
    }
}
