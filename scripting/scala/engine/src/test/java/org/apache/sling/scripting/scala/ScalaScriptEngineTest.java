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

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.naming.NamingException;
import javax.script.ScriptException;

import org.apache.sling.scripting.scala.interpreter.InterpreterException;
import org.apache.sling.scripting.scala.interpreter.JcrFS;
import org.apache.sling.scripting.scala.interpreter.ScalaBindings;
import org.apache.sling.scripting.scala.interpreter.JcrFS.JcrNode;

import scala.tools.nsc.io.AbstractFile;

public class ScalaScriptEngineTest extends ScalaTestBase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testEvalString() throws ScriptException, InvocationTargetException {
        String code = "print(1 + 2)";
        assertEquals("3", evalScala(code));
    }

    public void testEvalError() throws InvocationTargetException {
        String code = "syntax error";
        try {
            evalScala(code);
            assertTrue("Expecting ScriptException", false);
        }
        catch (ScriptException e) {
            // expected
        }
    }

    public void testError() {
        String err = "Some error here";
        String code = "throw new Error(\"" + err + "\")";
        try {
            evalScala(code);
            assertTrue("Expecting InvocationTargetException", false);
        }
        catch (ScriptException e) {
            Throwable inner = e.getCause();
            assertEquals("Inner exception is InterpreterException", InterpreterException.class, inner.getClass());
            inner = inner.getCause();
            assertEquals("Inner inner exception is java.lang.Error", Error.class, inner.getClass());
            assertEquals("Inner inner exception message is \"" + err + "\"", err, inner.getMessage());
        }
    }

    public void testEvalScript() throws URISyntaxException, IOException, ScriptException,
            InvocationTargetException {

        assertEquals("3", evalScalaScript("simple.scs"));
    }

    public void testNodeAccess() throws RepositoryException, NamingException, ScriptException, InvocationTargetException {
        Node n = getTestRootNode();
        String code = "print(n.getPath)";
        ScalaBindings bindings = new ScalaBindings();
        bindings.put("n", n, Node.class);
        assertEquals(n.getPath(), evalScala(code, bindings));
    }

    public void testEvalJcr() throws RepositoryException, NamingException, ScriptException {
        JcrNode appDir = JcrFS.create(getAppNode());
        AbstractFile srcDir = appDir.subdirectoryNamed("srcdir");

        ScalaBindings bindings = new ScalaBindings();
        Date time = Calendar.getInstance().getTime();
        bindings.put("msg", "Hello world", String.class);
        bindings.put("time", time, Date.class);

        AbstractFile src = srcDir.fileNamed("Testi");
        PrintWriter writer = new PrintWriter(src.output());
        writer.print("print(msg + \": \" + time)");
        writer.close();

        for(String name : new String[]{"org.apache.sling.scripting.scala.interpreter.Testi", "Testi"}) {
            evalScala(name, src, bindings);
        }
    }

}
