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

import tools.nsc.io.AbstractFile
import java.io.{OutputStream, InputStream}

/**
 * General purpose utility functions
 */
object Utils {

  /**
   * Evaluate <code>f</code> on <code>s</code> if <code>s</code> is not null.
   * @param s
   * @param f
   * @return <code>f(s)</code> if s is not <code>null</code>, <code>null</code> otherwise.
   */
  def nullOrElse[S, T](s: S)(f: S => T): T =
    if (s == null) null.asInstanceOf[T]
    else f(s)

  /**
   * @param t
   * @param default
   * @return <code>t</code> or <code>default</code> if <code>null</code>.
   */
  def valueOrElse[T](t: T)(default: => T) =
    if (t == null) default
    else t

  /**
   * Converts a value into an Option.
   * @param value
   * @returns <code>Some(value)</code> if value is not <code>null</code>,
   * <code>None</code> otherwise.
   */
  def option[T](value: T): Option[T] =
    if (null == value) None else Some(value)

  /**
   * Converts the given identifier to a legal Java/Scala identifier
   * @param identifier Identifier to convert
   * @return Legal Java/Scala identifier corresponding to the given identifier
   */
  def makeIdentifier(identifier: String) = {
    val id = new StringBuilder(identifier.length);
    if (!Character.isJavaIdentifierStart(identifier.charAt(0))) 
      id.append('_');
    
    for (val ch <- identifier) {
      if (Character.isJavaIdentifierPart(ch) && ch != '_') id.append(ch)
      else if (ch == '.') id.append('_')
      else id.append(mangleChar(ch))
    }
        
    if (isKeyword(id.toString)) 
      id.append('_')

    id.toString
  }

  /**
   * Mangle the specified character to create a legal Java/Scala class name.
   */
  def mangleChar(ch: Char) = {
    val result = new StringBuilder("01234")
    result(0) = '_'
    result(1) = Character.forDigit((ch >> 12) & 0xf, 16)
    result(2) = Character.forDigit((ch >> 8) & 0xf, 16)
    result(3) = Character.forDigit((ch >> 4) & 0xf, 16)
    result(4) = Character.forDigit(ch & 0xf, 16)
    result
  }

  /**
   * Test whether the argument is a Scala/Java keyword
   */
  def isKeyword(token: String) = 
    Set (
      "abstract", "assert", "boolean", "break", "byte", "case", "catch",
      "char", "class", "const", "continue", "default", "do", "double",
      "else", "enum", "extends", "final", "finally", "float", "for",
      "goto", "if", "implements", "import", "instanceof", "int",
      "interface", "long", "native", "new", "package", "private",
      "protected", "public", "return", "short", "static", "strictfp",
      "super", "switch", "synchronized", "this", "throws", "transient",
      "try", "void", "volatile", "while", "true", "false", "null",
      "forSome", "type", "var", "val", "def", "with", "yield", "match", 
      "implicit", "lazy", "override", "sealed", "trait", "object"
    ).contains(token)
  
}

object NonExistingFile extends AbstractFile {
  def name = null
  def path = null
  def absolute = this
  def container = null
  def file = null
  def create { unsupported }
  def delete { unsupported }
  def isDirectory = false
  def lastModified = 0
  def input: InputStream = null
  def output: OutputStream = null
  def iterator: Iterator[AbstractFile] = Iterator.empty
  def lookupName(name: String, directory: Boolean) = null
  def lookupNameUnchecked(name: String, directory: Boolean) = this
  override def exists = false
}
