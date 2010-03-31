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
import javax.jcr.Node
import javax.jcr.SimpleCredentials
import javax.jcr.Session

import junit.framework.TestCase
import junit.framework.Assert.assertEquals

import org.apache.jackrabbit.core.TransientRepository

import org.apache.sling.scripting.scala.Utils.valueOrElse
import org.apache.sling.scripting.scala.JcrFS

package org.apache.sling.scripting.scala.interpreter {

/** 
 * JCR based test cases where files are read/written to/from a JCR repository 
 * (Apache Jackrabbit).
 */  
class InterpreterWithJcrTest extends TestCase with Tests {
  var interpreterHelper: InterpreterHelper = null
  var repository: TransientRepository = null
  var session: Session = null
  var workNode: Node = null
  
  override def setUp() {
    super.setUp();
    
    repository = new TransientRepository
    session = repository.login(new SimpleCredentials("admin", "admin".toCharArray))
    val rootNode = session.getRootNode
    workNode = rootNode.addNode("scala_tests", "nt:folder");
    workNode.getSession().save();
    val workDir = JcrFS.create(workNode);
    val srcDir = workDir.subdirectoryNamed("src")
    val outDir = workDir.subdirectoryNamed("classes")
    
    interpreterHelper = new InterpreterHelper(srcDir, outDir) {
      override def getClasspath = valueOrElse(System.getProperty("surefire.test.class.path")) {
        super.getClasspath
      }
    }
  }
  
  override def tearDown() {
    interpreterHelper = null
    session.logout()
    repository.shutdown
    repository = null
    super.tearDown()
  }

  def testNodeAccess() {
    val code = "package a { class Testi(args: TestiArgs) { import args._; print(n.getPath)}}"
    val bindings = Bindings()
    bindings.putValue("n", workNode)
    assertEquals(workNode.getPath, interpreterHelper.eval("a.Testi", code, bindings))
  }
  
}

}