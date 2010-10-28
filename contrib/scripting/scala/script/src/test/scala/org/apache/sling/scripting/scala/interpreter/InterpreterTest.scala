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

import java.io.File
import junit.framework.TestCase
import org.apache.sling.scripting.scala.Utils.valueOrElse
import scala.tools.nsc.io.PlainFile

/**
 * Standard test cases where files are read/written to/from the file system.
 */  
class InterpreterTest extends TestCase with Tests {
  
  var interpreterHelper: InterpreterHelper = null;
  
  override def setUp() {
    super.setUp();
    
    val workDir = new PlainFile(new File("target")).subdirectoryNamed("tmp")
    val srcDir = workDir.subdirectoryNamed("src")
    val outDir = workDir.subdirectoryNamed("classes")
    
    interpreterHelper = new InterpreterHelper(srcDir, outDir) {
      override def getClasspath = valueOrElse(System.getProperty("surefire.test.class.path")) {
        super.getClasspath
      }
    }
  }
  
  override def tearDown() {
    super.tearDown()
    interpreterHelper = null
  }

}