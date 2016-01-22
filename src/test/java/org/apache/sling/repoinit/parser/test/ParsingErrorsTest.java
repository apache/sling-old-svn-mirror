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

package org.apache.sling.repoinit.parser.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.sling.repoinit.parser.impl.ACLDefinitionsParserImpl;
import org.apache.sling.repoinit.parser.impl.ParseException;
import org.apache.sling.repoinit.parser.impl.TokenMgrError;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Test various parsing errors */
@RunWith(Parameterized.class)
public class ParsingErrorsTest {

    private String input;
    private Class<? extends Throwable> expected;
    
    @Parameters
    public static Collection<Object[]> data() {
        @SuppressWarnings("serial")
        final List<Object []> result = new ArrayList<Object []>() {{
            add(new Object[] { "foo", ParseException.class  });
            add(new Object[] { "12", ParseException.class  });
            
            add(new Object[] { "set ACL on /apps \n remove * for u \n end", null });
            add(new Object[] { "set ACL on /apps \n badkeyword * for u \n end", ParseException.class });
            add(new Object[] { "set ACL on appsWithoutSlash \n remove * for u \n end", ParseException.class  });
            add(new Object[] { "set ACL", ParseException.class  });
            add(new Object[] { "set ACL \n end", ParseException.class  });
            
            add(new Object[] { "create service user bob, alice, tom21", null  });
            add(new Object[] { "create service user bob-221_BOB", null  });
            add(new Object[] { "create service user bob/221", ParseException.class  });
            add(new Object[] { "create service user bob,/alice, tom21", ParseException.class  });
            add(new Object[] { "create service user bob,alice,tom21 # comment not allowed here", TokenMgrError.class  });
            add(new Object[] { "CREATE service user bob, alice, tom21", ParseException.class });
            add(new Object[] { "create SERVICE user bob, alice, tom21", ParseException.class });
        }};
        return result;
    }
    
    public ParsingErrorsTest(String input, Class<? extends Throwable> expected) {
        this.input = input;
        this.expected = expected;
    }

    @Test
    public void checkResult() throws ParseException, IOException {
        final StringReader r = new StringReader(input);
        try {
            new ACLDefinitionsParserImpl(r).parse();
            if(expected != null) {
                fail("Expected a " + expected.getSimpleName() + " for [" + input + "]");
            }
        } catch(Exception e) {
            assertEquals(expected, e.getClass());
        } catch(Error err) {
            assertEquals(expected, err.getClass());
        } finally {
            r.close();
        }
    }
}
