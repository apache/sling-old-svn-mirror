/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.testing.mock.sling.context;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeTypeManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NodeTypeDefinitionScanner {
    
    private static final NodeTypeDefinitionScanner SINGLETON = new NodeTypeDefinitionScanner();
    
    private static final int MAX_ITERATIONS = 5;

    private static final Logger log = LoggerFactory.getLogger(NodeTypeDefinitionScanner.class);
    
    private final List<String> nodeTypeDefinitions;
        
    private NodeTypeDefinitionScanner() {
        nodeTypeDefinitions = findeNodeTypeDefinitions();
    }
    
    public List<String> getNodeTypeDefinitions() {
        return nodeTypeDefinitions;
    }

    /**
     * Registers node types found in classpath in JCR repository.
     * @param Session session
     */
    public void register(Session session) throws RepositoryException {
      ClassLoader classLoader = getClass().getClassLoader();
      Workspace workspace = session.getWorkspace();
      NodeTypeManager nodeTypeManager = workspace.getNodeTypeManager();
      NamespaceRegistry namespaceRegistry = workspace.getNamespaceRegistry();
      ValueFactory valueFactory = session.getValueFactory();

      // try registering node types multiple times because the ecact order is not known
      List<String> nodeTypeResources = new ArrayList<String>(NodeTypeDefinitionScanner.get().getNodeTypeDefinitions());
      int iteration = 0;
      while (!nodeTypeResources.isEmpty()) {
          registerAndRemoveSucceeds(nodeTypeResources, classLoader, nodeTypeManager, namespaceRegistry, valueFactory, false);
          iteration++;
          if (iteration >= MAX_ITERATIONS) {
              break;
          }
      }
      if (!nodeTypeResources.isEmpty()) {
          registerAndRemoveSucceeds(nodeTypeResources, classLoader, nodeTypeManager, namespaceRegistry, valueFactory, true);
      }
    }
    
    private void registerAndRemoveSucceeds(List<String> nodeTypeResources, ClassLoader classLoader,
            NodeTypeManager nodeTypeManager, NamespaceRegistry namespaceRegistry, ValueFactory valueFactory,
            boolean logError) {
        Iterator<String> nodeTypeResourcesIterator = nodeTypeResources.iterator();
        while (nodeTypeResourcesIterator.hasNext()) {
            String nodeTypeResource = nodeTypeResourcesIterator.next();
            InputStream is = classLoader.getResourceAsStream(nodeTypeResource);
            if (is == null) {
                continue;
            }
            try {
                Reader reader = new InputStreamReader(is);
                CndImporter.registerNodeTypes(reader, nodeTypeResource, nodeTypeManager, namespaceRegistry, valueFactory, false);
                nodeTypeResourcesIterator.remove();
            }
            catch (Throwable ex) {
                if (logError) {
                    log.warn("Unable to register node type: " + nodeTypeResource, ex);
                }
            }
            finally {
                IOUtils.closeQuietly(is);
            }
        }
    }
    
    private static List<String> findeNodeTypeDefinitions() {
        List<String> nodeTypeDefinitions = new ArrayList<String>();
        try {
            Enumeration<URL> resEnum = NodeTypeDefinitionScanner.class.getClassLoader().getResources(JarFile.MANIFEST_NAME);
            while (resEnum.hasMoreElements()) {
                try {
                    URL url = (URL)resEnum.nextElement();
                    InputStream is = url.openStream();
                    if (is != null) {
                        try {
                            Manifest manifest = new Manifest(is);
                            Attributes mainAttribs = manifest.getMainAttributes();
                            String nodeTypeDefinitionList = mainAttribs.getValue("Sling-Nodetypes");
                            String[] nodeTypeDefinitionArray = StringUtils.split(nodeTypeDefinitionList, ",");
                            if (nodeTypeDefinitionArray != null) {
                                for (String nodeTypeDefinition : nodeTypeDefinitionArray) {
                                    if (!StringUtils.isBlank(nodeTypeDefinition)) {
                                        nodeTypeDefinitions.add(StringUtils.trim(nodeTypeDefinition));
                                    }
                                }
                            }
                        }
                        finally {
                            is.close();
                        }
                    }
                }
                catch (Throwable ex) {
                    log.warn("Unable to read JAR manifest.", ex);
                }
            }
        }
        catch (IOException ex2) {
            log.warn("Unable to read JAR manifests.", ex2);
        }
        return nodeTypeDefinitions; 
    }
    
    public static NodeTypeDefinitionScanner get() {
        return SINGLETON;
    }
    
}
