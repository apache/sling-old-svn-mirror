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
package org.apache.sling.apt.parser.internal;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.maven.doxia.module.apt.AptParseException;
import org.apache.maven.doxia.module.apt.AptParser;
import org.apache.maven.doxia.sink.Sink;
import org.apache.sling.apt.parser.SlingAptParseException;
import org.apache.sling.apt.parser.SlingAptParser;

/**
 * SlingAptParser implementation, provided as an SCR service
 *
 */
@Component()
@Service
@Property(name="service.description", value="Sling APT structured text parser")
public class SlingAptParserImpl implements SlingAptParser {

    private final MacroResolver macroProvider;

    public SlingAptParserImpl() {
        macroProvider = null;
    }

    SlingAptParserImpl(MacroResolver mp) {
        macroProvider = mp;
    }

    public void parse(Reader input, Writer output) throws IOException, SlingAptParseException {
        parse(input, output, null);
    }

    public void parse(Reader input, Writer output, Map<String, Object> options) throws IOException, SlingAptParseException {
        final Sink sink = new CustomAptSink(output, options);
        final AptParser parser = new CustomAptParser(macroProvider);
        try {
            parser.parse(input, sink);
        } catch(AptParseException ape) {
            throw new SlingAptParseExceptionImpl(ape);
        }
    }

}
