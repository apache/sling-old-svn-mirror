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

import scala.tools.nsc.interpreter.AbstractFileClassLoader
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.reporters.Reporter
import scala.tools.nsc.util.{SourceFile, BatchSourceFile}
import scala.tools.nsc.{FatalError, Settings, Global}
import java.io.{InputStream, OutputStream}
import org.apache.sling.scripting.scala.Utils.option

/**
 * An interpreter for Scala scripts. Interpretation of scripts proceeds in the following steps:
 * <ol>
 * <li>Pre-compilation: The source script is {@link #preProcess} wrapped into a wrapper which
 *   contains variable definitions of the approproate types for the passed {@link Bindings bindings}.</li>
 * <li>Compilation: The resulting source code is {@link #compile compiled} by the Scala compiler. </li>
 * <li>Execution: The class file is {@link #execute loaded} and its main method called.</li>
 * </ol>
 * @param settings  compiler settings
 * @param reporter  reporter for compilation
 * @param classes  additional classes for the classpath
 */
class ScalaInterpreter(settings: Settings, reporter: Reporter, classes: Array[AbstractFile]) {

  /**
   * Same as <code>ScalaInterpreter(settings, reporter, null, outDir)</code>.
   * @param settings
   * @param reporter
   * @return
   */
  def this(settings: Settings, reporter: Reporter) =
    this(settings, reporter, Array.empty)

  /**
   * The parent class loader used for execution
   */
  protected val parentClassLoader: ClassLoader = getClass.getClassLoader

  /**
   * The Scala compiler used for compilation
   */
  protected val compiler: Global = new ScalaCompiler(settings, reporter, classes)

  /**
   * Generates a wrapper which contains variables declarations and implicit conversions
   * to make the bindings visible on all accessible types. 
   * @param name  name of the script. Used for generating the class name of the wrapper.
   * @param code  source code of the script
   * @param bindings  bindings to be passed to the script
   * @return  a valid Scala source
   * @throws InterpreterException
   */
  @throws(classOf[InterpreterException])
  protected def preProcess(name: String, code: String, bindings: Bindings): String = {
    val NL: String = System.getProperty("line.separator")

    def packetize(name: String): List[String] = name.split('.').toList
    def mangle(name: String) = packetize(name).mkString("_")

    def bind(arg: (String, AnyRef)) = {
      val views = bindings.getViews(arg._2.getClass)
      val className =
        // TODO some classloader roguery prevents c.q.l.c.Logger from being accessible
        // when executing the script although the class is accessible here. As a work
        // around special case to o.s.Logger here. See SLING-3053
        if ("ch.qos.logback.classic.Logger" == views.head.getName) "org.slf4j.Logger"
        else views.head.getName
      val implicits = 
        for {
          view <- views.tail
          intfName = view.getName
          methName = mangle(className) + "2" + mangle(intfName)  
        }
        yield 
          "    implicit def " + methName + "(x: " + className + "): " + intfName + " = x.asInstanceOf[" +  intfName + "]"
      
      "    lazy val " + arg._1 + " = bindings.getValue(\"" + arg._1 + "\").asInstanceOf[" + className + "]" + NL +
      implicits.mkString(NL)
    }

    val compounds = packetize(name)
    val className = compounds.last

    def packageDeclaration =
      if (compounds.size > 1) compounds.init.mkString("package ", ".", "") + NL
      else throw new InterpreterException("Default package not allowed: " + name)

    code + NL + 
    packageDeclaration + " {" + NL + 
    "  class " + className + "Args(bindings: org.apache.sling.scripting.scala.interpreter.Bindings) { " + NL +
         bindings.map(bind).mkString(NL) + NL + 
    "  } " + NL + 
    "  object " + className + "Runner {" + NL +
    "    def main(bindings: org.apache.sling.scripting.scala.interpreter.Bindings," + NL +
    "             stdIn: java.io.InputStream," + NL +
    "             stdOut: java.io.OutputStream) {" + NL +
    "      Console.withIn(stdIn) {" + NL +
    "        Console.withOut(stdOut) {" + NL +
    "          new " + className + "(new " + className + "Args(bindings))" + NL +
    "          stdOut.flush" + NL +
    "        }" + NL +
    "      }" + NL +
    "    }" + NL +
    "  }" + NL + 
    "}" + NL
  }

  /**
   * Compiles a list of source files. No pre-processing takes place.
   * @param sources  source files
   * @return  result of compilation
   */
  protected def compile(sources: List[SourceFile]): Reporter = {
    reporter.reset
    val run = new compiler.Run
    if (reporter.hasErrors)
      reporter
    else {
      run.compileSources(sources)
      reporter
    }
  }

  /**
   * Compiles a single source file. No pre-processing takes place.
   * @param name  name of the script
   * @param code  source code
   * @return  result of compilation
   */
  def compile(name: String, code: String): Reporter =
    compile(List(new BatchSourceFile(name, code.toCharArray)))

  /**
   * Pre-processes and compiles a single source file.
   * @param name  name of the script
   * @param code  source code
   * @param bindings  variable bindings to pass to the script
   * @return  result of compilation
   */
  def compile(name: String, code: String, bindings: Bindings): Reporter =
    {
      val process: String = preProcess(name, code, bindings)
      println(process)
      compile(name, process)
    }

  /**
   * Compiles a single source file. No pre-processing takes place.
   * @param sources  source file
   * @return  result of compilation
   */
  def compile(source: AbstractFile): Reporter = {
    compile(List(new BatchSourceFile(source)))
  }

  /**
   * Pre-processes and compiles a single source file.
   * @param name  name of the script
   * @param source  source file
   * @param bindings  variable bindings to pass to the script
   * @return  result of compilation
   */
  def compile(name: String, source: AbstractFile, bindings: Bindings): Reporter = {
    val code = new String(source.toByteArray)
    compile(name, preProcess(name, code, bindings))
  }

  /**
   * Interprete a script
   * @param name  name of the script
   * @param code  source code
   * @param bindings  variable bindings to pass to the script
   * @param in  stdIn for the script execution
   * @param out  stdOut for the script execution
   * @return  result of execution
   * @throws InterpreterException
   */
  @throws(classOf[InterpreterException])
  def interprete(name: String, code: String, bindings: Bindings, in: Option[InputStream],
                 out: Option[OutputStream]): Reporter = {
    compile(name, code, bindings)
    if (reporter.hasErrors)
      reporter
    else {
      execute(name, bindings, in, out)
    }
  }

  /**
   * Same as <code>interprete(name, code, bindings, None, None)</code>.
   * @param name  name of the script
   * @param code  source code
   * @param bindings  variable bindings to pass to the script
   * @return  result of execution
   * @throws InterpreterException
   */
  @throws(classOf[InterpreterException])
  def interprete(name: String, code: String, bindings: Bindings): Reporter =
    interprete(name, code, bindings, None, None)

  /**
   * Same as <code>interprete(name, code, bindings, Some(in), Some(out))</code>.
   * @param name  name of the script
   * @param code  source code
   * @param bindings  variable bindings to pass to the script
   * @param in  stdIn for the script execution
   * @param out  stdOut for the script execution
   * @return  result of execution
   * @throws InterpreterException
   */
  @throws(classOf[InterpreterException])
  def interprete(name: String, code: String, bindings: Bindings, in: InputStream,
                 out: OutputStream): Reporter =
    interprete(name, code, bindings, option(in), option(out))

  /**
   * Interprete a script
   * @param name  name of the script
   * @param source source file
   * @param bindings  variable bindings to pass to the script
   * @param in  stdIn for the script execution
   * @param out  stdOut for the script execution
   * @return  result of execution
   * @throws InterpreterException
   */
  @throws(classOf[InterpreterException])
  def interprete(name: String, source: AbstractFile, bindings: Bindings, in: Option[InputStream],
                 out: Option[OutputStream]): Reporter = {
    compile(name, source, bindings)
    if (reporter.hasErrors)
      reporter
    else {
      execute(name, bindings, in, out)
    }
  }

  /**
   * Same as <code>interprete(name, code, bindings, None, None)</code>.
   * @param name  name of the script
   * @param source source file
   * @param bindings  variable bindings to pass to the script
   * @return  result of execution
   * @throws InterpreterException
   */
  @throws(classOf[InterpreterException])
  def interprete(name: String, source: AbstractFile, bindings: Bindings): Reporter =
    interprete(name, source, bindings, None, None)

  /**
   * Same as <code>interprete(name, code, bindings, Some(in), Some(out))</code>.
   * @param name  name of the script
   * @param source source file
   * @param bindings  variable bindings to pass to the script
   * @param in  stdIn for the script execution
   * @param out  stdOut for the script execution
   * @return  result of execution
   * @throws InterpreterException
   */
  @throws(classOf[InterpreterException])
  def interprete(name: String, source: AbstractFile, bindings: Bindings, in: InputStream,
                 out: OutputStream): Reporter =
    interprete(name, source, bindings, option(in), option(out))

  /**
   * Executes a compiled script
   * @param name  name of the script
   * @param bindings  variable bindings to pass to the script
   * @param in  stdIn for the script execution
   * @param out  stdOut for the script execution
   * @return  result of execution
   * @throws  InterpreterException when class files cannot be accessed or nor entry point is found
   */
  @throws(classOf[InterpreterException])
  def execute(name: String, bindings: Bindings, in: Option[InputStream], out: Option[OutputStream]): Reporter = {
    try {
      val classLoader = new AbstractFileClassLoader(outputDir, parentClassLoader)
      val script = classLoader.loadClass(name + "Runner")
      val initMethod = (script
        .getDeclaredMethods
        .toList
        .find(method => method.getName == "main")
        .get)

      initMethod.invoke(null, Array(bindings, in.getOrElse(java.lang.System.in),
                                              out.getOrElse(java.lang.System.out)): _*)
      reporter
    }
    catch {
      case e: java.lang.reflect.InvocationTargetException =>
        throw new InterpreterException("Error executing " + name, e.getTargetException)
      case e: Exception =>
        throw new InterpreterException("Error executing " + name, e)
    }
  }

  /**
   * Same as <code>execute(name, bindings, None, None)</code>.
   * @param name  name of the script
   * @param bindings  variable bindings to pass to the script
   * @return  result of execution
   * @throws  InterpreterException when class files cannot be accessed or nor entry point is found
   */
  @throws(classOf[InterpreterException])
  def execute(name: String, bindings: Bindings): Reporter =
    execute(name, bindings, None, None)

  /**
   * Same as <code>execute(name, bindings, Some(in), Some(out))</code>.
   * @param name  name of the script
   * @param bindings  variable bindings to pass to the script
   * @param in  stdIn for the script execution
   * @param out  stdOut for the script execution
   * @return  result of execution
   * @throws  InterpreterException when class files cannot be accessed or nor entry point is found
   */
  @throws(classOf[InterpreterException])
  def execute(name: String, bindings: Bindings, in: InputStream, out: OutputStream): Reporter =
    execute(name, bindings, option(in), option(out))

  def outputDir = try {
      settings.outputDirs.outputDirFor(null);
    }
    catch {
      case e: FatalError => throw new InterpreterException(e) 
    }

}