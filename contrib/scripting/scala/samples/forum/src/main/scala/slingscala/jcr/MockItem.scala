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

import javax.jcr.{Item, ItemVisitor, Node, Session}

trait MockItem extends Item {
  def getPath: String = null
  def getName: String = null
  def getAncestor(depth: Int): Item = null
  def getParent: Node = null
  def getDepth: Int = 0
  def getSession: Session = null
  def isNode: Boolean = false
  def isNew: Boolean = false
  def isModified: Boolean = false
  def isSame(otherItem: Item): Boolean = false
  def accept(visitor: ItemVisitor) {}
  def save {}
  def refresh(keepChanges: Boolean) {}
  def remove {}
}
