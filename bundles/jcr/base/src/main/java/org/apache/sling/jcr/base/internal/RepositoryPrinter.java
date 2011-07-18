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
package org.apache.sling.jcr.base.internal;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;

import javax.jcr.Repository;

/**
 * The Printer Plugin
 */
public class RepositoryPrinter {

    private final Repository repository;

    private final String name;

    public RepositoryPrinter(final Repository repo, final Map<String, Object> props) {
        this.repository = repo;
        if ( repo.getDescriptor(Repository.REP_NAME_DESC) != null ) {
            name = "Repository " + repo.getDescriptor(Repository.REP_NAME_DESC);
        } else  if ( props.get("name") != null ) {
            name = "Repository " + props.get("name").toString();
        } else {
            this.name = "Repository @" + repo.hashCode();
        }
    }

    public String getTitle() {
        return this.name;
    }

    public Dictionary<String, Object> getProperties() {
        final Dictionary<String, Object> props = new Hashtable<String, Object>();
        props.put("felix.webconsole.label", this.name);
        props.put("felix.webconsole.title", this.name);
        props.put("felix.webconsole.configprinter.modes", "always");

        return props;
    }

    public void printConfiguration(final PrintWriter pw) {
        // try to get repository
        final Repository repo = this.repository;
        writeHeader(pw, "Repository Properties");
        final String[] keys = repo.getDescriptorKeys();
        Arrays.sort(keys);
        for (final String key : keys) {
            final String val = repo.getDescriptor(key);
            writeEntry(pw, key, val);
        }
    }

    private void writeHeader(final PrintWriter pw, final String value) {
        pw.print(value);
        pw.println(":");
    }

    private void writeEntry(final PrintWriter pw, final String key, final String value) {
        pw.print(key);
        pw.print(": ");
        if ( value != null ) {
            pw.println(value);
        } else {
            pw.println("-");
        }
    }
}
