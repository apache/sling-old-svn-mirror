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

import java.io.ByteArrayOutputStream
import javax.script.ScriptException
import scala.tools.nsc.Settings
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.reporters.Reporter
import org.apache.sling.scripting.scala.BacklogReporter

/**
 * Helper class for evaluating Scala scripts. 
 */
class InterpreterHelper(val srcDir: AbstractFile, val outDir: AbstractFile) {
  require(srcDir != null)
  require(outDir != null)
  
  val interpreter: ScalaInterpreter = createInterpreter
  
  private val interpreterOut = new ByteArrayOutputStream

  @throws(classOf[ScriptException]) 
  def eval(name: String, code: String, bindings: Bindings): String = {
    try {
      interpreterOut.reset
      val result = interpreter.interprete(name, code, bindings, null, interpreterOut)
      if (result.hasErrors) throw new ScriptException(result.toString())
      interpreterOut.toString
    }
    catch {
      case e: Exception => throw new ScriptException(e)
    }
  }

  @throws(classOf[ScriptException]) 
  def eval(name: String, src: AbstractFile, bindings: Bindings) = {
    try {
      interpreterOut.reset
      val result = interpreter.interprete(name, src, bindings, null, interpreterOut)
      if (result.hasErrors) throw new ScriptException(result.toString())
      interpreterOut.toString
    }
    catch {
      case e: Exception => throw new ScriptException(e)
    }
  }

  // -----------------------------------------------------< protected >---
  
  protected def getSettings: Settings = new Settings();
  protected def getClasspath: String = System.getProperty("java.class.path")
  protected def getReporter(settings: Settings): Reporter = new BacklogReporter(settings); 

  protected def createInterpreter: ScalaInterpreter = {
    val settings = getSettings
    settings.classpath.value = getClasspath
    settings.outputDirs.setSingleOutput(outDir)
    new ScalaInterpreter(settings, getReporter(settings))
  }

}