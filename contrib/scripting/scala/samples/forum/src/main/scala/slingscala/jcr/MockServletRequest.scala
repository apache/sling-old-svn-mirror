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
package slingscala.jcr

import java.util.Locale
import java.io.BufferedReader
import javax.servlet.{ServletRequest, ServletInputStream, RequestDispatcher}

trait MockServletRequest extends ServletRequest {
  def getAttribute(name: String): Object = null
  def getAttributeNames: java.util.Enumeration[_] = null 
  def getCharacterEncoding: String = null
  def setCharacterEncoding(env: String) {}
  def getContentLength: Int = 0
  def getContentType: String = null
  def getInputStream: ServletInputStream = null
  def getParameter(name: String): String = null
  def getParameterNames: java.util.Enumeration[_] = null
  def getParameterValues(name: String): Array[String] = null
  def getParameterMap: java.util.Map[_,_] = null
  def getProtocol: String = null
  def getScheme: String = null
  def getServerName: String = null
  def getServerPort: Int = 0
  def getReader: BufferedReader = null
  def getRemoteAddr: String = null
  def getRemoteHost: String = null
  def setAttribute(name: String, o: Object) {} 
  def removeAttribute(name: String) {}
  def getLocale: Locale = null
  def getLocales: java.util.Enumeration[_] = null
  def isSecure: Boolean = false
  def getRequestDispatcher(path: String): RequestDispatcher = null
  def getRealPath(path: String): String = null
  def getRemotePort: Int = 0
  def getLocalName: String = null
  def getLocalAddr: String = null
  def getLocalPort: Int = 0
}
