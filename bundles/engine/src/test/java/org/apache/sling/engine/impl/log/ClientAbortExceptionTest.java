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
package org.apache.sling.engine.impl.log;

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;

import org.apache.sling.engine.impl.helper.ClientAbortException;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.junit.Before;
import org.junit.Test;

/** Verify that RequestLoggerResponse wraps IOException in ClientAbortException */
public class ClientAbortExceptionTest {

    private Mockery context;
    private RequestLoggerResponse r;

    @Before
    public void setup() throws IOException {
        context = new Mockery();

        final ServletOutputStream sos = new ServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("Always fails, on purpose");
            }

            @Override
            public void flush() throws IOException {
                throw new IOException("Always fails, on purpose");
            }

            @Override
            public void close() throws IOException {
                throw new IOException("Always fails, on purpose");
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
            }
        };
        final HttpServletResponse raw =  context.mock(HttpServletResponse.class);
        context.checking(new Expectations() {{
            allowing(raw).getOutputStream();
            will(returnValue(sos));
        }});

        r = new RequestLoggerResponse(raw);
    }

    @Test(expected=ClientAbortException.class)
    public void writeInt() throws IOException {
        r.getOutputStream().write(42);
    }

    @Test(expected=ClientAbortException.class)
    public void writeSimpleByteArray() throws IOException {
        r.getOutputStream().write("foo".getBytes());
    }

    @Test(expected=ClientAbortException.class)
    public void writeByteArray() throws IOException {
        final byte [] data = "bar".getBytes();
        r.getOutputStream().write(data, 0, data.length);
    }

    @Test(expected=ClientAbortException.class)
    public void flush() throws IOException {
        r.getOutputStream().flush();
    }

    @Test(expected=ClientAbortException.class)
    public void close() throws IOException {
        r.getOutputStream().close();
    }
}
