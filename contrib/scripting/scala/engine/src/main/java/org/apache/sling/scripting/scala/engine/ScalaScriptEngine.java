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
import java.io.Reader;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.jcr.Node;
import javax.script.ScriptContext;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.api.scripting.SlingScript;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.scripting.api.AbstractSlingScriptEngine;
import org.apache.sling.scripting.scala.interpreter.Bindings;
import org.apache.sling.scripting.scala.interpreter.Bindings$;
import org.apache.sling.scripting.scala.interpreter.JcrFS;
import org.apache.sling.scripting.scala.interpreter.ScalaInterpreter;
import org.apache.sling.scripting.scala.interpreter.JcrFS.JcrNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import scala.tools.nsc.io.AbstractFile;
import scala.tools.nsc.reporters.Reporter;

public class ScalaScriptEngine extends AbstractSlingScriptEngine {
    private static final Logger log = LoggerFactory.getLogger(ScalaScriptEngine.class);

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

            final Bindings scalaBindings = Bindings$.MODULE$.apply();
            for (String name : bindings.keySet()) {
                if (name == null) {
                    log.debug("Bindings contain null key. skipping");
                    continue;
                }

                Object value = bindings.get(name);
                if (value == null) {
                    log.debug("{} has null value. skipping", name);
                    continue;
                }

                scalaBindings.putValue(makeIdentifier(name), value);
            }

            final JcrNode script = getScriptSource(scriptHelper);
            final long scriptMod = script.lastModified();
            final String scriptName = getScriptName(scriptHelper);

            // xxx: Scripts need to be compiled everytime.
            // The preamble for injecting the bindings into the script
            // dependes on the actual types of the bindings. So effectively
            // there is a specific script generated for each type of bindings.
            Reporter result = writeLocked(new Callable<Reporter>() {
                public Reporter call() throws Exception {
                    return interpreter.compile(scriptName, script, scalaBindings);
                }
            });
            if (result != null && result.hasErrors()) {
                throw new ScriptException(result.toString());
            }

            result = readLocked(new Callable<Reporter>() {
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
        String path = getRelativePath(scriptHelper.getScript().getScriptResource());
        if (path.endsWith(".scala")) {
            path = path.substring(0, path.length() - 6);
        }
        else if (path.endsWith(".scs")) {
            path = path.substring(0, path.length() - 4);
        }

        String[] parts = path.split("/");
        StringBuilder scriptName = new StringBuilder();
        scriptName.append(makeIdentifier(parts[0]));
        for (int k = 1; k < parts.length; k++) {
            scriptName.append('.').append(makeIdentifier(parts[k]));
        }

        return scriptName.toString();
    }

    private static String getRelativePath(Resource scriptResource) {
        String path = scriptResource.getPath();
        String[] searchPath = scriptResource.getResourceResolver().getSearchPath();

        for (int i = 0; i < searchPath.length; i++) {
            if (path.startsWith(searchPath[i])) {
                path = path.substring(searchPath[i].length());
                break;
            }
        }

        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
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

}


