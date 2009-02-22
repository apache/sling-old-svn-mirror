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
package org.apache.sling.scripting.scala;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.net.URL;

import javax.jcr.Node;
import javax.script.ScriptException;

import org.apache.sling.commons.testing.jcr.RepositoryTestBase;
import org.apache.sling.scripting.scala.engine.BacklogReporter;
import org.apache.sling.scripting.scala.interpreter.Bindings;
import org.apache.sling.scripting.scala.interpreter.InterpreterException;
import org.apache.sling.scripting.scala.interpreter.JcrFS;
import org.apache.sling.scripting.scala.interpreter.ScalaBindings;
import org.apache.sling.scripting.scala.interpreter.ScalaInterpreter;
import org.apache.sling.scripting.scala.interpreter.JcrFS.JcrNode;

import scala.tools.nsc.Settings;
import scala.tools.nsc.io.AbstractFile;
import scala.tools.nsc.reporters.Reporter;

public class ScalaTestBase extends RepositoryTestBase {
    public static final String NL = System.getProperty("line.separator");

    protected static final String SCRIPT_PATH = "/scripts/";

    private Node appNode;
    private ScalaInterpreter interpreter;
    private ByteArrayOutputStream interpreterOut;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Node testRoot = getTestRootNode();
        appNode = testRoot.addNode("app", "nt:folder");
        getSession().save();

        Settings settings = new Settings();
        String testCp = System.getProperty("surefire.test.class.path");
        String javaCp = System.getProperty("java.class.path");
        settings.classpath().v_$eq(testCp != null ? testCp : javaCp);
        JcrNode appDir = JcrFS.create(appNode);
        AbstractFile outDir = appDir.subdirectoryNamed("outdir");
        interpreter = new ScalaInterpreter(settings, new BacklogReporter(settings), outDir);
        interpreterOut = new ByteArrayOutputStream();
    }

    @Override
    protected void tearDown() {
        interpreter = null;
    }

    protected Node getAppNode() {
        return appNode;
    }

    protected String getScript(String scriptName) throws URISyntaxException, IOException {
        URL url = getClass().getResource(SCRIPT_PATH + scriptName);
        BufferedReader reader = new BufferedReader(new FileReader(new File(url.toURI())));

        StringBuilder script = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            script.append(line).append(NL);
        }

        return script.toString();
    }

    protected String evalScala(String code) throws ScriptException {
        Bindings bindings = new ScalaBindings();
        return evalScala(createScriptName(), code, bindings);
    }

    protected String evalScala(String code, Bindings bindings) throws ScriptException {
        return evalScala(createScriptName(), code, bindings);
    }

    protected String evalScala(String name, String code, Bindings bindings) throws ScriptException {
        try {
            interpreterOut.reset();
            Reporter result = interpreter.interprete(name, code, bindings, null, interpreterOut);
            if (result.hasErrors()) {
                throw new ScriptException(result.toString());
            }
            return interpreterOut.toString();
        }
        catch (InterpreterException e) {
            throw new ScriptException(e);
        }
    }

    protected String evalScalaScript(String scriptName) throws URISyntaxException, IOException,
            ScriptException, InvocationTargetException {

        String code = getScript(scriptName);
        return evalScala(code);
    }

    protected String evalScalaScript(String scriptName, Bindings bindings)
            throws URISyntaxException, IOException, ScriptException {

        String code = getScript(scriptName);
        return evalScala(scriptName, code, bindings);
    }

    protected String evalScala(String name, AbstractFile src, ScalaBindings bindings) throws ScriptException {
        try {
            interpreterOut.reset();
            Reporter result = interpreter.interprete(name, src, bindings, null, interpreterOut);
            if (result.hasErrors()) {
                throw new ScriptException(result.toString());
            }
            return interpreterOut.toString();
        }
        catch (InterpreterException e) {
            throw new ScriptException(e);
        }
    }

    protected String createScriptName() {
        return "scalaTest";
    }

}
