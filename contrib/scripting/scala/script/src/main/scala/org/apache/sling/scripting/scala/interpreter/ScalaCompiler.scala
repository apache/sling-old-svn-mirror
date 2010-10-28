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

import scala.tools.nsc.{Settings, Global}
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.reporters.Reporter
import scala.tools.nsc.util.{ClassPath, MergedClassPath, DirectoryClassPath}
import tools.nsc.backend.JavaPlatform

/**
 * Extended Scala compiler which supports a class path with {@link AbstractFile} entries.
 * Note: this implementation does not support MSIL (.NET).
 */
class ScalaCompiler(settings: Settings, reporter: Reporter, classes: Array[AbstractFile])
  extends Global(settings, reporter) {

  override lazy val classPath: ClassPath[AbstractFile] = {
    val classPathOrig = platform match {
      case p: JavaPlatform => p.classPath
      case _ =>  throw new InterpreterException("Only JVM target supported")
    }

    val classPathNew = classes.map(c => new DirectoryClassPath(c, classPathOrig.context))
    new MergedClassPath[AbstractFile](classPathOrig :: classPathNew.toList, classPathOrig.context)
  }

  override def rootLoader: LazyType = new loaders.JavaPackageLoader(classPath)
}
