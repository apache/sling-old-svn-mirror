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
package forum

import java.io.PrintWriter
import java.io.Reader

import javax.jcr.Node

import org.slf4j.Logger

import org.apache.sling.api.scripting.SlingScriptHelper
import org.apache.sling.api.resource.Resource
import org.apache.sling.api.{SlingHttpServletRequest, SlingHttpServletResponse}

trait MockBindings {
  def log: Logger = null
  def out: PrintWriter = new PrintWriter(Console.out)
  def sling: SlingScriptHelper = null
  def currentNode: Node = null
  def reader: Reader = Console.in
  def resource: Resource = null
  def request: SlingHttpServletRequest = null
  def response: SlingHttpServletResponse = null
}
