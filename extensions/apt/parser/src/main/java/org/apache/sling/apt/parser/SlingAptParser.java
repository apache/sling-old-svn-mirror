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
package org.apache.sling.apt.parser;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

/** APT parser interface for Sling */
public interface SlingAptParser {
    
    /** Set this option to "false" to disable the generation of
     *  an HTML page skeleton (html, head, body tags)
     */
    String OPT_HTML_SKELETON = "apt.parser.html.skeleton";
    
    /** Parse the given input, which must be in APT format, and
     *  write the HTML result to output.
     */
    void parse(Reader input, Writer output) throws IOException, SlingAptParseException;
    
    /** Parse the given input, which must be in APT format, and
     *  write the HTML result to output, taking specified options into account
     */
    void parse(Reader input, Writer output, Map<String, Object> options) throws IOException, SlingAptParseException;
}
