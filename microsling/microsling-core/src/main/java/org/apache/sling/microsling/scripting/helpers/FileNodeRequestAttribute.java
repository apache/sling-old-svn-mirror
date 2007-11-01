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
package org.apache.sling.microsling.scripting.helpers;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.microsling.helpers.exceptions.MissingRequestAttributeException;

/** Stores an nt:file Node in a request attribute,
 *  and allows the file's inputStream or reader to
 *  be accessed easily.
 *  Used by scripting SlingServlets to store resolved script nodes
 *  in between canProcess and doX method calls.
 */ 
public class FileNodeRequestAttribute {
    
    private final Node node;
    public static final String REQ_ATTR_NAME = FileNodeRequestAttribute.class.getName();
    
    /** Store this as an attribute of req */
    public FileNodeRequestAttribute(Node n,HttpServletRequest req) {
        node = n;
        req.setAttribute(REQ_ATTR_NAME,this);
    }
    
    /** Retrieve a FileNodeRequestAttribute from given request */
    public static FileNodeRequestAttribute getFromRequest(HttpServletRequest req) throws MissingRequestAttributeException {
        final FileNodeRequestAttribute result = 
            (FileNodeRequestAttribute)req.getAttribute(REQ_ATTR_NAME);
        if(result==null) {
            throw new MissingRequestAttributeException(REQ_ATTR_NAME);
        }
        return result;
    }
    
    /** Return our nt:file node */
    public Node getNode() {
        return node;
    }

    /** Return an InputStream that provides our node's file content */
    public InputStream getInputStream() throws RepositoryException {
        // TODO need more robust checks
        return node.getNode("jcr:content").getProperty("jcr:data").getStream();
    }
    
    /** Return a Reader that provides our node's file content */
    public Reader getReader() throws RepositoryException {
        return new InputStreamReader(getInputStream());
    }
}
