/*******************************************************************************
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
 ******************************************************************************/
package org.apache.sling.scripting.sightly.impl.html.dom;

import java.io.CharArrayWriter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Tokenizes a snippet of characters into a structured tag/attribute name list.
 */
class TagTokenizer {
    /** Tag name buffer */
    private final CharArrayWriter tagName = new CharArrayWriter(30);

    /** Attribute name buffer */
    private final CharArrayWriter attName = new CharArrayWriter(30);

    /** Attribute value buffer */
    private final CharArrayWriter attValue = new CharArrayWriter(30);

    /** Internal property list */
    private final AttributeListImpl attributes = new AttributeListImpl();

    /** Parse state constant */
    private final static int START = 0;

    /** Parse state constant */
    private final static int TAG = START + 1;

    /** Parse state constant */
    private final static int NAME = TAG + 1;

    /** Parse state constant */
    private final static int INSIDE = NAME + 1;

    /** Parse state constant */
    private final static int ATTNAME = INSIDE + 1;

    /** Parse state constant */
    private final static int EQUAL = ATTNAME + 1;

    /** Parse state constant */
    private final static int ATTVALUE = EQUAL + 1;

    /** Parse state constant */
    private final static int STRING = ATTVALUE + 1;

    /** Parse state constant */
    private final static int ENDSLASH = STRING + 1;

    /** Parse state constant */
    private final static int END = ENDSLASH + 1;

    /** Parse state constant */
    private final static int BETWEEN_ATTNAME = END + 1;

    /** Quote character */
    private char quoteChar = '"';

    /** Flag indicating whether the tag scanned is an end tag */
    private boolean endTag;

    /** Flag indicating whether an ending slash was parsed */
    private boolean endSlash;

    /** temporary flag indicating if attribute has a value */
    private boolean hasAttributeValue;

    /**
     * Scan characters passed to this parser
     */
    public void tokenize(char[] buf, int off, int len) {
        reset();

        int parseState = START;

        for (int i = 0; i < len; i++) {
            char c = buf[off + i];

            switch (parseState) {
                case START:
                    if (c == '<') {
                        parseState = TAG;
                    }
                    break;
                case TAG:
                    if (c == '/') {
                        endTag = true;
                        parseState = NAME;
                    } else if (c == '"' || c == '\'') {
                        quoteChar = c;
                        parseState = STRING;
                    } else if (Character.isWhitespace(c)) {
                        parseState = INSIDE;
                    } else {
                        tagName.write(c);
                        parseState = NAME;
                    }
                    break;
                case NAME:
                    if (Character.isWhitespace(c)) {
                        parseState = INSIDE;
                    } else if (c == '"' || c == '\'') {
                        quoteChar = c;
                        parseState = STRING;
                    } else if (c == '>') {
                        parseState = END;
                    } else if (c == '/') {
                        parseState = ENDSLASH;
                    } else {
                        tagName.write(c);
                    }
                    break;
                case INSIDE:
                    if (c == '>') {
                        attributeEnded();
                        parseState = END;
                    } else if (c == '/') {
                        attributeEnded();
                        parseState = ENDSLASH;
                    } else if (c == '"' || c == '\'') {
                        attributeValueStarted();
                        quoteChar = c;
                        parseState = STRING;
                    } else if (c == '=') {
                        parseState = EQUAL;
                    } else if (!Character.isWhitespace(c)) {
                        attName.write(c);
                        parseState = ATTNAME;
                    }
                    break;
                case ATTNAME:
                    if (c == '>') {
                        attributeEnded();
                        parseState = END;
                    } else if (c == '/') {
                        attributeEnded();
                        parseState = ENDSLASH;
                    } else if (c == '=') {
                        parseState = EQUAL;
                    } else if (c == '"' || c == '\'') {
                        quoteChar = c;
                        parseState = STRING;
                    } else if (Character.isWhitespace(c)) {
                        parseState = BETWEEN_ATTNAME;
                    } else {
                        attName.write(c);
                    }
                    break;
                case BETWEEN_ATTNAME:
                    if (c == '>') {
                        attributeEnded();
                        parseState = END;
                    } else if (c == '/') {
                        attributeEnded();
                        parseState = ENDSLASH;
                    } else if (c == '"' || c == '\'') {
                        attributeValueStarted();
                        quoteChar = c;
                        parseState = STRING;
                    } else if (c == '=') {
                        parseState = EQUAL;
                    } else if (!Character.isWhitespace(c)) {
                        attributeEnded();
                        attName.write(c);
                        parseState = ATTNAME;
                    }
                    break;
                case EQUAL:
                    if (c == '>') {
                        attributeEnded();
                        parseState = END;
                    } else if (c == '"' || c == '\'') {
                        attributeValueStarted();
                        quoteChar = c;
                        parseState = STRING;
                    } else if (!Character.isWhitespace(c)) {
                        attributeValueStarted();
                        attValue.write(c);
                        parseState = ATTVALUE;
                    }
                    break;
                case ATTVALUE:
                    if (Character.isWhitespace(c)) {
                        attributeEnded();
                        parseState = INSIDE;
                    } else if (c == '"' || c == '\'') {
                        attributeEnded();
                        quoteChar = c;
                        parseState = STRING;
                    } else if (c == '>') {
                        attributeEnded();
                        parseState = END;
                    } else {
                        attValue.write(c);
                    }
                    break;
                case STRING:
                    if (c == quoteChar) {
                        attributeEnded();
                        parseState = INSIDE;
                    } else {
                        attValue.write(c);
                    }
                    break;
                case ENDSLASH:
                    if (c == '>') {
                        endSlash = true;
                        parseState = END;
                    } else if (c == '"' || c == '\'') {
                        quoteChar = c;
                        parseState = STRING;
                    } else if (c != '/' && !Character.isWhitespace(c)) {
                        attName.write(c);
                        parseState = ATTNAME;
                    } else {
                        parseState = INSIDE;
                    }
                    break;
                case END:
                    break;

            }
        }
    }

    /**
     * Return a flag indicating whether the tag scanned was an end tag
     * @return <code>true</code> if it was an end tag, otherwise
     *         <code>false</code>
     */
    public boolean endTag() {
        return endTag;
    }

    /**
     * Return a flag indicating whether an ending slash was scanned
     * @return <code>true</code> if an ending slash was scanned, otherwise
     *         <code>false</code>
     */
    public boolean endSlash() {
        return endSlash;
    }

    /**
     * Return the tagname scanned
     * @return tag name
     */
    public String tagName() {
        return tagName.toString();
    }

    /**
     * Return the list of attributes scanned
     * @return list of attributes
     */
    public AttributeList attributes() {
        return attributes;
    }

    /**
     * Reset the internal state of the tokenizer
     */
    private void reset() {
        tagName.reset();
        attributes.reset();
        endTag = false;
        endSlash = false;
    }

    /**
     * Invoked when an attribute ends
     */
    private void attributeEnded() {
        if (attName.size() > 0) {
            if (hasAttributeValue) {
                attributes.addAttribute(attName.toString(), attValue.toString(),
                        quoteChar);
            } else {
                attributes.addAttribute(attName.toString(), quoteChar);

            }
            attName.reset();
            attValue.reset();
            hasAttributeValue = false;
        }
    }

    /**
     * Invoked when an attribute value starts
     */
    private void attributeValueStarted() {
        hasAttributeValue = true;
    }

    /**
     * Retransfers the tokenized tag data into html again
     * @return the reassembled html string
     */
    public String toHtmlString() {
        StringBuffer sb = new StringBuffer();
        if (endTag) {
            sb.append("</" + tagName());
        } else {
            sb.append("<" + tagName());
            Iterator<String> attNames = attributes().attributeNames();
            while (attNames.hasNext()) {
                String attName = attNames.next();
                String attValue = attributes().getQuotedValue(attName);

                sb.append(" ");
                sb.append(attName);
                if (attValue != null) {
                    sb.append('=');
                    sb.append(attValue);
                }
            }
            if (endSlash) {
                sb.append(" /");
            }
        }
        sb.append(">");
        return sb.toString();
    }
}

/**
 * Internal implementation of an <code>AttributeList</code>
 */
class AttributeListImpl implements AttributeList {

    /**
     * Internal Value class
     */
    static class Value {

        /**
         * Create a new <code>Value</code> instance
         */
        public Value(char quoteChar, String value) {
            this.quoteChar = quoteChar;
            this.value = value;
        }

        /** Quote character */
        public final char quoteChar;

        /** Value itself */
        public final String value;

        /** String representation */
        private String stringRep;

        /**
         * @see Object#toString()
         */
        @Override
        public String toString() {
            if (stringRep == null) {
                stringRep = quoteChar + value + quoteChar;
            }
            return stringRep;
        }
    }

    /** Attribute/Value pair map with case insensitives names */
    private final Map<String, Value> attributes = new LinkedHashMap<String, Value>();

    /** Attribute names, case sensitive */
    private final Set<String> attributeNames = new LinkedHashSet<String>();

    /** Flag indicating whether this object was modified */
    private boolean modified;

    /**
     * Add an attribute/value pair to this attribute list
     */
    public void addAttribute(String name, String value, char quoteChar) {
        attributes.put(name.toUpperCase(), new Value(quoteChar, value));
        attributeNames.add(name);
    }

    /**
     * Add an attribute/value pair to this attribute list
     */
    public void addAttribute(String name, char quoteChar) {
        attributes.put(name.toUpperCase(), null);
        attributeNames.add(name);
    }

    /**
     * Empty this attribute list
     */
    public void reset() {
        attributes.clear();
        attributeNames.clear();
        modified = false;
    }

    /**
     * @see AttributeList#attributeCount
     */
    public int attributeCount() {
        return attributes.size();
    }

    /**
     * @see AttributeList#attributeNames
     */
    public Iterator<String> attributeNames() {
        return attributeNames.iterator();
    }

    /**
     * @see AttributeList#containsAttribute(String)
     */
    public boolean containsAttribute(String name) {
        return attributes.containsKey(name.toUpperCase());
    }

    /**
     * @see AttributeList#getValue(String)
     */
    public String getValue(String name) {
        Value value = getValueEx(name);
        if (value != null) {
            return value.value;
        }
        return null;
    }

    /**
     * @see AttributeList#getQuoteChar(java.lang.String)
     */
    public char getQuoteChar(String name) {
        Value value = getValueEx(name);
        if (value != null) {
            return value.quoteChar;
        }
        return 0;
    }

    /**
     * @see AttributeList#getQuotedValue(String)
     */
    public String getQuotedValue(String name) {
        Value value = getValueEx(name);
        if (value != null) {
            return value.toString();
        }
        return null;
    }

    /**
     * @see AttributeList#setValue(String, String)
     */
    public void setValue(String name, String value) {
        if (value == null) {
            removeValue(name);
        } else {
            Value old = getValueEx(name);
            if (old == null) {
                addAttribute(name, value, '"');
                modified = true;
            } else if (!old.value.equals(value)) {
                addAttribute(name, value, old.quoteChar);
                modified = true;
            }
        }
    }

    /**
     * @see AttributeList#removeValue(String)
     */
    public void removeValue(String name) {
        attributeNames.remove(name);
        attributes.remove(name.toUpperCase());
        modified = true;
    }

    /**
     * @see AttributeList#isModified
     */
    public boolean isModified() {
        return modified;
    }

    /**
     * Return internal value structure
     */
    protected Value getValueEx(String name) {
        return attributes.get(name.toUpperCase());
    }
}