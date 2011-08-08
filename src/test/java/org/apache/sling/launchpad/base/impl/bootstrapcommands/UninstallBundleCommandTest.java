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
package org.apache.sling.launchpad.base.impl.bootstrapcommands;

import static org.junit.Assert.assertNotNull;

import java.util.Hashtable;

import org.apache.felix.framework.Logger;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

/** Test the UninstallBundleCommand */
@RunWith(JMock.class)
public class UninstallBundleCommandTest {
    final Mockery mockery = new JUnit4Mockery();
    private BundleContext bundleContext;
    private final Logger logger = new Logger();

    @Before
    public void setUp() throws Exception {
        final Bundle [] b = new Bundle[3];
        for(int i=0; i < b.length; i++) {
            b[i] = mockery.mock(Bundle.class, "bundle" + i);
        }

        // b0 is in version range, will be uninstalled
        mockery.checking(new Expectations() {{
            allowing(b[0]).getSymbolicName();
            will(returnValue("testbundle"));
            allowing(b[0]).getHeaders();
            will(returnValue(new Hashtable<String, String>()));
            allowing(b[0]).getVersion();
            will(returnValue(new Version("1.0.0")));
            exactly(1).of(b[0]).uninstall();
        }});

        // b1 is not in version range, not uninstalled
        mockery.checking(new Expectations() {{
            allowing(b[1]).getSymbolicName();
            will(returnValue("testbundle"));
            allowing(b[1]).getHeaders();
            will(returnValue(new Hashtable<String, String>()));
            allowing(b[1]).getVersion();
            will(returnValue(new Version("2.0.0")));
        }});

        // b2 has different symbolic name, not uninstalled
        mockery.checking(new Expectations() {{
            allowing(b[2]).getSymbolicName();
            will(returnValue("otherbundle"));
            allowing(b[2]).getHeaders();
            will(returnValue(new Hashtable<String, String>()));
            allowing(b[2]).getVersion();
            will(returnValue(new Version("1.0.0")));
        }});

        bundleContext = mockery.mock(BundleContext.class);
        mockery.checking(new Expectations() {{
            allowing(bundleContext).getBundles();
            will(returnValue(b));
        }});
    }

    @Test
    public void testExplicitVersion() throws Exception {
        final UninstallBundleCommand proto = new UninstallBundleCommand();
        // v=1.0.0 should remove 1.0.0 only, not 2.0.0
        final Command cmd = proto.parse("uninstall testbundle 1.0.0");
        assertNotNull("Expecting parsing to succeed", cmd);
        cmd.execute(logger, bundleContext);
    }

    @Test
    public void testVersionRange() throws Exception {
        final UninstallBundleCommand proto = new UninstallBundleCommand();
        final String from1Includedto2NotIncluded = "[1,2)";
        final Command cmd = proto.parse("uninstall testbundle " + from1Includedto2NotIncluded);
        assertNotNull("Expecting parsing to succeed", cmd);
        cmd.execute(logger, bundleContext);
    }
}