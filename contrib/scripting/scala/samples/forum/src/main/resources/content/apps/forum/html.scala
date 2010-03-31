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
  
object utils {
  import scala.xml.NodeSeq
  import scala.xml.NodeSeq.Empty
  
  /**
   * @param condition  a condition which determines whether the following block
   *     will execute
   * @param block  a block returning a <code>NodeSeq</codew>
   * @return  the <code>NodeSeq</code> returned by <code>block</code> if 
   *     <code>condition</code> is true. <code>Empty</code> otherwise.
   */
  def emptyUnless(condition: Boolean)(block: => NodeSeq)  = 
    if (condition) block
    else Empty

  /**
   * Rich wrappers. These would normally go into separate support libraries. 
   * To keep things simple they are included directly here.   
   */
  object RichJCR {
    import javax.jcr.Session
    import javax.jcr.Node
    import javax.jcr.NodeIterator
    import javax.jcr.Property
    import javax.jcr.PropertyIterator
    import javax.jcr.ItemNotFoundException
    import javax.jcr.version.Version
    import javax.jcr.version.VersionIterator
    import javax.servlet.http.HttpServletRequest
  
    implicit def iterator(it: NodeIterator): Iterator[Node] = {
      new Iterator[Node] {
        def hasNext = it.hasNext
        def next() = it.next.asInstanceOf[Node]
      }
    }
  
    implicit def iterator(it: PropertyIterator): Iterator[Property] = {
      new Iterator[Property] {
        def hasNext = it.hasNext
        def next() = it.next.asInstanceOf[Property]
      }
    }

    implicit def iterator(it: VersionIterator): Iterator[Version] = {
      new Iterator[Version] {
        def hasNext = it.hasNext
        def next() = it.next.asInstanceOf[Version]
      }
    }
  
    implicit def rich(node: Node) = new {
      def apply(name: String) =
        if (node.hasProperty(name)) node.getProperty(name).getString
        else "" 

      def path = node.getPath
      def nodes = iterator(node.getNodes)
      def properties = iterator(node.getProperties)
      def uuid = node.getUUID
      def session = node.getSession
      def versions = node.getVersionHistory.getAllVersions
      def baseVersion = node.getBaseVersion
    }
    
    implicit def rich(request: HttpServletRequest) = new {
      def apply(name: String) =  
        if(request.getParameter(name) == null) "" 
        else request.getParameter(name)
   }
    
    implicit def rich(version: Version) = new {
      def predecessors = version.getPredecessors
      def successors = version.getSuccessors
      def frozenNode = version.getNode("jcr:frozenNode")
      def created = version.getCreated.getTime
    }
    
    implicit def rich(session: Session) = new {
      def root = session.getRootNode
      def getNode(absPath: String) = {
        val item = session.getItem(absPath)
        if (!item.isNode) throw new ItemNotFoundException("Node: " + absPath)
        item.asInstanceOf[Node]
      }
    }
  }

}

/**
 * Search box component 
 */
object SearchBox {
  import javax.jcr.Session
  import javax.jcr.query.Query
  import org.apache.sling.api.SlingHttpServletRequest
  import utils.RichJCR._
  
  /** 
   * Name of the request parameter containing the query text
   */
  val QUERY_PARAM = "query"

  /**
   * Render the search box component to HTML. Uses <code>QUERY_PARAM</code> to 
   * extract the query text from the request. 
   * @param request  The <code>SlingHttpServletRequest</code> which renders the
   *     search box.  
   * @return  <code>NodeSeq</code> of the rendered search box.
   */
  def render(request: SlingHttpServletRequest) = {
    <div id="Menu">
      <p>search all threads:
        <form action="/content/forum.search.html" method="GET" enctype="multipart/form-data">
          <input type="text" name={ QUERY_PARAM } size="10" value={ request(QUERY_PARAM) } />
          <input type="submit" value="search" />
        </form>
      </p>
    </div>
  }
  
  /**
   * Execute a query. Uses <code>QUERY_PARAM</code> to extract the query text from the request.
   * @param session  The JCR session against which to execute the query
   * @param request  The <code>SlingHttpServletRequest</code> which executes the query.
   * @return  <code>NodeIterator</code> containg the nodes resulting from the query
   */
  def query(session: Session, request: SlingHttpServletRequest) = {
    val queryString = "/jcr:root/content/forum//*[jcr:contains(., '" + request(QUERY_PARAM)+"')]"
    val query = session.getWorkspace.getQueryManager.createQuery(queryString, Query.XPATH)
    query.execute.getNodes
  }
}

/**
 * New thread form component
 */
object ThreadNewForm {
  
  /**
   * Render the new thread form component to HTML.
   */
  def render = {
    <h1>start a new thread</h1>
    <span id="inp">
      <form action="/content/forum/*" method="POST" enctype="multipart/form-data">
        <p>subject</p>
        <p><input type="text" name="subject" /></p>
        <p><textarea name="body"></textarea></p>
        <p>logo</p>
        <p><input type="file" name="logo" /></p>
        <p><input type="submit" value="save" /></p>
        <input name=":redirect" value="/content/forum.html" type="hidden" />
      </form>
    </span>
  }
}

/**
 * Thread overview component
 */
object ThreadOverview {
  import javax.jcr.Node
  import utils._
  import utils.RichJCR._
  
  /**
   * Render the thread overview component
   */
  def render(node: Node) = 
    emptyUnless(node.hasNodes) {
      <h1>threads</h1>
      <ul>{ node.nodes map toListItem }</ul>
    }
  
  private def toListItem(node: Node) = { 
    <li><p>
      {
        emptyUnless(node.hasNode("logo")) {
          <img src={ node.getNode("logo").path + "/jcr:content" } />
        }
      }
      { node("subject") } 
      (<a href={ node.path + ".thread.html"}>show thread</a>)
    </p></li>    
  }
  
}

class html(args: htmlArgs) {
  import java.util.Calendar
  import javax.jcr.Node
  import utils.RichJCR._
  import args._
  
  val node: Node = currentNode
    
  println {
    <html>
      <head>
        <link rel="stylesheet" href="/apps/forum/static/blue.css" />
      </head>
      <body>
        <div id="Header">
          Welcome to the { node("name") } forum  
          &mdash; { Calendar.getInstance.getTime } 
        </div>
        { SearchBox.render(request) }
        <div id="Content">
          { ThreadNewForm.render }
          { ThreadOverview.render(node) }
        </div>
      </body>
    </html>  
  }

}

}
