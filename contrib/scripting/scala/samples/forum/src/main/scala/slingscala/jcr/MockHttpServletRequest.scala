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

import java.security.Principal
import javax.servlet.http.{HttpServletRequest, HttpSession, Cookie}

trait MockHttpServletRequest extends HttpServletRequest {
  def getAuthType: String = null
  def getCookies: Array[Cookie] = null
  def getDateHeader(name: String): Long = 0
  def getHeader(name :String): String = null
  def getHeaders(name: String): java.util.Enumeration[_] = null
  def getHeaderNames: java.util.Enumeration[_] = null
  def getIntHeader(name: String): Int = 0
  def getMethod: String = null
  def getPathInfo: String = null
  def getPathTranslated: String = null
  def getContextPath: String = null
  def getQueryString: String = null
  def getRemoteUser: String = null
  def isUserInRole(role: String): Boolean = false
  def getUserPrincipal: Principal = null
  def getRequestedSessionId: String = null
  def getRequestURI: String = null
  def getRequestURL: StringBuffer = null
  def getServletPath: String = null
  def getSession(create: Boolean): HttpSession = null
  def getSession: HttpSession = null
  def isRequestedSessionIdValid: Boolean = false
  def isRequestedSessionIdFromCookie: Boolean = false
  def isRequestedSessionIdFromURL: Boolean = false
  def isRequestedSessionIdFromUrl: Boolean = false
}
