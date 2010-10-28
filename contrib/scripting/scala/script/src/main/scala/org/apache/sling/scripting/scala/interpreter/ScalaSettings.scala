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

import scala.tools.nsc.Settings

/**
 * Utility to parse Scala compiler settings from a string. This class
 * can be used from Java instead of Settings. Settings is not accessible
 * from Java since scala.Nothing is not visible exposed. 
 * See https://lampsvn.epfl.ch/trac/scala/ticket/1254
 */  
class ScalaSettings(error: String => Unit) extends Settings(error) {
  def this() = this(Console.println)
  def parse(line: String) = parseParams(splitParams(line))
}

