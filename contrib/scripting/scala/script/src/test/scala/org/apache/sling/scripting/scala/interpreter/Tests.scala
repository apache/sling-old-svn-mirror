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
package org.apache.sling.scripting.scala.interpreter

import junit.framework.Assert.{assertEquals, assertFalse, fail}
import java.io.PrintWriter
import javax.script.ScriptException

/**
 * Generic test cases. Implementors inject an InterpreterHelper instance.  
 */  
trait Tests {
  var interpreterHelper: InterpreterHelper
  
  def testEvalString {
    val code = "package a { class Testi(args: TestiArgs) { print(1 + 2) }}"
    assertEquals("3", interpreterHelper.eval("a.Testi", code, Bindings()))
  }

  def testEvalError {
    val code = "syntax error"
    try {
      interpreterHelper.eval("a.Testi", code, Bindings())
      fail("Expecting ScriptException")
    }
    catch {
      case _: ScriptException =>  // expected
    }
  }
  
  def testError {
    val err = "Some error here";
    val code = "package a { class Testi(args: TestiArgs) { throw new Error(\"" + err + "\") }}"
    try {
      interpreterHelper.eval("a.Testi", code, Bindings())
      fail("Expecting Exception")
    }
    catch {
      case e: ScriptException if err == e.getCause.getCause.getMessage => // expected
    }
  }

  def testScalaInterpreter {
    val bindings = Bindings()
    val time = java.util.Calendar.getInstance.getTime
    bindings.putValue("msg", "Hello world")
    bindings.putValue("time", time)
    val code = "package a { class Testi(args: TestiArgs) {import args._; print(msg + \": \" + time)}}"
    val result = interpreterHelper.eval("a.Testi", code, bindings)
    assertEquals("Hello world: " + time, result)
  }
  
  def testCompileExecute {
    val srcDir = interpreterHelper.srcDir
    val interpreter = interpreterHelper.interpreter

    val bindings = Bindings()
    val time = java.util.Calendar.getInstance.getTime
    bindings.putValue("msg", "Hello world")
    bindings.putValue("time", time)

    val code = "package a { class Testi(args: TestiArgs) {import args._; print(msg + \": \" + time)}}"
    val src = srcDir.fileNamed("Testi.scala")
    val writer = new PrintWriter(src.output)
    writer.print(code)
    writer.close

    val out = new java.io.ByteArrayOutputStream
    var result = interpreter.compile("a.Testi", src, bindings)
    if (result.hasErrors) {
      fail(result.toString)
    }

    result = interpreter.execute("a.Testi", bindings, None, Some(out))
    assertFalse(result.hasErrors)

    assertEquals("Hello world: " + time, out.toString)
  }

}