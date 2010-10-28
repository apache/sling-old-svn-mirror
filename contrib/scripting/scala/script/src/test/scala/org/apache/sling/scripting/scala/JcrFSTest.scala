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

import java.io.{PrintWriter, InputStreamReader}
import javax.jcr.{Session, Repository, Node, SimpleCredentials}
import junit.framework.TestCase
import junit.framework.Assert.{assertEquals, assertTrue, assertFalse}
import org.apache.sling.scripting.scala.JcrFS.{JcrNode, JcrFile, JcrFolder}
import org.apache.jackrabbit.core.{TransientRepository}

class JcrFSTest extends TestCase {
  var session: Session = null
  var repository: Repository = null
  var testRoot: Node = null

  override def setUp() {
    super.setUp()
    repository = new TransientRepository
    session = repository.login(new SimpleCredentials("admin", "admin".toCharArray))
    testRoot = session.getRootNode.addNode("testRoot", "nt:folder")
    session.save()
  }

  override def tearDown() {
    testRoot.remove()
    testRoot = null
    session.save()
    session.logout()
    session = null
    repository = null
    super.tearDown()
  }

  def testTraverse: Unit = {
    def traverse(entry: JcrNode): String = {
      (for (entry <- entry.elements) yield {entry match {
        case file: JcrFile => file.path
        case folder: JcrFolder => folder.path + traverse(folder)
      }})
      .mkString("(", ",", ")")
    }

    var _id = 0
    def id() = {
      _id += 1
      _id
    }

    def addChildren(folder: Node) = {
      folder.addNode("file" + id(), "nt:file")
      folder.addNode("file" + id(), "nt:file")
      (folder.addNode("folder" + id(), "nt:folder"), folder.addNode("folder" + id(), "nt:folder"))
    }

    val (f1, f2) = addChildren(testRoot)
    val (f3, f4) = addChildren(f1)
    addChildren(f2)
    addChildren(f3)
    addChildren(f4)
    val actual = traverse(JcrFS.create(testRoot))
    val expected =
      "(/testRoot/file1,/testRoot/file2,/testRoot/folder3(/testRoot/folder3/file5,/testRoot/folder3/file6," +
      "/testRoot/folder3/folder7(/testRoot/folder3/folder7/file13,/testRoot/folder3/folder7/file14," +
      "/testRoot/folder3/folder7/folder15(),/testRoot/folder3/folder7/folder16())," +
      "/testRoot/folder3/folder8(/testRoot/folder3/folder8/file17,/testRoot/folder3/folder8/file18," +
      "/testRoot/folder3/folder8/folder19(),/testRoot/folder3/folder8/folder20()))," +
      "/testRoot/folder4(/testRoot/folder4/file9,/testRoot/folder4/file10,/testRoot/folder4/folder11()," +
      "/testRoot/folder4/folder12()))"
    assertEquals(expected, actual)
  }

  def testCreateFile {
    val root = JcrFS.create(testRoot)
    val file = root.fileNamed("file")
    val fileNode = testRoot.getNode("file")
    assertFalse(file.isDirectory)
    assertEquals("nt:file", fileNode.getPrimaryNodeType.getName)
    assertEquals("file", file.name)
    assertEquals("/testRoot/file", file.path)
    assertEquals(fileNode.getProperty("jcr:content/jcr:lastModified").getLong, file.lastModified)
    assertEquals(fileNode.getProperty("jcr:content/jcr:data").getLength, file.sizeOption.get.toLong)

    val contentNode = fileNode.getNode("jcr:content")
    assertEquals("nt:resource", contentNode.getPrimaryNodeType.getName)

    val input = file.input
    assertEquals(0, input.available)
    assertEquals(-1, input.read)
  }

  def testCreateFolder {
    val root = JcrFS.create(testRoot)
    val folder = root.subdirectoryNamed("folder")
    val folderNode = testRoot.getNode("folder")
    assertTrue(folder.isDirectory)
    assertEquals(0L, folder.lastModified)
    assertEquals("nt:folder", folderNode.getPrimaryNodeType.getName)
    assertEquals("folder", folder.name)
    assertEquals("/testRoot/folder", folder.path)
  }

  def testParent {
    val root = JcrFS.create(testRoot)
    val folder = root.subdirectoryNamed("folder")
    val file = folder.fileNamed("file")
    assertEquals(folder, file.container)
  }

  def testReadWriteContent {
    val root = JcrFS.create(testRoot)
    val file = root.fileNamed("file")
    val contentNode = testRoot.getNode("file/jcr:content")

    val writer = new PrintWriter(file.output)
    writer.print("Hello world")
    writer.close
    assertEquals("Hello world", contentNode.getProperty("jcr:data").getString)
    assertEquals(11, file.sizeOption.get)

    val reader = new InputStreamReader(file.input)
    val c = new Array[Char](32)
    reader.read(c)
    assertEquals("Hello world", new String(c, 0, 11))
  }

}