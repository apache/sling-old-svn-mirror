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
package forum { 

class POST(args: POSTArgs) {
  import java.util.Date
  import java.io.ByteArrayInputStream
  import java.util.Calendar
  import javax.jcr.Session
  import javax.jcr.Node
  import org.apache.sling.api.resource.ResourceUtil
  import utils.RichJCR._
  import args._
  
  /**
   * Add a child node inclusive all intermediate nodes to a node
   * @param parent  Parent node where the child node will be added 
   * @param path  Path to the child node as list of strings. 
   */
  def addNodes(parent: Node, path: List[String]): Node = path match {
    case name::names => 
      if (parent.hasNode(name)) addNodes(parent.getNode(name), names)
      else {
        if (parent.isNodeType("mix:versionable")) {
          parent.checkout()
          val n = addNodes(parent.addNode(name), names) 
          n.addMixin("mix:referenceable")
          n.addMixin("mix:versionable")
          parent.save()
          parent.checkin()
          n
        }
        else {
          val n = addNodes(parent.addNode(name), names) 
          n.addMixin("mix:referenceable")
          n.addMixin("mix:versionable")
          parent.save()
          n
        }
      }
    case Nil => parent
  }
  
  val session = request.getResourceResolver.adaptTo(classOf[Session])
  val path = resource.getPath
  
  val newPath: List[String] = {
    def validJcrChar(c: Char)  = 
      if ("abcdefghijklmnopqrstuvwxyz0123456789_ ".contains(c)) c
      else '_' 
    
    val nodeName = request("subject").toLowerCase map validJcrChar 
    val parentPath = ResourceUtil.getParent(path)  
    List(parentPath.split("/"):_*).filter(_ != "") ::: List(nodeName.mkString)
  }
  
  val node = 
    if(ResourceUtil.isStarResource(resource)) { 
      val node = addNodes(session.root, newPath)
      
      if (request("logo") != "") {
        node.checkout()
        val logoNode = node.addNode("logo", "nt:file")
        val contentNode = logoNode.addNode("jcr:content", "nt:resource")
        val logo = request.getRequestParameter("logo")
        contentNode.setProperty("jcr:data", logo.getInputStream)
        contentNode.setProperty("jcr:lastModified", Calendar.getInstance)    
        contentNode.setProperty("jcr:mimeType", logo.getContentType)
        node.save()
        node.checkin()
      }   
      node
    }
    else {
      session.getNode(path)
    }

  node.checkout
  node.setProperty("body", request("body"))
  node.setProperty("subject", request("subject"))

  session.save
  node.checkin
  
  response.sendRedirect(request(":redirect"))
}

}