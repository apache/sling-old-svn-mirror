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
package org.apache.sling.launchpad.app;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;
import org.apache.sling.launchpad.base.shared.SharedConstants;

public class MainTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        System.setProperty("___sling_dont_exit___", "true");
    }

    public void test_parseCommandLine_null_args() {
        String[] args = null;
        Map<String, String> commandline = Main.parseCommandLine(args);
        assertNotNull("commandline map must not be null", commandline);
        assertTrue("commandline map must be empty", commandline.isEmpty());
    }

    public void test_parseCommandLine_empty_args() {
        String[] args = {};
        Map<String, String> commandline = Main.parseCommandLine(args);
        assertNotNull("commandline map must not be null", commandline);
        assertTrue("commandline map must be empty", commandline.isEmpty());
    }

    public void test_parseCommandLine_single_dash() {
        String[] args = { "-" };
        Map<String, String> commandline = Main.parseCommandLine(args);
        assertNotNull("commandline map must not be null", commandline);
        assertTrue("commandline map must be empty", commandline.isEmpty());
    }

    public void test_parseCommandLine_single_arg_no_par() {
        String[] args = { "-a" };
        Map<String, String> commandline = Main.parseCommandLine(args);
        assertNotNull("commandline map must not be null", commandline);
        assertEquals("commandline map must have one entry", 1,
            commandline.size());
        assertEquals("single argument must be " + args[0].charAt(1),
                String.valueOf(args[0].charAt(1)),
                commandline.keySet().iterator().next());
        assertEquals("single argument value must be " + args[0].charAt(1),
                String.valueOf(args[0].charAt(1)),
                commandline.values().iterator().next());
    }

    public void test_parseCommandLine_single_arg_with_par() {
        String[] args = { "-a", "value" };
        Map<String, String> commandline = Main.parseCommandLine(args);
        assertNotNull("commandline map must not be null", commandline);
        assertEquals("commandline map must have one entry", 1,
            commandline.size());
        assertEquals("single argument must be " + args[0].charAt(1),
            String.valueOf(args[0].charAt(1)),
            commandline.keySet().iterator().next());
        assertEquals("single argument value must be " + args[1], args[1],
            commandline.values().iterator().next());
    }

    public void test_parseCommandLine_two_args_no_par() {
        String[] args = { "-a", "-b" };
        Map<String, String> commandline = Main.parseCommandLine(args);
        assertNotNull("commandline map must not be null", commandline);
        assertEquals("commandline map must have two entries", 2,
            commandline.size());
        assertEquals("argument a must a", "a", commandline.get("a"));
        assertEquals("argument b must b", "b", commandline.get("b"));
    }

    public void test_parseCommandLine_two_args_first_par() {
        String[] args = { "-a", "apar", "-b" };
        Map<String, String> commandline = Main.parseCommandLine(args);
        assertNotNull("commandline map must not be null", commandline);
        assertEquals("commandline map must have two entries", 2,
            commandline.size());
        assertEquals("argument a must apar", "apar", commandline.get("a"));
        assertEquals("argument b must b", "b", commandline.get("b"));
    }

    public void test_parseCommandLine_two_args_second_par() {
        String[] args = { "-a", "-b", "bpar" };
        Map<String, String> commandline = Main.parseCommandLine(args);
        assertNotNull("commandline map must not be null", commandline);
        assertEquals("commandline map must have two entries", 2,
            commandline.size());
        assertEquals("argument a must a", "a", commandline.get("a"));
        assertEquals("argument b must bpar", "bpar", commandline.get("b"));
    }

    public void test_parseCommandLine_two_args_all_par() {
        String[] args = { "-a", "apar", "-b", "bpar" };
        Map<String, String> commandline = Main.parseCommandLine(args);
        assertNotNull("commandline map must not be null", commandline);
        assertEquals("commandline map must have two entries", 2,
            commandline.size());
        assertEquals("argument a must apar", "apar", commandline.get("a"));
        assertEquals("argument b must bpar", "bpar", commandline.get("b"));
    }

    public void test_parseCommandLine_three_args_with_dash() {
        String[] args = { "-a", "apar", "-", "-b", "bpar" };
        Map<String, String> commandline = Main.parseCommandLine(args);
        assertNotNull("commandline map must not be null", commandline);
        assertEquals("commandline map must have three entries", 3,
                commandline.size());
        assertEquals("argument a must apar", "apar", commandline.get("a"));
        assertEquals("argument -b must -b", "-b", commandline.get("-b"));
        assertEquals("argument bpar must bpar", "bpar", commandline.get("bpar"));
    }

    public void test_parseCommandLine_single_arg_with_dash_par() {
        String[] args = { "-a", "-" };
        Map<String, String> commandline = Main.parseCommandLine(args);
        assertNotNull("commandline map must not be null", commandline);
        assertEquals("commandline map must have three entries", 1,
            commandline.size());
        assertEquals("argument a must -", "-", commandline.get("a"));
    }

    public void test_convertCommandLineArgs_no_args() {
        Map<String, String> props = Main.convertCommandLineArgs(new HashMap<String, String>());
        assertNotNull(props);
        assertTrue(props.isEmpty());
    }

    public void test_converCommandLineArgs_unknown() {
        assertNull(Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                put("u", "short");
            }
        }));
        assertNull(Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                put("longer", "long");
            }
        }));
    }

    public void test_converCommandLineArgs_j_start_stop_status() {
        Map<String, String> props = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                put("j", "j");
            }
        });
        assertNull(props);

        Map<String, String> props1 = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                put("j", "host:port");
            }
        });
        assertNotNull(props1);
        assertEquals(1, props1.size());
        assertEquals("host:port", props1.get("sling.control.socket"));

        Map<String, String> propsStart = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                put("start", "start");
            }
        });
        assertNotNull(propsStart);
        assertEquals(1, propsStart.size());
        assertEquals("start", propsStart.get("sling.control.action"));

        Map<String, String> propsStatus = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                put("status", "status");
            }
        });
        assertNotNull(propsStatus);
        assertEquals(1, propsStatus.size());
        assertEquals("status", propsStatus.get("sling.control.action"));

        Map<String, String> propsStop = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                put("stop", "stop");
            }
        });
        assertNotNull(propsStop);
        assertEquals(1, propsStop.size());
        assertEquals("stop", propsStop.get("sling.control.action"));
    }

    public void test_converCommandLineArgs_l() {
        Map<String, String> props = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                put("l", "INFO");
            }
        });
        assertNotNull(props);
        assertEquals(1, props.size());
        assertEquals("INFO", props.get("org.apache.sling.commons.log.level"));

        Map<String, String> props2 = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                final String l = "l";
                put(l, l);
            }
        });
        assertNull(props2);
    }

    public void test_converCommandLineArgs_f() {
        Map<String, String> props = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                put("f", "sling.changed");
            }
        });
        assertNotNull(props);
        assertEquals(1, props.size());
        assertEquals("sling.changed", props.get("org.apache.sling.commons.log.file"));

        Map<String, String> props1 = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                put("f", "-");
            }
        });
        assertNotNull(props1);
        assertEquals(1, props1.size());
        assertEquals("", props1.get("org.apache.sling.commons.log.file"));

        Map<String, String> props2 = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                final String f = "f";
                put(f, f);
            }
        });
        assertNull(props2);
    }

    public void test_converCommandLineArgs_c() {
        Map<String, String> props = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                put("c", "sling.changed");
            }
        });
        assertNotNull(props);
        assertEquals(1, props.size());
        assertEquals("sling.changed", props.get(SharedConstants.SLING_HOME));

        Map<String, String> props2 = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                final String c = "c";
                put(c, c);
            }
        });
        assertNull(props2);
    }

    public void test_converCommandLineArgs_i() {
        Map<String, String> props = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                put("i", "launchpad");
            }
        });
        assertNotNull(props);
        assertEquals(1, props.size());
        assertEquals("launchpad", props.get(SharedConstants.SLING_LAUNCHPAD));

        Map<String, String> props2 = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                final String i = "i";
                put(i, i);
            }
        });
        assertNull(props2);
    }

    public void test_converCommandLineArgs_a() {
        Map<String, String> props = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                put("a", "0.0.0.0");
            }
        });
        assertNotNull(props);
        assertEquals(1, props.size());
        assertEquals("0.0.0.0", props.get("org.apache.felix.http.host"));
    }

    public void test_converCommandLineArgs_r() {
        Map<String, String> props = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                put("r", "/mycontext");
            }
        });
        assertNotNull(props);
        assertEquals(1, props.size());
        assertEquals("/mycontext", props.get("org.apache.felix.http.context_path"));
    }

    public void test_converCommandLineArgs_p() {
        Map<String, String> props = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                put("p", "1234");
            }
        });
        assertNotNull(props);
        assertEquals(1, props.size());
        assertEquals("1234", props.get("org.osgi.service.http.port"));

        Map<String, String> props1 = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                put("p", "abc");
            }
        });
        assertNull(props1);

        Map<String, String> props2 = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                final String p = "p";
                put(p, p);
            }
        });
        assertNull(props2);
    }

    public void test_converCommandLineArgs_n() {
        Map<String, String> props = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                put("n", "n");
            }
        });
        assertNotNull(props);
        assertEquals(1, props.size());
        assertEquals(Boolean.FALSE.toString(), props.get("sling.shutdown.hook"));

        Map<String, String> props1 = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                put("D", "sling.shutdown.hook=" + Boolean.TRUE.toString());
            }
        });
        assertNotNull(props1);
        assertEquals(1, props1.size());
        assertEquals(Boolean.TRUE.toString(), props1.get("sling.shutdown.hook"));
    }

    public void test_converCommandLineArgs_multi_D() {
        String[] args = {"-Da1=b1", "-Da2=b2"};
        Map<String, String> commandline = Main.parseCommandLine(args);
        Map<String, String> props = Main.convertCommandLineArgs(commandline);
        assertEquals(2, props.size());
        assertEquals("b1", props.get("a1"));
        assertEquals("b2", props.get("a2"));
    }

    public void test_converCommandLineArgs_multi_D_with_space() {
        String[] args = {"-D", "a1=b1", "-D", "a2=b2"};
        Map<String, String> commandline = Main.parseCommandLine(args);
        Map<String, String> props = Main.convertCommandLineArgs(commandline);
        assertEquals(2, props.size());
        assertEquals("b1", props.get("a1"));
        assertEquals("b2", props.get("a2"));
    }

    public void test_converCommandLineArgs_D() {
        Map<String, String> props = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                put("D", "name=value");
            }
        });
        assertNotNull(props);
        assertEquals(1, props.size());
        assertEquals("value", props.get("name"));

        Map<String, String> props2 = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                put("D", "flag");
            }
        });
        assertNotNull(props2);
        assertEquals(1, props2.size());
        assertEquals("flag", props2.get("flag"));

        Map<String, String> props1 = Main.convertCommandLineArgs(new HashMap<String, String>() {
            {
                final String d = "D";
                put(d, d);
            }
        });
        assertNull(props1);
    }

    public void test_installShutdownHook() throws SecurityException, NoSuchMethodException, IllegalArgumentException,
            IllegalAccessException, InvocationTargetException {
        final Method m = Main.class.getDeclaredMethod("installShutdownHook", Map.class);
        m.setAccessible(true);

        final String key = "sling.shutdown.hook";
        System.getProperties().remove(key);

        TestCase.assertEquals(Boolean.TRUE, m.invoke(null, new HashMap<String, String>()));
        TestCase.assertEquals(Boolean.TRUE, m.invoke(null, new HashMap<String, String>() {
            {
                put(key, "true");
            }
        }));
        TestCase.assertEquals(Boolean.TRUE, m.invoke(null, new HashMap<String, String>() {
            {
                put(key, "TRUE");
            }
        }));
        TestCase.assertEquals(Boolean.FALSE, m.invoke(null, new HashMap<String, String>() {
            {
                put(key, "false");
            }
        }));
        TestCase.assertEquals(Boolean.FALSE, m.invoke(null, new HashMap<String, String>() {
            {
                put(key, "not true");
            }
        }));

        System.setProperty(key, "true");
        TestCase.assertEquals(Boolean.TRUE, m.invoke(null, new HashMap<String, String>()));
        System.setProperty(key, "TRUE");
        TestCase.assertEquals(Boolean.TRUE, m.invoke(null, new HashMap<String, String>()));
        System.setProperty(key, "false");
        TestCase.assertEquals(Boolean.FALSE, m.invoke(null, new HashMap<String, String>()));
        System.setProperty(key, "not true");
        TestCase.assertEquals(Boolean.FALSE, m.invoke(null, new HashMap<String, String>()));

        System.setProperty(key, "true");
        TestCase.assertEquals(Boolean.TRUE, m.invoke(null, new HashMap<String, String>() {
            {
                put(key, "true");
            }
        }));
        TestCase.assertEquals(Boolean.TRUE, m.invoke(null, new HashMap<String, String>() {
            {
                put(key, "TRUE");
            }
        }));
        TestCase.assertEquals(Boolean.FALSE, m.invoke(null, new HashMap<String, String>() {
            {
                put(key, "false");
            }
        }));
        TestCase.assertEquals(Boolean.FALSE, m.invoke(null, new HashMap<String, String>() {
            {
                put(key, "not true");
            }
        }));

        System.setProperty(key, "false");
        TestCase.assertEquals(Boolean.TRUE, m.invoke(null, new HashMap<String, String>() {
            {
                put(key, "true");
            }
        }));
        TestCase.assertEquals(Boolean.TRUE, m.invoke(null, new HashMap<String, String>() {
            {
                put(key, "TRUE");
            }
        }));
        TestCase.assertEquals(Boolean.FALSE, m.invoke(null, new HashMap<String, String>() {
            {
                put(key, "false");
            }
        }));
        TestCase.assertEquals(Boolean.FALSE, m.invoke(null, new HashMap<String, String>() {
            {
                put(key, "not true");
            }
        }));
    }
}
