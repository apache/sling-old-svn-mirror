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
import scala.collection.Map
import scala.collection.mutable.HashMap

package org.apache.sling.scripting.scala.interpreter {

/**
 * An argument consists of a value and its type
 */
trait Argument[T <: AnyRef] {

  /**
   * @returns  the value of this argument
   */
  def getValue: T

  /**
   * @returns  the type of this argument
   */
  def getType: Class[T]
}

/**
 * Bindings of names to {@link Argument}s
 */
trait Bindings extends Map[String, Argument[_]] {

  /**
   * Associate an argument with a name
   * @param name
   * @param argument
   * @returns  The argument which was previously associated with the
   *   given name or null if none.
   */
  def put(name: String, argument: Argument[_]): Argument[_]

  /**
   * Associate an argument with a name
   * @param  name
   * @param  value  the value of the {@link Argument}
   * @param  tyqe the type of the {@link Argument}
   */
  def put[T <: AnyRef](name: String, value: T, tyqe: Class[T]): Argument[_] =
    put(name, new Argument[T] {
      def getValue: T = value
      def getType: Class[T] = tyqe
    })

  /**
   * @returns  the value of the {@link Argument} associated with the given name
   * @param name
   */
  def getValue(name: String): AnyRef =
    get(name) match {
      case Some(a) => a.getValue
      case None => null
    }

  /**
   * @returns  the type of the {@link Argument} associated with the given name
   * @param name
   */
  def getType(name: String): Class[_] =
    get(name) match {
      case Some(a) => a.getClass
      case None => null
    }
}

/**
 * HashMap based default implementation of {@link Bindings}.
 */
class ScalaBindings extends Bindings {
  private val bindings = new HashMap[String, Argument[_]]

  def size: Int = bindings.size
  def get(name: String): Option[Argument[_]] = bindings.get(name)
  def elements: Iterator[(String, Argument[_])] = bindings.elements

  def put(name: String, argument: Argument[_]): Argument[_] =
    bindings.put(name, argument) match {
      case Some(a) => a
      case None => null
    }
}

}

