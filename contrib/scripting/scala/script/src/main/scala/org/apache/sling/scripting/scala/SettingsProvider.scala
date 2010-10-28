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

import javax.script.ScriptException
import scala.tools.nsc.Settings
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.reporters.Reporter

/**
 * Provides compiler settings to the {@link ScalaScriptEngineFactory}. 
 */  
trait SettingsProvider {
  
  /**
   * @return true if the passed value differs from the current value
   */
  @throws(classOf[ScriptException])
  def setScalaSettings(value: Settings): Boolean
  
  @throws(classOf[ScriptException])
  def getSettings: Settings
  
  /**
   * @return true if the passed value differs from the current value
   */
  @throws(classOf[ScriptException])
  def setReporter(reporter: Reporter): Boolean
  
  @throws(classOf[ScriptException])
  def getReporter: Reporter
  
  /**
   * @return true if the passed value differs from the current value
   */
  @throws(classOf[ScriptException])
  def setClasspathX(classpath: Array[AbstractFile]): Boolean
  
  @throws(classOf[ScriptException])
  def getClasspathX: Array[AbstractFile]
}