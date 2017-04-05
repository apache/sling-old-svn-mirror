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
package org.apache.sling.repoinit.parser.impl;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import org.apache.sling.repoinit.parser.RepoInitParser;
import org.apache.sling.repoinit.parser.RepoInitParsingException;
import org.apache.sling.repoinit.parser.operations.Operation;
import org.osgi.service.component.annotations.Component;

/** ACL definitions parser service */
@Component(service=RepoInitParser.class)
public class RepoInitParserService implements RepoInitParser {

    @Override
    public List<Operation> parse(Reader r) throws RepoInitParsingException {
        try {
            return new RepoInitParserImpl(r).parse();
        } catch (ParseException pe) {
            throw new RepoInitParsingException(pe.getMessage(), pe);
        } finally {
            try {
                r.close();
            } catch(IOException ignore) {
            }
        }
    }
}
