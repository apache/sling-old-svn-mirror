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

import javax.script.ScriptException;
import org.apache.sling.scripting.scala.interpreter.ScalaInterpreter
import org.slf4j.LoggerFactory
import scala.tools.nsc.Settings
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.reporters.Reporter

/**
 * Abstract base implementation of a {@link SettingsProvider}. 
 */  
abstract class AbstractSettingsProvider extends SettingsProvider {
  protected var settings: Settings = {
    val settings = new Settings
    settings.usejavacp.value = true
    settings
  }
  
  protected var reporter: Reporter = createReporter(settings)
  protected var classpathX: Array[AbstractFile] = Array.empty

  @throws(classOf[ScriptException])
  def setScalaSettings(settings: Settings): Boolean = {
    if (settings == null) {
      throw new IllegalArgumentException(ScalaScriptEngineFactory.SCALA_SETTINGS + " must not be null");
    }
    
    if (this.settings != settings) {
      this.settings = settings
      reporter = createReporter(settings)
      true
    }
    else false
  }
  
  @throws(classOf[ScriptException])
  def getSettings: Settings = settings
  
  @throws(classOf[ScriptException])
  def setReporter(reporter: Reporter): Boolean = {
    if (reporter == null) {
      throw new IllegalArgumentException(ScalaScriptEngineFactory.SCALA_REPORTER + " must not be null");
    }
    
    if (this.reporter != null) {
      this.reporter = reporter
      true
    }
    else false
  }
  
  @throws(classOf[ScriptException])
  def getReporter: Reporter = reporter
  
  @throws(classOf[ScriptException])
  def setClasspathX(classpathX: Array[AbstractFile]): Boolean = {
    if (this.classpathX != classpathX) {
      this.classpathX = classpathX
      true
    }
    else false
  }

  @throws(classOf[ScriptException])
  def getClasspathX: Array[AbstractFile] = classpathX

  protected def createReporter(settings: Settings) = 
    new LogReporter(LoggerFactory.getLogger(classOf[ScalaInterpreter]), settings);
  
}