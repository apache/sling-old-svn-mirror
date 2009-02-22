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
package org.apache.sling.scripting.scala.engine;

import static org.apache.sling.scripting.scala.engine.ExceptionHelper.initCause;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.jcr.Node;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.apache.sling.scripting.scala.interpreter.JcrFS;
import org.apache.sling.scripting.scala.interpreter.ScalaBindings;
import org.apache.sling.scripting.scala.interpreter.ScalaInterpreter;
import org.apache.sling.scripting.scala.interpreter.JcrFS.JcrNode;
import org.slf4j.Logger;

import scala.tools.nsc.io.AbstractFile;
import scala.tools.nsc.reporters.Reporter;

public class ScalaScriptEngine extends AbstractSlingScriptEngine {
    public static final String NL = System.getProperty("line.separator");

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final ScalaInterpreter interpreter;

    public ScalaScriptEngine(ScalaInterpreter interpreter, ScriptEngineFactory scriptEngineFactory) {
        super(scriptEngineFactory);
        this.interpreter = interpreter;
    }

    public Object eval(Reader scriptReader, final ScriptContext context) throws ScriptException {
        try {
            SlingBindings bindings = getBindings(context);
            SlingScriptHelper scriptHelper = bindings.getSling();
            if (scriptHelper == null) {
                throw new IllegalArgumentException("Bindings does not contain script helper object");
            }

            TypeHints typeHints = new TypeHints(bindings);
            final ScalaBindings scalaBindings = new ScalaBindings();
            for (Object name : bindings.keySet()) {
                scalaBindings.put((String) name, bindings.get(name), typeHints.get(name));
            }

            final JcrNode script = getScriptSource(scriptHelper);
            final long scriptMod = script.lastModified();
            final String scriptName = getScriptName(scriptHelper);

            Boolean outDated = readLocked(new Callable<Boolean>() {
                public Boolean call() throws Exception {
                    return isOutDated(scriptMod, scriptName);
                }
            });

            if (outDated) {
                Reporter result = writeLocked(new Callable<Reporter>() {
                    public Reporter call() throws Exception {
                        return isOutDated(scriptMod, scriptName)
                            ? interpreter.compile(scriptName, script, scalaBindings)
                            : null;
                    }
                });

                if (result != null && result.hasErrors()) {
                    throw new ScriptException(result.toString());
                }
            }

            Reporter result = readLocked(new Callable<Reporter>() {
                public Reporter call() throws Exception {
                    OutputStream outputStream = getOutputStream(context);
                    Reporter result = interpreter.execute(scriptName, scalaBindings, getInputStream(context),
                            outputStream);
                    outputStream.flush();
                    return result;
                }
            });
            if (result.hasErrors()) {
                throw new ScriptException(result.toString());
            }
        }
        catch (Exception e) {
            if (e instanceof ScriptException) {
                throw (ScriptException)e;
            }
            else {
                throw initCause(new ScriptException("Error executing script"), e);
            }
        }
        return null;
    }

    // -----------------------------------------------------< private >---

    private boolean isOutDated(long scriptMod, String scriptName) {
        if (scriptMod == 0) {
            return true;
        }

        AbstractFile clazz = interpreter.getClassFile(scriptName);
        if (clazz == null) {
            return true;
        }

        long clazzMod = clazz.lastModified();
        if (clazzMod == 0) {
            return true;
        }

        return scriptMod > clazzMod;
    }

    private <T> T readLocked(Callable<T> thunk) throws Exception {
        rwLock.readLock().lock();
        try {
            return thunk.call();
        }
        finally {
            rwLock.readLock().unlock();
        }
    }

    private <T> T writeLocked(Callable<T> thunk) throws Exception {
        rwLock.writeLock().lock();
        try {
            return thunk.call();
        }
        finally {
            rwLock.writeLock().unlock();
        }
    }

    @SuppressWarnings("unchecked")
    private static SlingBindings getBindings(ScriptContext context) {
        SlingBindings bindings = new SlingBindings();
        bindings.putAll(context.getBindings(ScriptContext.ENGINE_SCOPE));
        return bindings;
    }

    private static JcrNode getScriptSource(SlingScriptHelper scriptHelper) {
        SlingScript script = scriptHelper.getScript();
        Node scriptNode = script.getScriptResource().adaptTo(Node.class);
        return JcrFS.create(scriptNode);
    }

    private static String getScriptName(SlingScriptHelper scriptHelper) {
        String path = scriptHelper.getScript().getScriptResource().getPath();
        if (path.endsWith(".scala")) {
            path = path.substring(0, path.length() - 6);
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        String[] parts = path.split("/");
        StringBuilder scriptName = new StringBuilder();
        scriptName.append(makeIdentifier(parts[0]));
        for (int k = 1; k < parts.length; k++) {
            scriptName.append('.').append(makeIdentifier(parts[k]));
        }

        return scriptName.toString();
    }

    /**
     * Converts the given identifier to a legal Java/Scala identifier
     * @param identifier Identifier to convert
     * @return Legal Java/Scala identifier corresponding to the given identifier
     */
    private static final String makeIdentifier(String identifier) {
        StringBuffer id = new StringBuffer(identifier.length());
        if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
            id.append('_');
        }
        for (int i = 0; i < identifier.length(); i++) {
            char ch = identifier.charAt(i);
            if (Character.isJavaIdentifierPart(ch) && ch != '_') {
                id.append(ch);
            }
            else if (ch == '.') {
                id.append('_');
            }
            else {
                id.append(mangleChar(ch));
            }
        }
        if (isKeyword(id.toString())) {
            id.append('_');
        }
        return id.toString();
    }

    /**
     * Mangle the specified character to create a legal Java/Scala class name.
     */
    private static final String mangleChar(char ch) {
        char[] result = new char[5];
        result[0] = '_';
        result[1] = Character.forDigit((ch >> 12) & 0xf, 16);
        result[2] = Character.forDigit((ch >> 8) & 0xf, 16);
        result[3] = Character.forDigit((ch >> 4) & 0xf, 16);
        result[4] = Character.forDigit(ch & 0xf, 16);
        return new String(result);
    }

    @SuppressWarnings("serial")
    private static final Set<String> KEYWORDS = new HashSet<String>() {{
        add("abstract"); add("assert"); add("boolean"); add("break"); add("byte"); add("case"); add("catch");
        add("char"); add("class"); add("const"); add("continue"); add("default"); add("do"); add("double");
        add("else"); add("enum"); add("extends"); add("final"); add("finally"); add("float"); add("for");
        add("goto"); add("if"); add("implements"); add("import"); add("instanceof"); add("int");
        add("interface"); add("long"); add("native"); add("new"); add("package"); add("private");
        add("protected"); add("public"); add("return"); add("short"); add("static"); add("strictfp");
        add("super"); add("switch"); add("synchronized"); add("this"); add("throws"); add("transient");
        add("try"); add("void"); add("volatile"); add("while"); add("true"); add("false"); add("null");
        add("forSome"); add("type"); add("var"); add("val"); add("def"); add("with"); add("yield");
        add("match"); add("implicit"); add("lazy"); add("override"); add("sealed"); add("trait");
        add("object");
    }};

    /**
     * Test whether the argument is a Java keyword
     */
    private static boolean isKeyword(String token) {
        return KEYWORDS.contains(token);
    }

    private static InputStream getInputStream(final ScriptContext context) {
        return new InputStream() {
            private final Reader reader = context.getReader();

            @Override
            public int read() throws IOException {
                return reader.read();
            }
        };
    }

    private static OutputStream getOutputStream(final ScriptContext context) {
        return new OutputStream() {
            private final Writer writer = context.getWriter();

            @Override
            public void write(int b) throws IOException {
                writer.write(b);
            }
        };
    }

    // todo fix: redirect stdErr when Scala supports it
    @SuppressWarnings("unused")
    private static OutputStream getErrorStream(final ScriptContext context) {
        return new OutputStream() {
            private final Writer writer = context.getErrorWriter();

            @Override
            public void write(int b) throws IOException {
                writer.write(b);
            }
        };
    }

    @SuppressWarnings("serial")
    private static class TypeHints extends HashMap<String, Class<?>> {
        public static final Class<SlingHttpServletRequest> REQUEST_TYPE = SlingHttpServletRequest.class;
        public static final Class<SlingHttpServletResponse> RESPONSE_TYPE = SlingHttpServletResponse.class;
        public static final Class<Reader> READER_TYPE = Reader.class;
        public static final Class<SlingScriptHelper> SLING_TYPE = SlingScriptHelper.class;
        public static final Class<Resource> RESOURCE_TYPE = Resource.class;
        public static final Class<PrintWriter> OUT_TYPE = PrintWriter.class;
        public static final Class<Boolean> FLUSH_TYPE = Boolean.class;
        public static final Class<Logger> LOG_TYPE = Logger.class;
        public static final Class<Node> NODE_TYPE = Node.class;

        private static final java.util.Map<String, Class<?>> TYPES = new HashMap<String, Class<?>>() {{
            put(SlingBindings.REQUEST, REQUEST_TYPE);
            put(SlingBindings.RESPONSE, RESPONSE_TYPE);
            put(SlingBindings.READER, READER_TYPE);
            put(SlingBindings.SLING, SLING_TYPE);
            put(SlingBindings.RESOURCE, RESOURCE_TYPE);
            put(SlingBindings.OUT, OUT_TYPE);
            put(SlingBindings.FLUSH, FLUSH_TYPE);
            put(SlingBindings.LOG, LOG_TYPE);
            put("currentNode", NODE_TYPE);
        }};

        public TypeHints(SlingBindings bindings) {
            super();
            for (Object name : bindings.keySet()) {
                setType((String) name, TYPES.get(name));
            }
        }

        public void setType(String name, Class<?> type) {
            if (type != null) {
                put(name, type);
            }
        }

        public Class<?> getType(String name) {
            Class<?> c = get(name);
            return c == null ? Object.class : c;
        }

        /**
         * Compile time assertion enforcing type safety
         */
        @SuppressWarnings("unused")
        private static class CompileTimeAssertion {
            static {
                SlingBindings b = new SlingBindings();
                b.setRequest(REQUEST_TYPE.cast(null));
                b.setResponse(RESPONSE_TYPE.cast(null));
                b.setReader(READER_TYPE.cast(null));
                b.setSling(SLING_TYPE.cast(null));
                b.setResource(RESOURCE_TYPE.cast(null));
                b.setOut(OUT_TYPE.cast(null));
                b.setFlush(FLUSH_TYPE.cast(null));
                b.setLog(LOG_TYPE.cast(null));
            }
        }

    }

}


