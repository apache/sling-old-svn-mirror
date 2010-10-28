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

import javax.script.{ScriptContext, ScriptException}

object AbstractScriptInfo {
  val SCALA_SCRIPT_CLASS = "scala.script.class"
  val DEFAULT_SCALA_SCRIPT_CLASS = "org.apache.sling.scripting.scala.Script"
}

/**
 * Abstract base implementation of {@link ScriptInfo}. 
 */
abstract class AbstractScriptInfo(protected var defaultScriptClass: String) extends ScriptInfo {
  
  def this() {
    this(AbstractScriptInfo.DEFAULT_SCALA_SCRIPT_CLASS)
  }
  
  /**
   * @return  {@link #DEFAULT_SCALA_SCRIPT_CLASS}
   */
  def getDefaultScriptClass = defaultScriptClass
                 
  /**
   * @return  the value of the {@link AbstractScriptInfo.SCALA_SCRIPT_CLASS} attribute
   *   in the <code>context</code>. 
   */
  @throws(classOf[ScriptException])
  def getScriptClass(script: String, context: ScriptContext) = {
    val value = context.getAttribute(AbstractScriptInfo.SCALA_SCRIPT_CLASS);
    
    value match {
      case v: String  => v
      case _          => defaultScriptClass
    }
  }
                 
}
