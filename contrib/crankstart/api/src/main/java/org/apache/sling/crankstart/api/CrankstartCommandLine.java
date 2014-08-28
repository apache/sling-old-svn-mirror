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
package org.apache.sling.crankstart.api;

import java.util.Dictionary;
import java.util.Hashtable;

/** A command line read from a crankstart txt file */
public class CrankstartCommandLine {
    private final String verb;
    private final String qualifier;
    private final Dictionary<String, Object> properties;
    
    private static final Dictionary<String, Object> EMPTY_PROPERTIES = new Hashtable<String, Object>();
    
    public CrankstartCommandLine(String verb, String qualifier, Dictionary<String, Object> properties) {
        this.verb = verb;
        this.qualifier = qualifier;
        this.properties = properties == null ? EMPTY_PROPERTIES : properties;
    }
    
    @Override
    public String toString() {
        if(qualifier != null && qualifier.length() > 0) {
            return verb + " " + qualifier;
        } else {
            return verb;
        }
    }

    public String getVerb() {
        return verb;
    }

    public String getQualifier() {
        return qualifier;
    }

    public Dictionary<String, Object> getProperties() {
        return properties;
    }
}