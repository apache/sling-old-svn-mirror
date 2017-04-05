/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.commons.fsclassloader.impl;

import java.io.PrintWriter;
import java.io.StringWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.commons.classloader.ClassLoaderWriter;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class FSClassLoaderWebConsoleTest {

    private FSClassLoaderWebConsole console;
    private ClassLoaderWriter classLoaderWriter;

    @After
    public void after() {
        console = null;
        classLoaderWriter = null;
    }

    @Test
    public void testClearClassLoaderOK() throws Exception {
        setFixture(true);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
        when(request.getParameter(FSClassLoaderWebConsole.POST_PARAM_CLEAR_CLASSLOADER)).thenReturn("true");
        console.doPost(request, response);
        verify(classLoaderWriter).delete("");
        verify(response).setStatus(HttpServletResponse.SC_OK);
        assertEquals("{ \"status\" : \"success\" }", stringWriter.toString());
    }

    @Test
    public void testClearClassLoaderWrongCommand() throws Exception {
        setFixture(true);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
        when(request.getParameter(FSClassLoaderWebConsole.POST_PARAM_CLEAR_CLASSLOADER)).thenReturn("random");
        console.doPost(request, response);
        verify(classLoaderWriter, Mockito.times(0)).delete("");
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertEquals("{ \"status\" : \"failure\", \"message\" : \"invalid command\" }", stringWriter.toString());
    }

    @Test
    public void testClearClassLoaderUnableToClean() throws Exception {
        setFixture(false);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
        when(request.getParameter(FSClassLoaderWebConsole.POST_PARAM_CLEAR_CLASSLOADER)).thenReturn("true");
        console.doPost(request, response);
        verify(classLoaderWriter).delete("");
        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        assertEquals("{ \"status\" : \"failure\", \"message\" : \"unable to clear classloader; check server log\" }",
                stringWriter.toString());
    }

    private void setFixture(boolean clwReturn) {
        console = spy(new FSClassLoaderWebConsole());
        classLoaderWriter = mock(ClassLoaderWriter.class);
        when(classLoaderWriter.delete("")).thenReturn(clwReturn);
        Whitebox.setInternalState(console, "classLoaderWriter", classLoaderWriter);
    }


}
