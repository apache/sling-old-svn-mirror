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

import java.util.Collections
import java.util.List
import javax.script.{ScriptException, ScriptEngine, ScriptEngineFactory, ScriptContext}
import org.apache.sling.scripting.scala.interpreter.ScalaInterpreter
import org.slf4j.LoggerFactory
import scala.tools.nsc.Settings
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.reporters.Reporter

object ScalaScriptEngineFactory {
  private val log = LoggerFactory.getLogger(classOf[ScalaScriptEngineFactory]);
  private val NL = System.getProperty("line.separator");
  
  val SCALA_SETTINGS = "scala.settings"
  val SCALA_REPORTER = "scala.reporter"
  val SCALA_CLASSPATH_X = "scala.classpath.x"

  val ENGINE_NAME = "Scala Scripting Engine"
  val LANGUAGE_VERSION = "2.8.1"
  val ENGINE_VERSION = "0.9/scala " + LANGUAGE_VERSION
  val EXTENSIONS = Collections.singletonList("scala")
  val LANGUAGE_NAME = "Scala"
  val MIME_TYPES = Collections.singletonList("application/x-scala")
  val NAMES = Collections.singletonList("scala")
}

/**
 * JSR 223 compliant {@link ScriptEngineFactory} for Scala.
 * {@link ScriptInfo} and {@link SettingsProvider} may be used to parametrize
 * this factory. When running inside an OSGi container, ScriptInfo and 
 * SettingsProvider are looked up and injected by the Service Component Runtime.
 */
class ScalaScriptEngineFactory extends ScriptEngineFactory {
  import ScalaScriptEngineFactory._

  @volatile
  private var scriptInfo: ScriptInfo = 
    new AbstractScriptInfo{}

  @volatile
  private var settingsProvider: SettingsProvider = 
    new AbstractSettingsProvider{}
  
  private var scalaInterpreter: ScalaInterpreter = null;

  // -----------------------------------------------------< ScriptEngineFactory >---
  
  def getEngineName: String =  ENGINE_NAME
  def getEngineVersion: String = ENGINE_VERSION
  def getExtensions: List[String] = EXTENSIONS
  def getLanguageName: String = LANGUAGE_NAME
  def getLanguageVersion: String = LANGUAGE_VERSION
  def getMimeTypes: List[String] = MIME_TYPES
  def getNames: List[String] = NAMES
  
  def getParameter(key: String): String = key.toUpperCase match {
    case ScriptEngine.ENGINE => getEngineName
    case ScriptEngine.ENGINE_VERSION => getEngineVersion
    case ScriptEngine.NAME => getNames.get(0)
    case ScriptEngine.LANGUAGE =>  getLanguageName
    case ScriptEngine.LANGUAGE_VERSION => getLanguageVersion
    case "threading" => "multithreaded" 
    case _ => null
  }
  
  def getMethodCallSyntax(obj: String, method: String, args: String*): String =
    obj + "." + method + "(" + args.mkString(",") + ")"

  def getOutputStatement(toDisplay: String): String =
    "println(\""+ toDisplay+ "\")"

  def getProgram(statements: String*): String = {
    def packageOf(className: String ) = {
      val i = className.lastIndexOf('.')
      if (i >= 0) className.substring(0, i)
      else null
    }

    def classOf(className: String) = {
      val i = className.lastIndexOf('.')
      if (i == className.length()) ""
      else className.substring(i + 1)
    }
    
    val qClassName = scriptInfo.getDefaultScriptClass
    val packageName = packageOf(qClassName);
    val className = classOf(qClassName);

    "package " + packageName + " {" + NL +
    "  class " + className + "(args: " + className + "Args) {" + NL +
    statements.mkString(NL) + 
    "  }" + NL + 
    "}" + NL;
  }

  def getScriptEngine: ScriptEngine =  
    new ScalaScriptEngine(this, scriptInfo)
  
  // -----------------------------------------------------< SCR integration >---

  def setScriptInfo(scriptInfo: ScriptInfo) {
    if (scriptInfo == null) 
      throw new IllegalArgumentException("ScriptInfo may not be null")

    if (scriptInfo != this.scriptInfo) 
      this.scriptInfo = scriptInfo
  }

  protected def unsetScriptInfo(scriptInfo: ScriptInfo) {
    this.scriptInfo = new AbstractScriptInfo{}
  }

  def getScriptInfoProvider: ScriptInfo = scriptInfo

  def setSettingsProvider(settingsProvider: SettingsProvider) {
    if (settingsProvider == null) 
      throw new IllegalArgumentException("SettingsProvider may not be null")

    if (this.settingsProvider != settingsProvider) {
      this.settingsProvider = settingsProvider
      scalaInterpreter = null
    }
  }

  protected def unsetSettingsProvider(settingsProvider: SettingsProvider) {
    this.settingsProvider = new AbstractSettingsProvider{}
  }

  def getSettingsProvider: SettingsProvider = settingsProvider

  // -----------------------------------------------------< private >---

  @throws(classOf[ScriptException])
  def getScalaInterpreter(context: ScriptContext): ScalaInterpreter = {
    context.getAttribute(SCALA_SETTINGS) match {
      case settings: Settings => 
        if (settingsProvider.setScalaSettings(settings)) scalaInterpreter = null
        
      case x => if (x != null) log.warn("Invalid settings: {}", x);
    }
    
    context.getAttribute(SCALA_REPORTER) match {
      case reporter: Reporter => 
        if (settingsProvider.setReporter(reporter)) scalaInterpreter = null
        
      case x => if (x != null) log.warn("Invalid reporter: {}", x);
    }
    
    context.getAttribute(SCALA_CLASSPATH_X) match {
      case classpath: Array[AbstractFile] => 
        if (settingsProvider.setClasspathX(classpath)) scalaInterpreter = null
      
      case x => if (x != null) log.warn("Invalid classpathx: {}", x);
    }
    
    if (scalaInterpreter == null) {
      log.debug("Creating Scala script engine from settings {}", settingsProvider);

        scalaInterpreter = new ScalaInterpreter(
            settingsProvider.getSettings,
            settingsProvider.getReporter,
            settingsProvider.getClasspathX);
    }

    return scalaInterpreter;
  }
  
}