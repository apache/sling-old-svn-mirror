/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.engine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SightlyScriptEngineFactoryTest {

    private SightlyEngineConfiguration sightlyEngineConfiguration;

    @Before
    public void setUp() {
        sightlyEngineConfiguration = mock(SightlyEngineConfiguration.class);
        when(sightlyEngineConfiguration.getEngineVersion()).thenReturn("1.0.17-SNAPSHOT");
        when(sightlyEngineConfiguration.getScratchFolder()).thenReturn("/org/apache/sling/scripting/sightly");
    }

    @After
    public void tearDown() {
        sightlyEngineConfiguration = null;
    }

    @Test
    public void testActivateNoPreviousInfo() {
        SightlyScriptEngineFactory scriptEngineFactory = new SightlyScriptEngineFactory();
        ClassLoaderWriter classLoaderWriter = mock(ClassLoaderWriter.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(classLoaderWriter.getOutputStream(SightlyScriptEngineFactory.SIGHTLY_CONFIG_FILE)).thenReturn(outputStream);
        Whitebox.setInternalState(scriptEngineFactory, "classLoaderWriter", classLoaderWriter);
        Whitebox.setInternalState(scriptEngineFactory, "sightlyEngineConfiguration", sightlyEngineConfiguration);
        scriptEngineFactory.activate();
        verify(classLoaderWriter).delete(sightlyEngineConfiguration.getScratchFolder());
        assertEquals("1.0.17-SNAPSHOT", outputStream.toString());
    }

    @Test
    public void testActivateOverPreviousVersion()  {
        SightlyScriptEngineFactory scriptEngineFactory = new SightlyScriptEngineFactory();
        ClassLoaderWriter classLoaderWriter = mock(ClassLoaderWriter.class);
        try {
            when(classLoaderWriter.getInputStream(SightlyScriptEngineFactory.SIGHTLY_CONFIG_FILE))
                    .thenReturn(IOUtils.toInputStream("1.0.16", "UTF-8"));
        } catch (IOException e) {
            fail("IOException while setting tests.");
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(classLoaderWriter.getOutputStream(SightlyScriptEngineFactory.SIGHTLY_CONFIG_FILE)).thenReturn(outputStream);
        when(classLoaderWriter.delete(sightlyEngineConfiguration.getScratchFolder())).thenReturn(true);
        Whitebox.setInternalState(scriptEngineFactory, "classLoaderWriter", classLoaderWriter);
        Whitebox.setInternalState(scriptEngineFactory, "sightlyEngineConfiguration", sightlyEngineConfiguration);
        scriptEngineFactory.activate();
        verify(classLoaderWriter).delete(sightlyEngineConfiguration.getScratchFolder());
        assertEquals("1.0.17-SNAPSHOT", outputStream.toString());
    }

    @Test
    public void testActivateOverSameVersion() {
        SightlyScriptEngineFactory scriptEngineFactory = new SightlyScriptEngineFactory();
        ClassLoaderWriter classLoaderWriter = mock(ClassLoaderWriter.class);
        try {
            when(classLoaderWriter.getInputStream(SightlyScriptEngineFactory.SIGHTLY_CONFIG_FILE))
                    .thenReturn(IOUtils.toInputStream("1.0.17-SNAPSHOT", "UTF-8"));
        } catch (IOException e) {
            fail("IOException while setting tests.");
        }
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream spyOutputStream = spy(outputStream);
        when(classLoaderWriter.getOutputStream(SightlyScriptEngineFactory.SIGHTLY_CONFIG_FILE)).thenReturn(spyOutputStream);
        Whitebox.setInternalState(scriptEngineFactory, "classLoaderWriter", classLoaderWriter);
        Whitebox.setInternalState(scriptEngineFactory, "sightlyEngineConfiguration", sightlyEngineConfiguration);
        scriptEngineFactory.activate();
        verify(classLoaderWriter, never()).delete(sightlyEngineConfiguration.getScratchFolder());
        try {
            verify(spyOutputStream, never()).write(any(byte[].class));
            verify(spyOutputStream, never()).close();
        } catch (IOException e) {
            fail("IOException in test verification.");
        }
    }
}
