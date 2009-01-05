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
package org.apache.sling.engine;

import java.io.IOException;
import java.io.Writer;

/** Response-related utilities */
public class ResponseUtil {
    
    private static class XmlEscapingWriter extends Writer {
        private final Writer target;
        
        XmlEscapingWriter(Writer target) {
            this.target = target;
        }

        @Override
        public void close() throws IOException {
            target.close();
        }

        @Override
        public void flush() throws IOException {
            target.flush();
        }

        @Override
        public void write(char[] buffer, int offset, int length) throws IOException {
            for(int i = offset; i < offset + length; i++) {
                write(buffer[i]);
            }
        }

        @Override
        public void write(char[] cbuf) throws IOException {
            write(cbuf, 0, cbuf.length);
        }

        @Override
        public void write(int c) throws IOException {
            if(c == '&') {
                target.write("&amp;");
            } else if(c == '<') {
                target.write("&lt;");
            } else if(c == '>') {
                target.write("&gt;");
            } else {
                target.write(c);
            }
        }

        @Override
        public void write(String str, int off, int len) throws IOException {
            write(str.toCharArray(), off, len);
        }

        @Override
        public void write(String str) throws IOException {
            write(str.toCharArray());
        }
    }
    
    /** Escape xml text */
    public static String escapeXml(String input) {
        if(input == null) {
            return null;
        }
        
        final StringBuffer b = new StringBuffer(input.length());
        for(int i = 0;i  < input.length(); i++) {
            final char c = input.charAt(i);
            if(c == '&') {
                b.append("&amp;");
            } else if(c == '<') {
                b.append("&lt;");
            } else if(c == '>') {
                b.append("&gt;");
            } else {
                b.append(c);
            }
        }
        return b.toString();
    }
    
    /** Return a Writer that writes escaped XML text to target
     */
    public static Writer getXmlEscapingWriter(Writer target) {
        return new XmlEscapingWriter(target);
    }
}
