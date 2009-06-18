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
package org.apache.sling.scripting.jst;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/** Copy the ECT script to the output, filtering <script>
 *  tags on the way (see SLING-114 comment 10/Dec/07 01:00 AM).
 *  To simplify things, assumes each out.write statement is on
 *  its own line in input.
 */
class ScriptFilteredCopy {

    /** Read r line-by-line, process lines that need to be
     *  modified, and write results to w
     */
    void copy(Reader r, Writer w) throws IOException {
        final BufferedReader br = new BufferedReader(r);
        String line = null;
        while( (line = br.readLine()) != null) {
            if(copyLine(line)) {
                final String toWrite = processLine(line);
                w.write(toWrite, 0, toWrite.length());
                w.write('\n');
            }
        }
    }
    
    protected boolean copyLine(String line) {
        return true;
    }

    /** Transform lines that look like
     *  <pre>
     *      out.write("something and a <script> tag");
     *  </pre>
     *
     *  Into
     *   <pre>
     *      out.write("something and a <");
     *      out.write("script> tag");
     *   </pre>
     *
     *  To work around browsers problems when they
     *  see a </script> tag in a String.
     */
    String processLine(String line) {
        if(line.startsWith("out.write(") && line.endsWith("\");")) {
            return line.replaceAll("script>","\");\nout.write(\"script>");
        }
        return line;
    }
}
