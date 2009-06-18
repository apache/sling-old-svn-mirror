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
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.util.ClassPath

package org.apache.sling.scripting.scala.interpreter {

/**
 * Specialized class path for the {@link ScalaCompiler} which supports {@link AbstractFile}
 * entries.
 */
class ScalaClasspath(onlyPresentation: Boolean) extends ClassPath(onlyPresentation) {

  class BuildClasspath(classpath: String, source: String, output: String, boot: String, extdirs: String,
      codebase: String, classes: Array[AbstractFile])
      extends Build(classpath, source, output, boot, extdirs, codebase) {

    if (classes != null) {
      for (file <- classes if file != null) {
        val lib = new Library(file)
        entries += lib
      }
    }
  }

}

}