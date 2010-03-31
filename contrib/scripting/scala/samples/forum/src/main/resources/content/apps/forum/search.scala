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
  
class search(args: searchArgs) {
  import javax.jcr.Node
  import utils.RichJCR._
  import args._
  
  def javascript(function: String, args: String*) = 
    args.mkString("javascript:" + function + "('", "', '", "')")
  
  def searchDetail(node: Node) = {
    val threadPath = node.path.split("/").take(4).mkString("/")
    val threadUUID = node.session.getNode(threadPath).uuid
    
    <li><p>
      { node("subject") } 
      <a href={ threadPath + ".thread.html#" + node.uuid }> (open thread)</a> 
      <span id={ threadUUID + node.uuid + "link" }>
        <a href={ javascript("expand", threadUUID + node.uuid, threadPath, node.uuid) }>(quick show)</a>
      </span>
      <div id={ threadUUID + node.uuid + "content" }></div>
    </p></li>
  }
  
  val node: Node = currentNode

  println {
    <html>
      <head>
        <link rel="stylesheet" href="/apps/forum/static/blue.css" />
      </head>
      <body>
        <div id="Header">
          <a href="/content/forum.html">&lt; back to thread overview</a>
        </div>
        { SearchBox.render(request) } 
        <script type="text/javascript"><!--
            var xmlHttp = null;
            // Mozilla, Opera, Safari, Internet Explorer 7
            if (typeof XMLHttpRequest != 'undefined') {
                xmlHttp = new XMLHttpRequest();
            }
            if (!xmlHttp) {
                // Internet Explorer 6
                try {
                    xmlHttp  = new ActiveXObject("Msxml2.XMLHTTP");
                } catch(e) {
                    try {
                        xmlHttp  = new ActiveXObject("Microsoft.XMLHTTP");
                    } catch(e) {
                        xmlHttp  = null;
                    }
                }
            }

            function nodePrinter(nodes, hilight) {
                var ret = "<ul>";
                if(nodes["jcr:uuid"] == hilight) {
                    ret += '<li><p><span style="background-color: #FFFF00">' + nodes["subject"] + "</span></p></li>";
                } else {
                    ret += '<li><p>' + nodes["subject"] + "</p></li>";
                }
                for (var a in nodes) {
                    if(nodes[a]["jcr:primaryType"] == "nt:unstructured") {
                        ret += nodePrinter(nodes[a], hilight);
                    }
                }
                ret += "</ul>";
                return ret;
            }

            function expand(id, path, hilight) {
                document.getElementById(id + "link").style.display = 'none';
                document.getElementById(id + "content").innerHTML = 'loading...';
                
                // now some ajax
                // the selector and extension '.-1.json' will return the complete thread in json format
                xmlHttp.open("GET", path + '.-1.json', true);
                xmlHttp.onreadystatechange = function(){            
                    switch(xmlHttp.readyState) {
                        case 4:
                            if(xmlHttp.status!=200) {
                                alert("error:"+xmlHttp.status); 
                            }else{    
                                document.getElementById(id + "content").innerHTML = nodePrinter(eval('(' +xmlHttp.responseText + ')'), hilight);
                            }
                            break;
                    
                        default:
                            return false;
                            break;     
                    }
                };
                xmlHttp.send(null);
            }
        --></script>

       <div id="Content">
       {
         val result = SearchBox.query(node.session, request)
         <h1>search results for query { request.getParameter("qt") }, hits: { result.getSize() } </h1>
         <ul>
         { result map searchDetail }
         </ul>
       }
       </div>
     </body>
    </html>
  }
}

}