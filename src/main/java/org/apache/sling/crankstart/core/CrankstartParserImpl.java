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
package org.apache.sling.crankstart.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.sling.crankstart.api.CrankstartCommandLine;
import org.apache.sling.crankstart.api.CrankstartParser;

/** Default crankstart parser */
public class CrankstartParserImpl implements CrankstartParser {

    @Override
    public Iterator<CrankstartCommandLine> parse(Reader r) throws IOException {
        return new CmdIterator(r, this);
    }
    
    protected String getVariable(String name) {
        final StringBuilder sb = new StringBuilder();
        return sb.append("CRANKSTART_VAR_NOT_FOUND(").append(name).append(")").toString();
    }
}

class ParserException extends RuntimeException {
    private static final long serialVersionUID = 4156681962159138482L;

    ParserException(String reason) {
        super(reason);
    }
    
    ParserException(String reason, Throwable cause) {
        super(reason, cause);
    }
}

class CmdIterator implements Iterator<CrankstartCommandLine> {

    private String line;
    private final BufferedReader input;
    private final CrankstartParserImpl parser;
    private final Pattern varPattern = Pattern.compile("\\$\\{([a-zA-Z0-9\\._]+)\\}");
    
    CmdIterator(Reader r, CrankstartParserImpl parser) throws IOException {
        input = new BufferedReader(r);
        this.parser = parser;
        takeLine();
    }
    
    private String takeLine() throws IOException {
        final String result = line;
        line = input.readLine();
        while(line != null && ignore(line)) {
            line = input.readLine();
        }
        return injectVariables(result);
    }
    
    private String injectVariables(String line) {
        if(line == null) {
            return null;
        }
        final StringBuffer b = new StringBuffer();
        final Matcher m = varPattern.matcher(line);
        while(m.find()) {
            m.appendReplacement(b, getValue(m.group(1)));
        }
        m.appendTail(b);
        return b.toString();
    }
    
    private String getValue(String variable) {
        return parser.getVariable(variable);
    }
    
    private boolean ignore(String line) {
        if(line == null) {
            return false;
        }
        line = line.trim();
        return empty(line) || line.startsWith(("#"));
    }
    
    private boolean isVerb() {
        return line != null && line.length() > 0 && !Character.isWhitespace(line.charAt(0));
    }
    
    @Override
    public boolean hasNext() {
        return line != null;
    }
    
    @Override
    public CrankstartCommandLine next() {
        
        // Command must start with a verb, optionally followed
        // by properties
        if(!isVerb()) {
            throw new ParserException("Expecting verb, current line is " + line);
        }
        
        CrankstartCommandLine result = null;
        
        try {
            // Parse verb and qualifier from first line
            final String [] firstLine= takeLine().split(" ");
            final String verb = firstLine[0];
            final StringBuilder qualifier = new StringBuilder();
            for(int i=1; i < firstLine.length; i++) {
                if(qualifier.length() > 0) {
                    qualifier.append(' ');
                }
                qualifier.append(firstLine[i]);
            }
            
            // Parse properties from optional indented lines
            // that follow verb line
            Dictionary<String, Object> props = null;
            while(line != null && !isVerb()) {
                if(props == null) {
                    props = new Hashtable<String, Object>();
                }
                addProperty(props, takeLine());
            }
            result = new CrankstartCommandLine(verb, qualifier.toString(), props);
        } catch(IOException ioe) {
            line = null;
            throw new ParserException("IOException in takeLine()", ioe);
        }
        return result;
    }
    
    private static boolean empty(String str) {
        return str == null || str.trim().length() == 0;
    }
    
    private void addProperty(Dictionary<String, Object> props, String line) throws ParserException {
        if(line == null) {
            return;
        }
        final int equalsPos = line.indexOf('=');
        final String key = line.substring(0, equalsPos).trim();
        final String value = line.substring(equalsPos + 1).trim();
        if(empty(key) || empty(value)) {
            throw new ParserException("Invalid property line [" + line + "]");
        }
        
        // If we already have a value with the same name, make that an array
        Object o = props.get(key);
        if(o == null) {
            props.put(key, value);
        } else if(o instanceof String[]) {
            String [] a = (String [])o;
            a = Arrays.copyOf(a, a.length + 1);
            a[a.length - 1] = value;
            props.put(key, a);
        } else {
            String [] a = new String[2];
            a[0] = (String)o;
            a[1] = value;
            props.put(key, a);
        }
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}

    
    