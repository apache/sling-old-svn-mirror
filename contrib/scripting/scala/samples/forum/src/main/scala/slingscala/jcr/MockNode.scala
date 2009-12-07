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

import java.io.InputStream
import java.util.Calendar
import javax.jcr.{Item, Node, Value, Property, NodeIterator, PropertyIterator}
import javax.jcr.version.{Version, VersionHistory}
import javax.jcr.lock.Lock
import javax.jcr.nodetype.{NodeType, NodeDefinition}

trait MockNode extends Node {
  def addNode(relPath: String): Node = null
  def addNode(relPath: String, primaryNodeTypeName: String): Node = null  
  def orderBefore(srcChildRelPath: String, destChildRelPath: String) {}
  def setProperty(name: String, value: Value): Property = null
  def setProperty(name: String, value: Value, tyqe: Int): Property = null
  def setProperty(name: String, values: Array[Value]): Property = null
  def setProperty(name: String, values: Array[Value], tyqe: Int): Property = null
  def setProperty(name: String, values: Array[String]): Property = null
  def setProperty(name: String, values: Array[String], tyqe: Int): Property = null
  def setProperty(name: String, value: String): Property = null
  def setProperty(name: String, value: String, tyqe: Int): Property = null
  def setProperty(name: String, value: InputStream): Property = null
  def setProperty(name: String, value: Boolean): Property = null
  def setProperty(name: String, value: Double): Property = null
  def setProperty(name: String, value: Long): Property= null
  def setProperty(name: String, value: Calendar): Property = null
  def setProperty(name: String, value: Node): Property = null
  def getNode(relPath: String): Node = null
  def getNodes: NodeIterator = null
  def getNodes(namePattern: String): NodeIterator = null
  def getProperty(relPath: String): Property = null
  def getProperties: PropertyIterator = null
  def getProperties(namePattern: String): PropertyIterator = null
  def getPrimaryItem: Item = null
  def getUUID: String = null
  def getIndex: Int = 0
  def getReferences: PropertyIterator = null
  def hasNode(relPath: String): Boolean = false
  def hasProperty(relPath: String): Boolean = false
  def hasNodes: Boolean = false
  def hasProperties: Boolean = false
  def getPrimaryNodeType: NodeType = null
  def getMixinNodeTypes: Array[NodeType] = null
  def isNodeType(nodeTypeName: String): Boolean = false
  def addMixin(mixinName: String) {}
  def removeMixin(mixinName: String) {}
  def canAddMixin(mixinName: String): Boolean = false
  def getDefinition: NodeDefinition = null
  def checkin: Version = null
  def checkout {}
  def doneMerge(version: Version) {}
  def cancelMerge(version: Version) {}
  def update(srcWorkspaceName: String) {}
  def merge(srcWorkspace: String, bestEffort: Boolean): NodeIterator = null
  def getCorrespondingNodePath(workspaceName: String): String = null
  def isCheckedOut: Boolean = false
  def restore(versionName: String, removeExisting: Boolean) {}
  def restore(version: Version, removeExisting: Boolean) {}
  def restore(version: Version, relPath: String, removeExisting: Boolean) {}
  def restoreByLabel(versionLabel: String, removeExisting: Boolean) {}
  def getVersionHistory: VersionHistory = null
  def getBaseVersion: Version  = null
  def lock(isDeep: Boolean, isSessionScoped: Boolean): Lock = null
  def getLock: Lock = null
  def unlock {}
  def holdsLock: Boolean = false
  def isLocked: Boolean = false
}
