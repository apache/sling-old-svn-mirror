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

import java.util.{Locale, ResourceBundle}
import javax.servlet.RequestDispatcher
import javax.servlet.http.Cookie
import org.apache.sling.api.SlingHttpServletRequest
import org.apache.sling.api.resource.{Resource, ResourceResolver}
import org.apache.sling.api.request.{RequestPathInfo, RequestParameter, RequestParameterMap, 
                                     RequestDispatcherOptions, RequestProgressTracker}

trait MockSlingHttpServletRequest extends SlingHttpServletRequest {
  def getResource: Resource = null
  def getResourceResolver: ResourceResolver = null
  def getRequestPathInfo: RequestPathInfo = null
  def getRequestParameter(name: String): RequestParameter = null
  def getRequestParameters(name: String): Array[RequestParameter] = null
  def getRequestParameterMap: RequestParameterMap = null
  def getRequestDispatcher(path: String, options: RequestDispatcherOptions): RequestDispatcher = null
  def getRequestDispatcher(resource: Resource, options: RequestDispatcherOptions): RequestDispatcher = null
  def getRequestDispatcher(resource: Resource): RequestDispatcher = null
  def getCookie(name: String): Cookie = null
  def getResponseContentType: String = null
  def getResponseContentTypes: java.util.Enumeration[String] = null
  def getResourceBundle(locale: Locale): ResourceBundle = null
  def getResourceBundle(baseName: String, locale: Locale): ResourceBundle = null
  def getRequestProgressTracker: RequestProgressTracker = null
  def adaptTo[T](t: Class[T]): T = null.asInstanceOf[T]
}
