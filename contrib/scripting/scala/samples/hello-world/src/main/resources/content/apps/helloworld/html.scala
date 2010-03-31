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
package helloworld {

class html(args: htmlArgs) { 
  import scala.xml.NodeSeq
  import java.util.Calendar
  import javax.jcr.Node

  implicit def rich(node: Node) = new {
    def apply(property: String) = node.getProperty(property).getString
    def path = node.getPath
    def nodes = new Iterator[Node] {
      val children = node.getNodes
      def hasNext = children.hasNext
      def next = children.nextNode
    }
  }
  
  class Tree(node: Node) {
    def render: NodeSeq = {
      <pre>{ render(node) }</pre>
    }
    
    private def render(node: Node): String = {
      val children = 
        for (child <- node.nodes) yield 
          render(child)
      
      node.path + "\n" + children.mkString
    }
  }
  object Tree {
    def apply(node: Node) = new Tree(node)
  }

  
  import args._
  
  val node: Node = currentNode
  
  println {
    <html>
      <h1>{ "Hello " + node("title") }</h1>
      Today is { Calendar.getInstance.getTime } <br />
      My path is { node.path } <br />
      : { resource.adaptTo(classOf[javax.jcr.Node]).getProperty("title").getValue().getString() } <br />
      { Tree(currentNode).render }
    </html>  
  }

}

}
