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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;

import org.apache.sling.scripting.javascript.io.EspReader;

/** Generates javascript code by converting a JST script to
 *  client-side javascript code that renders the node. Meant
 *  to be used with the JstScriptEngine.      
 */
class JsCodeGenerator {

    /** Convert the script read on scriptReader to javascript code and 
     *  write the result to output using a BodyOnlyScriptFilteredCopy,
     *  to keep only the part that's relevant for the document body
     */
    void generateCode(Reader scriptReader, PrintWriter output) throws IOException {
        final EspReader er = new EspReader(scriptReader);
        er.setOutInitStatement("");
        output.println("out=document;");
        new BodyOnlyScriptFilteredCopy().copy(er,output);
    }
}
