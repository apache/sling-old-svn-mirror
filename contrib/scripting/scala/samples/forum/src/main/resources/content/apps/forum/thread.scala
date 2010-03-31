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

class thread(args: threadArgs) {
  import scala.xml.NodeSeq  
  import scala.xml.NodeSeq.Empty
  import javax.jcr.Node
  import utils._
  import utils.RichJCR._
  import args._

  def javascript(function: String, args: String*) = 
    args.mkString("javascript:" + function + "('", "', '", "')")
  
  def versions(node: Node) = {
    <span id={ node.uuid + "showversionlink" }>
      <a href={ javascript("showVersions", node.uuid) }>old versions</a>
   </span>
   
   <span style="display:none" id={ node.uuid + "hideversionlink" }><p>
     <a href={ javascript("hideVersions", node.uuid) }>hide versions</a>
   </p></span>
   
   <div style="display:none" id={ node.uuid + "versions" }>
     
   {
     for (version <- node.versions; 
          if version.predecessors.length > 0; 
          if version.successors.length > 0) yield {
       
       <hr />
       <p>version created at: { version.created }</p>
       <p> { version.frozenNode("subject") } </p> 
       <p> { version.frozenNode("body") } </p> 
     }
   }
   
   <hr />
   </div>
  }
  
  def detail(node: Node): NodeSeq = {
    val threadPath = node.path.split("/").take(4).mkString("/")
    
    <p>
      {
        emptyUnless(node.hasNode("logo")) {
          <img src={ node.getNode("logo").path + "/jcr:content" } />
        }
      }
    </p>
    <p>
      <span id={ node.uuid + "body" }>
        <a name={ node.uuid } /> 
        <p> { node("subject") } </p>
        <p> { node("body") } </p>
      </span>
    </p>
      
    <p>
      <span id={ node.uuid + "showeditlink" }>
        <a href={ javascript("showEdit", node.uuid) }> edit</a>
      </span>
      
      <span style="display:none" id={ node.uuid + "editform" }>
        <form action={ node.path } method="POST">
          <p><input type="text" name="subject" value={ node("subject") } /></p>
          <textarea rows="3" name="body">{ node("body") }</textarea>
          <p>
            <input type="submit" value="update" /> 
            <a href={ javascript("cancelEdit", node.uuid) }>cancel</a>
          </p>
          <input name=":redirect" value={ threadPath + ".thread.html#" + node.uuid } type="hidden" />    
        </form>
      </span>
    </p>
    
    <p>created: { node.baseVersion.created }</p>
    
    <div>
    {
      emptyUnless(node.versions.getSize > 2) {
        <p> { versions(node) } </p>
      }
    }
    </div>
    
    <p><span id={ node.uuid + "showformlink" }>
      <a href={ javascript("showComment", node.uuid) }>add comment</a>
    </span></p>
    
    <div style="display:none" id={ node.uuid + "form" }>
      <form action={{ node.path } + "/*" } method="POST">
        <p><input type="text" name="subject" /></p>
        <textarea rows="2" name="body"></textarea>
        <p>
          <input type="submit" value="submit comment" /> 
          <a href={ javascript("cancelComment", node.uuid) }>cancel</a>
        </p>
        <input name=":redirect" value={ threadPath + ".thread.html#" + node.uuid } type="hidden" />    
      </form>
    </div>
    
    <ul>
    {
      node.nodes.filter(_.getPrimaryNodeType.getName != "nt:file") map {node => { 
        <li> { detail(node) } </li>} 
      } 
    }
    </ul>
  }

  println {
    <html>
      <head>
        <link rel="stylesheet" href="/apps/forum/static/blue.css" />
        <script type="text/javascript"><!--
            function showComment(id) {
                document.getElementById(id + "showformlink").style.display = 'none';
                document.getElementById(id + "form").style.display = 'block';
            }

            function cancelComment(id) {
                document.getElementById(id + "showformlink").style.display = 'block';
                document.getElementById(id + "form").style.display = 'none';
            }
    
            function showEdit(id) {
                document.getElementById(id + "showeditlink").style.display = 'none';
                document.getElementById(id + "body").style.display = 'none';
                document.getElementById(id + "editform").style.display = 'block';
            }
    
            function cancelEdit(id)  {
                document.getElementById(id + "showeditlink").style.display = 'block';
                document.getElementById(id + "body").style.display = 'block';
                document.getElementById(id + "editform").style.display = 'none';
            }
    
            function showVersions(id) {
                document.getElementById(id + "showversionlink").style.display = 'none';
                document.getElementById(id + "hideversionlink").style.display = 'block';
                document.getElementById(id + "versions").style.display = 'block';
            }
    
            function hideVersions(id) {
                document.getElementById(id + "showversionlink").style.display = 'block';
                document.getElementById(id + "hideversionlink").style.display = 'none';
                document.getElementById(id + "versions").style.display = 'none';
            }
        --></script>
      </head>
      
      <body>
        <div id="Header">
          <a href="/content/forum.html">&lt; back to thread overview</a>
        </div>
        { SearchBox.render(request) } 
        <div id="Content">
        { detail(currentNode) }
        </div>
      </body>
    </html>
  }
  
}

}