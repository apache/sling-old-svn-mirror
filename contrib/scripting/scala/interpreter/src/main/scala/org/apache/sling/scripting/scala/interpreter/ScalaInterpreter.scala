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
import scala.tools.nsc.{Settings, Global}
import scala.tools.nsc.interpreter.AbstractFileClassLoader
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.reporters.Reporter
import scala.tools.nsc.util.{SourceFile, BatchSourceFile}
import java.net.URLClassLoader
import java.io.{File, InputStream, OutputStream}
import org.apache.sling.scripting.scala.Utils.{option}

package org.apache.sling.scripting.scala.interpreter {

/**
 * An interpreter for Scala scripts. Interpretation of scripts proceeds in the following steps:
 * <ol>
 * <li>Pre-compilation: The source script is {@link #preProcess} wrapped into an object wrapper which
 *   contains variable definitions of the approproate types for the passed {@link Bindings bindings}.</li>
 * <li>Compilation: The resulting source code is {@link #compile compiled} by the Scala compiler. </li>
 * <li>Execution: The class file is {@link #execute loaded} and its main method called.</li>
 * </ol>
 * @param settings  compiler settings
 * @param reporter  reporter for compilation
 * @param classes  additional classes for the classpath
 * @param outDir  ourput directory for the compiler
 */
class ScalaInterpreter(settings: Settings, reporter: Reporter, classes: Array[AbstractFile],
                       outDir: AbstractFile) {

  /**
   * Same as <code>ScalaInterpreter(settings, reporter, classes, null)</code>.
   * @param settings
   * @param reporter
   * @param classes
   * @return
   */
  def this(settings: Settings, reporter: Reporter, classes: Array[AbstractFile]) =
    this(settings, reporter, classes, null)

  /**
   * Same as <code>ScalaInterpreter(settings, reporter, null, outDir)</code>.
   * @param settings
   * @param reporter
   * @param outDir
   * @return
   */
  def this(settings: Settings, reporter: Reporter, outDir: AbstractFile) =
    this(settings, reporter, null, outDir)

  /**
   * Same as <code>ScalaInterpreter(settings, reporter, null, null)</code>.
   * @param settings
   * @param reporter
   * @return
   */
  def this(settings: Settings, reporter: Reporter) =
    this(settings, reporter, null, null)

  /**
   * Line separater used in the wrapper object
   */
  protected val NL: String = System.getProperty("line.separator");

  /**
   * The parent class loader used for execution
   */
  protected val parentClassLoader: ClassLoader = getClass.getClassLoader

  /**
   * The Scala compiler used for compilation
   */
  protected val compiler: Global = {
    val c = new ScalaCompiler(settings, reporter, classes)
    if (outDir != null) c.genJVM.outputDir = outDir
    c
  }

  /**
   * Utility method for parsing a fully quallyfied class name into its packet compounds
   * @param name  Full qualified class name
   * @return  The compounds consisting of the (sub)-packates followed by the class name
   */
  protected def packetize(name: String): List[String] =
    name.split('.').toList

  /**
   * Pre processor for wrapping the script such that it becomes a valid Scala source entity
   * which can be passed the the Scala compiler.
   * @param name  name of the script. Used for generating the class name of the wrapper.
   * @param code  source code of the script
   * @param bindings  bindings to be passed to the script
   * @return  a valid Scala source
   */
  protected def preProcess(name: String, code: String, bindings: Bindings): String = {
    def bind(a: (String, Argument[_])) =
      "val " + a._1 + " = bindings.getValue(\"" + a._1 + "\").asInstanceOf[" + a._2.getType.getName + "]"

    val compounds = packetize(name)

    def packageDeclaration =
      if (compounds.size > 1) compounds.init.mkString("package ", ".", "") + NL
      else ""

    def className = compounds.last

    packageDeclaration +
    "object " + className + " {" + NL +
    "  def main(bindings: org.apache.sling.scripting.scala.interpreter.Bindings," + NL +
    "           stdIn: java.io.InputStream," + NL +
    "           stdOut: java.io.OutputStream) {" + NL +
    "    def run() {" + NL +
           bindings.map(bind).mkString("", NL, NL) +
           code + "" + NL +
    "      return" + NL +
    "    }" + NL +
    "    Console.withIn(stdIn) {" + NL +
    "      Console.withOut(stdOut) {" + NL +
    "        run" + NL +
    "        stdOut.flush" + NL +
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
    compile(name, preProcess(name, code, bindings))

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
   * Looks up the class file for a compiled script
   * @param  name  script name
   * @return  the class file or null if not found
   */
  def getClassFile(name: String): AbstractFile = {
    var file: AbstractFile = compiler.genJVM.outputDir
    val pathParts = name.split("[./]").toList
    for (dirPart <- pathParts.init) {
      file = file.lookupName(dirPart, true)
      if (file == null) {
        return null
      }
    }
    file.lookupName(pathParts.last + ".class", false)
  }

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
      val classLoader = new AbstractFileClassLoader(compiler.genJVM.outputDir, parentClassLoader)
      val script = Class.forName(name, true, classLoader)
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

}


}