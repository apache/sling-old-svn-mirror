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

import scala.tools.nsc.Settings;
import scala.tools.nsc.reporters.AbstractReporter;
import scala.tools.nsc.util.Position;

object BacklogReporter {
  val DEFAULT_SIZE = 50
}

class BacklogReporter(val settings: Settings, size: Int) extends AbstractReporter {
  private var backLog: List[Info] = Nil
  
  def this(settings: Settings) {
    this(settings, BacklogReporter.DEFAULT_SIZE)
  } 

  override def reset() {
    super.reset
    backLog = Nil
  }

  override def display(pos: Position, msg: String, severity: Severity) {
    severity.count += 1
    if (size > 0) {
      backLog = backLog:::List(new Info(pos, msg, severity))
      if (backLog.length > size) {
        backLog = backLog.tail
      }
    }
  }

  override def displayPrompt() {
    // empty
  }
  
  override def toString = 
    backLog.map(_.toString).mkString("\n")

  private class Info(pos: Position, msg: String, severity: Severity) {
                                 
    override def toString = {
      val level = severity match {
        case INFO => "INFO "
        case WARNING => "WARNING "
        case _ => "ERROR " 
      }
      
      val source = try {
        pos.source + " "
      }
      catch {
        case _: UnsupportedOperationException => ""
      } 
      
      val line = try {
        "line " + pos.line + " "
      }
      catch {
        case _: UnsupportedOperationException => ""
      }
      
      level + source + line + ": " + msg
    }

  }

}
