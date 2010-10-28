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

import java.io.{File, InputStream, OutputStream, IOException, ByteArrayOutputStream, ByteArrayInputStream}
import javax.jcr.{Session, Node, Property}
import org.apache.sling.scripting.scala.Utils.{nullOrElse, valueOrElse}
import tools.nsc.io.AbstractFile

/**
 * Implementation of {@link AbstractFile} on top of the {@link javax.jcr.Node}s
 * of a JCR repositoy.
 */
object JcrFS {

  /**
   * Create a new {@link AbstractFile} for the given node.
   * @param node
   * @throws IOException  if the node type is neither nt:folder nor nt:file
   */
  def create(node: Node): JcrNode = node.getPrimaryNodeType.getName match {
    case "nt:file" => JcrFile(node) // todo fix: dont hc ns prefixes
    case "nt:folder" => JcrFolder(node)
    case "sling:Folder" => JcrFolder(node)
    case _ => throw new IOException("Neither file nor folder: " + node.getPath)
  }

  /**
   * Create a new {@link AbstractFile} for node at the given path in the given
   * {@link javax.jcr.Session}.
   * @param session
   * @param path
   * @throws IOException  if the node at the given path is neither nt:folder nor
   *   nt:file or the path does not identify a node.
   */
  def create(session: Session, path: String): JcrNode = {
    session.getItem(path) match {
      case n: Node => create(n)
      case _ => throw new IOException("Path not found: " + path)
    }
  }

  abstract sealed class JcrNode(node: Node) extends AbstractFile {
    require(node != null, "node must not be null")

    /**
     * @returns  an empty stream
     */
    val emptyInputStream: InputStream = new InputStream {
      override def read = -1
    }

    private def getIndex: String =
      if (node.getIndex == 1) ""
      else "[" + node.getIndex + "]"

    /**
     * @returns  the name of the node covered
     */
    def name: String = node.getName + getIndex

    /**
     * @returns  the path of the node covered
     */
    def path: String = node.getPath

    def container: JcrNode = JcrFS.create(node.getParent)

    /**
     * @returns  null
     */
    def file: File = null

    def absolute = this

    /**
     * @returns  the value of the jcr:lastModified property of either the jcr:content node
     *   beneath this node or from this node itself otherwise or 0 if this node does not
     *   have a jcr:lastModified property.
     */
    def lastModified: Long =
      if (node.hasProperty("jcr:content/jcr:lastModified")) node.getProperty("jcr:content/jcr:lastModified").getLong
      else {
        if (node.hasProperty("jcr:lastModified")) node.getProperty("jcr:lastModified").getLong
        else 0
      }
    override def equals(other: Any): Boolean =
      other match {
        case that: JcrNode => this.path == that.path
        case _ => false
      }

    override def hashCode: Int = path.hashCode

    def create { unsupported }
    def delete { unsupported }

    def lookupNameUnchecked(name: String, directory: Boolean) = {
      val file = lookupName(name, directory)
      if (file == null) NonExistingFile
      else file
    }
  }

  case class JcrFolder(node: Node) extends JcrNode(node) {

    /**
     * @returns  true
     */
    def isDirectory: Boolean = true

    /**
     * Not supported. Always throws an IOException.
     * @throws IOException
     */
    def input: InputStream = throw new IOException("Cannot read from directory")

    /**
     * Not supported. Always throws an IOException.
     * @throws IOException
     */
    def output: OutputStream = throw new IOException("Cannot write to directory")

    /**
     * @returns  the child nodes of this nodes which are either nt:file or nt:folder
     */
    def iterator: Iterator[JcrNode] =
      new Iterator[Node] {
        val childs = node.getNodes
        def hasNext = childs.hasNext
        def next = childs.next.asInstanceOf[Node]
      }
      .filter((node: Node) =>
        "nt:file" == node.getPrimaryNodeType.getName ||
        "nt:folder" == node.getPrimaryNodeType.getName)
      .map((node: Node) => JcrFS.create(node))

    /**
     * Considers only child nodes which are wither nt:file or nt:folder
     */
    def lookupName(name: String, directory: Boolean): AbstractFile = {
      if (node.hasNode(name)) {
        val n = node.getNode(name)
        if (directory && "nt:folder" == n.getPrimaryNodeType.getName ||
           !directory && "nt:file" == n.getPrimaryNodeType.getName)
          JcrFS.create(n)
        else
          null
      }
      else
        null
    }

    /**
     * Creates a child node of type nt:file with the given name if such node exists.
     * @param name
     * @returns  the (possibly newly created) child node
     */
    override def fileNamed(name: String): AbstractFile = {
      valueOrElse(lookupName(name, false)) {
        val file = node.addNode(name, "nt:file")
        val content = file.addNode("jcr:content", "nt:resource")
        content.setProperty("jcr:mimeType", "application/octet-stream") // todo fix: dont hc MIME
        content.setProperty("jcr:data", emptyInputStream)
        content.setProperty("jcr:lastModified", System.currentTimeMillis)
        node.save()
        JcrFS.create(file)
      }
    }

    /**
     * Creates a child node of type nt:folder with the given name if such node exists.
     * @param name
     * @returns  the (possibly newly created) child node
     */
    override def subdirectoryNamed(name: String): AbstractFile = {
      valueOrElse(lookupName(name, true)) {
        val dir = node.addNode(name, "nt:folder")
        node.save()
        JcrFS.create(dir)
      }
    }
  }

  case class JcrFile(node: Node) extends JcrNode(node) {

    /**
     * @returns  the jcr:content child node of this node or null if none exists
     */
    def contentNode: Node =
      if (node.hasNode("jcr:content")) node.getNode("jcr:content")
      else null

    /**
     * @returns  the jcr:data property of the jcr:content child node of this node or
     *   null if none exists
     */
    def dataProperty: Property =
      nullOrElse(contentNode) { node =>
        if (node.hasProperty("jcr:data")) node.getProperty("jcr:data")
        else null
      }

    /**
     * @returns  false
     */
    def isDirectory = false

    /**
     * @returns  a stream for reading from {@ling #dataProperty}. The stream is empty
     *   if the property does not exist.
     */
    def input: InputStream = dataProperty match {
      case prop: Property => prop.getStream
      case null => emptyInputStream
    }

    /**
     * @returns  a stream for writing to {@ling #dataProperty}. If necessary the child
     *   jcr:content child node and its jcr:data property are created.
     */
    def output: OutputStream = new ByteArrayOutputStream() {
      override def close() {
        super.close()
        val content = valueOrElse(contentNode) {
          node.addNode("jcr:content", "nt:resource")
        }
        content.setProperty("jcr:lastModified", System.currentTimeMillis)
        content.setProperty("jcr:data", new ByteArrayInputStream(buf, 0, size))
        node.save()
      }
    }

    /**
     * @returns the size of the {@ling #dataProperty} or None if the property does not
     * exist.
     */
    override def sizeOption: Option[Int] = {
      val p = dataProperty
      if (p == null) None
      else Some(p.getLength.toInt)
    }

    def iterator: Iterator[AbstractFile] = Iterator.empty
    def lookupName(name: String, directory: Boolean): AbstractFile = null
  }

}