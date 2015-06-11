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

/**
 *  The sling javascript client gives access to a JCR repository
 *  from client-side java code, using the sling post servlet as a back-end.    
 *   
 * @version $Rev: $, $Date: 2007-03-27 16:30:52 +0200 (Tue, 27 Mar 2007) $
 */

var Sling = null;

// start sling code scope
(function() {

    Sling = new Object();
    Sling.NAME_OF_THIS_FILE = "sling.js";
    Sling.PATH_OF_THIS_FILE = "/system/sling.js";
    
    /** This method tries to figure out what to do with a page */
    Sling.wizard = function() {
        //TODO add lots of magic here
        var form=document.getElementById("slingform");
        if (!form) form=document.forms[0];
        if (form) {
            var sp=new Object();
            sp.formElement=form;
            Sling.setupPage(sp);
        }
    
    }
    /** Call this to merge sling data in an HTML page
        TODO deprecate other functions
    */
    Sling.setupPage = function(options) {
      var tree = Sling.getContent(Sling._getJsonUrl(),1);
      
      if(options.formElement) {
        Sling._setFormValues(options.formElement,Sling._getJsonUrl(),tree);
      }
      
      if(options.displayElement) {
        Sling.displayValues(options.displayElement,tree);
      }
    }
    
    /**
     * HTTP GET XHR Helper
     * @param {String} url The URL
     * @return the XHR object, use .responseText for the data
     * @type String
     */
    Sling.httpGet = function(url) {
        var httpcon = Sling.getXHR();
        if (httpcon) {
            httpcon.open('GET', url, false);
            httpcon.send(null);
            return httpcon;
        } else {
            return null;
        }
    }
    /**
     * Produces a "sort-of-json" string representation of a object
     * for debugging purposes only
     * @param {Object} obj The object
     * @param {int} level The indentation level
     * @return The result
     * @type String
     */
    Sling.dumpObj = function(obj, level) {
        var res="";
        for (var a in obj) {
            if (typeof(obj[a])!="object") {
                res+=a+":"+obj[a]+"  ";
            } else {
                res+=a+": { ";
                res+=Sling.dumpObj(obj[a])+"} ";
            }
        }
        return (res);
    }
    
    /** Produces an aggregate of get all the property names used
     * in a tree as a helper for table oriented display
     * @param {Object} obj The Content Tree object
     * @param {Object} names internal object used for collecting all
     *  the names during the recursion
     * @return An Array of names of properties that exist in a tree
     * @type Array
     */
    Sling.getAllPropNames = function(obj, names) {
        var root=false;
        if (!names) {
            names=new Object();
            root=true;
        }
        for (var a in obj) {
            if (typeof(obj[a])!="object") {
                names[a]="1";
            } else {
                getAllPropNames(obj[a], names);
            }
        }
        if (root) {
            var ar=new Array();
            var i=0;
            for (var a in ar) {
                ar[i]=a;
                i++;
            }
            names=ar;
        }
        return (names);
    }
    
    /** Reads a tree of items given a maxlevel from the repository as JSON
     * @param {String} path Path into the current workspace
     * @param {int} maxlevel maximum depth to traverse to
     * @param {Array} filters filter only these properties
     * @return An Object tree of content nodes and properties, null if not found
     * @type Object
     */
    Sling.getContent = function(path, maxlevels, filter) {
        var obj=new Object();
        if (!path)  {
            path=Sling.currentPath;
        }
        if (path.indexOf("/")==0) {
            /*
            this assumes that paths that start with a slash
            are meant to be workspace paths rather than URLs
            and therefore need some additions before they are sent
            to the server
            */
            if(maxlevels == "0" || maxlevels) {
              maxlevels = "." + maxlevels;
            } else {
              maxlevels = "";
            }
            path=Sling.baseurl + path + maxlevels + ".json";
        }
        //checking for a trailing "/*"
        if (path.indexOf("/*")>=0) return obj;
    
        // TODO for now we explicitely defeat caching on this...there must be a better way
        // but in tests IE6 tends to cache too much
        var passThroughCacheParam = "?clock=" + new Date().getTime();
        var res=Sling.httpGet(path + passThroughCacheParam + (maxlevels?"&maxlevels="+maxlevels:""));
        
        if(res.status == 200) {
            var obj=Sling.evalString(res.responseText);
            if (!filter) {
                for (var a in obj) {
                    if (a.indexOf("jcr:")==0) delete(obj[a]);
                }
            }
            return obj;
        }
        return null; 
    }
    
    /** Remove content by path */
    Sling.removeContent = function(path) {
        var httpcon = Sling.getXHR();
        if (httpcon) {
            var params = ":operation=delete";
            httpcon.open('POST', Sling.baseurl + path, false);

            // Send the proper header information along with the request
            httpcon.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
            httpcon.setRequestHeader("Content-length", params.length);
            httpcon.setRequestHeader("Connection", "close");
            httpcon.send(params);
            return httpcon;
        } else {
            return false;
        }
    }
    
    /** eval str, accepting various object delimiters */
    Sling.evalString = function(str) {
        var obj = null;
        if(str.indexOf('[')==0) {
            eval("obj="+str);
        } else if(str.indexOf('{')==0) {
            eval("obj="+str);
        } else {
            eval("obj={"+str+"}");
        }
        return obj;
    }
     
    /** Get "session info" from repository. Mainly answers the question: "Who am I"
     *  and "Which workspace am I logged into.
     * @return An Object tree containing the session information, null if server status <> 200
     * @type Object
     */
    Sling.getSessionInfo = function() {
        var res=Sling.httpGet(Sling.baseurl+"/system/sling/info.sessionInfo.json");
        if(res.status == 200) {
            return Sling.evalString(res.responseText);
        }
        return null;
    }
    
    /** Replace extension in a path */
    Sling._replaceExtension = function(path,newExtension) {
        var i = path.lastIndexOf(".");
        if(i >= 0) path = path.substring(0,i);
        i = path.lastIndexOf(".");
        if(i >= 0) path = path.substring(0,i);
        return path + newExtension;
    }
    
    /** Get the JSON data URL that for the current page
     *  (assuming a .extension for the current page, .html or something else)   
     */
    Sling._getJsonUrl = function() {
      return Sling._replaceExtension(window.location.href,".json");
    }
    
    /** Get the content repository path from the URL
     *  (assuming a .extension for the current page, .html or something else)
     */
    Sling._getPath = function() {
    
        var noextensions=Sling._replaceExtension(window.location.href,"");
        var path=noextensions.substring(Sling.baseurl.length);
        return (path);
    }
    
    /** Display values inside a container: an element inside given container,
     *  with an id like ./stuff, has its innerHTML set to the value of stuff
     *  in the tree, if it exists.
     */
    Sling.displayValues = function(container,tree) {
      if(!tree) {
        tree = Sling.getContent(Sling._getJsonUrl(),1);
      }
      
      var elements = container.getElementsByTagName("*"); 
      var toSet = new Array();
      for (var i = 0; i < elements.length; i++) { 
        var value = Sling._getElementValue(elements[i],tree);
        if(value) {
          toSet[toSet.length] = { e:elements[i], v:value };
        }
      }
      
      for(var i = 0; i < toSet.length; i++) {
        toSet[i].e.innerHTML = toSet[i].v;
      }
    }
    
    /** If e has an ID that matches a property of tree, set e's innerHTML accordingly */
    Sling._getElementValue = function(e,tree) {
      var id = e.getAttribute("id");
      if(id) {
        return tree[id.substring(2)];
      }
    }
    
      
    /** Set form elements based on the tree of items passed into the method
     * @param {IdOrElement} form the Form element to set, or its id
     * @param {String} path passes a string specifying the path
     * @param {Object} tree optionally pass the content that you want the
     * form to be populated with. This assumes an item tree as returned by
     * getContent().
     * Returns an object indicating whether data was found on the server.
     *
     */
    Sling._setFormValues = function(form, path, tree) {
        var result = new Object();
        
        /** TODO: deal with abolute paths?
         *  TODO: deal with @ValueFrom
         */
        if (!path) return;
    
        form.setAttribute("action", path);
    
        if (!tree) {
            tree=Sling.getContent(path,1);
        }
    
        var elems=form.elements;
        var i=0;
        formfieldprefix="";
    
        while (elems.length > i) {
            var elem=elems[i];
            var a=elem.name;
            if (a.indexOf("./")==0) {
                formfieldprefix="./";
                break;
            }
            i++;
        }
    
        var i=0;
        while (elems.length > i) {
            var elem=elems[i];
            var a=elem.name;
            
            if (a.indexOf("/")==0) {
                var nodepath=a.substring(0,a.lastIndexOf("/"));
                var propname=a.substring(a.lastIndexOf("/")+1);
                var node=Sling.getContent(nodepath);
                var propval=node[propname];
            } else if (a.indexOf(formfieldprefix)==0) {
                var propname=a.substring(formfieldprefix.length);
                var propval=tree[propname];
            }
            
            if (propval) {
                if (elem.type == "file") {
                    // cannot edit uplodaded files for now
                } else if (elem.type == "checkbox") {
                    var vals;
                    if (typeof(propval)=="object") vals=propval;
                    else {
                        vals=new Array();
                        vals[0]=propval;
                    }
                    var j=0;
                    while (vals.length > j) {
                        if (vals[j] == elem.value) elem.checked=true;
                        j++;
                    }
                 } else {
                    elem.value=propval;
                 }
            }
            i++;
        }
        
    }
    
    /** return Path as specified as the URL Parameter
     *  @param URL
     *  @return The Path parameter isolated from the URL
     *  @type String
     */
    Sling.TODO_NOT_USED_isolatePathFromUrl = function(url) {
      var pattern = "[\\?&]Path=([^&#]*)";
      var regex = new RegExp( pattern );
      var results = regex.exec( url );
      if( results == null )
            // none found
            return "";
      else
            // found
            return unescape(results[1]);
    }
    
    /**
     *  Get an XMLHttpRequest in a portable way
     *      
     */
    Sling.getXHR = function () {
        var xhr=null;
        
        if(!xhr) {
            try {
                // built-in (firefox, recent Opera versions, etc)
                xhr=new XMLHttpRequest();
            } catch (e) {
                // ignore
            }
        }
        
        if(!xhr) {
            try {
                // IE, newer versions
                xhr=new ActiveXObject("Msxml2.XMLHTTP");
            } catch (e) {
                // ignore
            }
        }
        
        if(!xhr) {
            try {
                // IE, older versions
                xhr=new ActiveXObject("Microsoft.XMLHTTP");
            } catch (e) {
                // ignore
            }
        }
        
        if(!xhr) {
            alert("Unable to access XMLHttpRequest object, sling will not work!");
        }
        
        return xhr;
    }
    
    // obtain the base_url to communicate with sling on the server
    var scripts = document.getElementsByTagName("SCRIPT")
    for (var i = 0; i < scripts.length; i++) {
        var scriptSrc = scripts[i].src
        if (scriptSrc.match(Sling.PATH_OF_THIS_FILE+"$")) {
            Sling.baseurl = scriptSrc.substring(0,scriptSrc.length - Sling.PATH_OF_THIS_FILE.length);
            Sling.currentPath = Sling._getPath();
            Sling.isNew  = (Sling.currentPath.indexOf("/*")>=0)?true:false;
    
            break;
        }
    }

// end sling code scope 
})();
