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

import javax.script.{ScriptException, ScriptContext}

/**
 * A ScriptInfo instance provides information about Scala scripts.
 */  
trait ScriptInfo {
  /**
   * @return  the default name of the script class
   */
  def getDefaultScriptClass: String
  
  /**
   * @return  the name of the script class ofr the passed <code>script</code> in 
   *   the given <code>context</code>. 
   */
  @throws(classOf[ScriptException])
  def getScriptClass(script: String, context: ScriptContext): String
}
