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
package org.apache.sling.microsling.helpers.json;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

/** Dumps JCR Items as JSON data */
public class JsonItemWriter {
    final Set<String> propertyNamesToIgnore;

    /** Create a JsonItemWriter
     *  @param propertyNamesToIgnore if not null, a property having a name from this
     *  set of values is ignored
     */ 
    public JsonItemWriter(Set<String> propertyNamesToIgnore) {
        this.propertyNamesToIgnore = propertyNamesToIgnore;
    }

    /** Dump all Nodes of given NodeIterator in JSON */
    public void dump(NodeIterator it, PrintWriter out)
            throws RepositoryException {
        boolean first = true;
        out.println("[");
        while (it.hasNext()) {
            dumpSingleNode(it.nextNode(), out, 1, 0, first);
            first = false;
        }
        out.println("]");
    }

    /** Dump given node in JSON, optionally recursing into its child nodes */
    public void dump(Node node, PrintWriter out, int currentRecursionLevel,
            int maxRecursionLevels) throws RepositoryException {
        PropertyIterator props = node.getProperties();

        boolean first = true;

        // the node's actual properties
        while (props.hasNext()) {
            Property prop = props.nextProperty();

            if (propertyNamesToIgnore!=null && propertyNamesToIgnore.contains(prop.getName())) {
                continue;
            }

            if (!prop.getDefinition().isMultiple()) {
                if (!(prop.getType() == PropertyType.BINARY)) {
                    writeProperty(out, currentRecursionLevel, prop.getName(),
                            stringValue(prop, prop.getValue()), first);
                    first = false;
                }
            } else {
                if (!(prop.getType() == PropertyType.BINARY)) {
                    out.print(first ? "" : ",\r\n");
                    first = false;
                    indent(out, currentRecursionLevel);
                    Value[] vals = prop.getValues();
                    int i = 0;
                    boolean firstval = true;
                    out.print("\"" + prop.getName() + "\":[");
                    while (i < vals.length) {
                        if (!firstval) {
                            out.print(",");
                        }
                        firstval = false;
                        out.print("\"" + escape(stringValue(prop, vals[i]))
                                + "\"");
                        i++;
                    }
                    out.print("]");
                }
            }
        }
        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            Node childnode = nodes.nextNode();
            out.print(first ? "" : ",\r\n");
            first = false;
            dumpSingleNode(childnode, out, currentRecursionLevel, maxRecursionLevels, first);
        }
    }

    /** Dump a single node */
    private void dumpSingleNode(Node n, PrintWriter out,
            int currentRecursionLevel, int maxRecursionLevels, boolean first)
            throws RepositoryException {
        indent(out, currentRecursionLevel);
        out.print("\"" + n.getName() + "\":");
        out.println("{");
        if (maxRecursionLevels == 0
                || currentRecursionLevel + 1 < maxRecursionLevels) {
            dump(n, out, currentRecursionLevel + 1, maxRecursionLevels);
        }
        out.println("");
        indent(out, currentRecursionLevel + 1);
        out.println("}");
    }

    /**
     * <p/> escape Java Style.
     * </p>
     * 
     * @param str
     *            String to escape values in, may be null
     * @param escapeSingleQuotes
     *            escapes single quotes if <code>true</code>
     * @return the escaped string
     */
    private String escapeJavaStyleString(String str, boolean escapeSingleQuotes) {
        if (str == null) {
            return null;
        }
        try {
            StringWriter writer = new StringWriter(str.length() * 2);
            escapeJavaStyleString(writer, str, escapeSingleQuotes);
            return writer.toString();
        } catch (IOException ioe) {
            // this should never ever happen while writing to a StringWriter
            ioe.printStackTrace();
            return null;
        }
    }

    /**
     * <p/> escape Java Style.
     * </p>
     * 
     * @param out
     *            write to receieve the escaped string
     * @param str
     *            String to escape values in, may be null
     * @param escapeSingleQuote
     *            escapes single quotes if <code>true</code>
     * @throws IOException
     *             if an IOException occurs
     */
    private void escapeJavaStyleString(Writer out, String str,
            boolean escapeSingleQuote) throws IOException {
        if (out == null) {
            throw new IllegalArgumentException("The Writer must not be null");
        }
        if (str == null) {
            return;
        }
        int sz;
        sz = str.length();
        for (int i = 0; i < sz; i++) {
            char ch = str.charAt(i);

            // handle unicode
            if (ch > 0xfff) {
                out.write("\\u" + hex(ch));
            } else if (ch > 0xff) {
                out.write("\\u0" + hex(ch));
            } else if (ch > 0x7f) {
                out.write("\\u00" + hex(ch));
            } else if (ch < 32) {
                switch (ch) {
                case '\b':
                    out.write('\\');
                    out.write('b');
                    break;
                case '\n':
                    out.write('\\');
                    out.write('n');
                    break;
                case '\t':
                    out.write('\\');
                    out.write('t');
                    break;
                case '\f':
                    out.write('\\');
                    out.write('f');
                    break;
                case '\r':
                    out.write('\\');
                    out.write('r');
                    break;
                default:
                    if (ch > 0xf) {
                        out.write("\\u00" + hex(ch));
                    } else {
                        out.write("\\u000" + hex(ch));
                    }
                    break;
                }
            } else {
                switch (ch) {
                case '\'':
                    if (escapeSingleQuote) {
                        out.write('\\');
                    }
                    out.write('\'');
                    break;
                case '"':
                    out.write('\\');
                    out.write('"');
                    break;
                case '\\':
                    out.write('\\');
                    out.write('\\');
                    break;
                default:
                    out.write(ch);
                    break;
                }
            }
        }
    }

    /**
     * <p/> Returns an upper case hexadecimal <code>String</code> for the
     * given character.
     * </p>
     * 
     * @param ch
     *            The character to convert.
     * @return An upper case hexadecimal <code>String</code>
     */
    private String hex(char ch) {
        return Integer.toHexString(ch).toUpperCase();
    }

    /**
     * outputs spaces per currentRecursionLevel
     * 
     * @param out
     *            printwriter for indentation
     * @param currentRecursionLevel
     *            indentation currentRecursionLevel
     */
    private void indent(PrintWriter out, int currentRecursionLevel) {
        int i = 0;
        while (i++ < currentRecursionLevel) {
            out.print("  ");
        }
    }

    /**
     * escape String for JSON format
     */
    private String escape(String orig) {
        return escapeJavaStyleString(orig, false);
    }

    /**
     * write a single property
     */
    private void writeProperty(PrintWriter out, int indent, String name,
            String value, boolean first) {
        if (!first) {
            out.println(",");
        }
        if (indent > 0) {
            indent(out, indent);
        }
        out.print("\"" + name + "\":\"" + escape(value) + "\"");
    }

    /** Convert given Value to a String */
    private String stringValue(Property prop, Value value)
            throws RepositoryException {
        String result = null;

        if (prop.getType() == PropertyType.DATE) {
            // TODO more flexible format?
            final DateFormat fmt = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss 'GMT'Z", Locale.US);
            result = fmt.format(value.getDate().getTime());
        } else {
            result = value.getString();
        }

        return result;
    }

}
