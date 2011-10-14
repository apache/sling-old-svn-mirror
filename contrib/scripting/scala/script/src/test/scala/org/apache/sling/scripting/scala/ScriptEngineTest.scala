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
package org.apache.sling.scripting.scala

import javax.script.{ ScriptEngineFactory, ScriptEngine, ScriptContext }
import java.io.StringWriter
import java.util.concurrent.{ TimeUnit, Executors, Callable, Future }
import junit.framework.Assert._
import junit.framework.TestCase

/**
 * JSR 223 compliance test
 *
 * <br>
 *
 * there is no direct reference to the ScalaScriptingEngine
 *
 */
class ScriptEngineTest extends TestCase {

  implicit def fun2Call[R](f: () => R) = new Callable[R] { def call: R = f() }

  def getScriptEngine(): ScriptEngine = {
    import scala.collection.JavaConversions._

    val factories = javax.imageio.spi.ServiceRegistry.lookupProviders(classOf[ScriptEngineFactory])
    val scalaEngineFactory = factories.find(_.getEngineName == "Scala Scripting Engine")

    scalaEngineFactory.map(_.getScriptEngine).getOrElse(
      throw new AssertionError("Scala Scripting Engine not found")
    )
  }

  /**
   *  tests a simple piece of code
   *
   *  this can be used as a reference for how to build a valid string that contains scala code for the ScalaScriptingEngine
   */
  def testSimple() {

    val scriptEngine: ScriptEngine = getScriptEngine();

    var code = new StringBuilder();
    code.append("package org.apache.sling.scripting.scala{");
    code.append("\n");
    code.append("class Script(args: ScriptArgs) {");
    code.append("\n");
    code.append("import args._");
    code.append("\n");
    code.append("println(\"output:\" + obj.saySomething()) ");
    code.append("\n");
    code.append("}}");

    val say = "hello";

    val b = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
    b.put("obj", new TestInject(say));

    val writer = new StringWriter();
    scriptEngine.getContext().setWriter(writer);

    scriptEngine.eval(code.toString(), b)
    assertEquals("output:" + say, writer.toString.trim())
  }

  /**
   * multi-threaded test
   *
   * the purpose of this test is to demonstrate the capabilities/faults that the current ScalaScriptingEngine implementation has.
   */
  //TODO get this test to work again
  /*def testMultipleThreads() {

    var code = new StringBuilder();
    code.append("package org.apache.sling.scripting.scala{");
    code.append("\n");
    code.append("class Script(args: ScriptArgs) {");
    code.append("\n");
    code.append("import args._");
    code.append("\n");
    code.append("println(\"output:\" + obj.saySomething()) ");
    code.append("\n");
    code.append("}}");

    val threads = 2;
    val operations = 100;
    val e = Executors.newFixedThreadPool(threads);
    var futures = List[Future[Boolean]]();

    for (i <- 0 to operations) {
      val say = i % 3 match {
        case 0 => "hello"
        case 1 => "you again"
        case 2 => "bye"
      }
      val c: Callable[Boolean] = () => buildSayCallable(code.toString, say);
      val f: Future[Boolean] = e.submit(c);
      futures = f :: futures;
    }
    e.shutdown();
    e.awaitTermination(120, TimeUnit.SECONDS);

    futures.foreach(f => {
      try {
        assertTrue(f.get(10, TimeUnit.SECONDS))
      } catch {
        case e: Exception => { e.printStackTrace; fail(e.getMessage); }
      }
    });
  }*/

  def buildSayCallable(code: String, say: String): Boolean = {

    val scriptEngine: ScriptEngine = getScriptEngine
    println("thread executing with engine: " + scriptEngine + ", say: " + say);

    val b = scriptEngine.getBindings(ScriptContext.ENGINE_SCOPE);
    b.put("obj", new TestInject(say));

    val writer = new StringWriter();
    scriptEngine.getContext().setWriter(writer);

    scriptEngine.eval(code.toString(), b)
    return "output:" + say == writer.toString.trim();
  };

  class TestInject(sayWhat: String) {
    def saySomething() = sayWhat;
  }

}

