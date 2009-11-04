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
package org.apache.sling.i18n.impl;

import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.testing.SlingTestHelper;
import org.apache.sling.commons.testing.jcr.RepositoryTestBase;
import org.apache.sling.commons.testing.jcr.RepositoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests the {@link JcrResourceBundle} class.
 */
public class JcrResourceBundleTest extends RepositoryTestBase {
    
    private static final Logger log = LoggerFactory.getLogger(JcrResourceBundleTest.class);

    protected ResourceResolver resolver;
    
    public void setUp() throws Exception {
        super.setUp();
        
        cleanRepository();
        
        SlingTestHelper.registerSlingNodeTypes(getSession());
        RepositoryUtil.registerNodeType(getSession(), getClass()
                .getResourceAsStream("/SLING-INF/nodetypes/jcrlanguage.cnd"));
        RepositoryUtil.registerNodeType(getSession(), getClass()
                .getResourceAsStream("/SLING-INF/nodetypes/message.cnd"));

        resolver = SlingTestHelper.getResourceResolver(getRepository(),
                getSession());
        
        createTestContent();
    }
    
    public void cleanRepository() throws Exception {
        NodeIterator nodes = getSession().getRootNode().getNodes();
        while (nodes.hasNext()) {
            Node node = nodes.nextNode();
            if (!node.getDefinition().isProtected() && !node.getDefinition().isMandatory()) {
                try {
                    node.remove();
                } catch (RepositoryException e) {
                    log.error("Test clean repo: Cannot remove node: " + node.getPath(), e);
                }
            }
        }
        getSession().save();
    }
    
    // ---------------------------------------------------------------< test data helper >
    
    /**
     * Helper class for creating test data in a generic way.
     */
    public static class Message {
        public String key;
        public String message;
        public boolean useNodeName;
        public String path;
        
        public Message(String path, String key, String message, boolean useNodeName) {
            this.path = path;
            this.key = key;
            this.message = message;
            this.useNodeName = useNodeName;
        }
        
        private static int nodeNameCounter = 0;
        
        public void add(Node languageNode) throws RepositoryException {
            Node node = languageNode;
            String[] pathElements = path.split("/");
            for (String pathStep : pathElements) {
                if (pathStep != null && pathStep.length() > 0) {
                    node = node.addNode(pathStep, "nt:folder");
                }
            }
            if (useNodeName) {
                node = node.addNode(key, "sling:MessageEntry");
            } else {
                node = node.addNode("node" + nodeNameCounter, "sling:MessageEntry");
                nodeNameCounter++;
                node.setProperty("sling:key", key);
            }
            node.setProperty("sling:message", message);
        }
    }
    
    // test data to add to the repository (use linked hash map for insertion order)
    public static final Map<String, Message> MESSAGES_DE = new LinkedHashMap<String, Message>();
    public static final Map<String, Message> MESSAGES_EN = new LinkedHashMap<String, Message>();
    public static final Map<String, Message> MESSAGES_DE_APPS = new LinkedHashMap<String, Message>();
    public static final Map<String, Message> MESSAGES_DE_BASENAME = new LinkedHashMap<String, Message>();
    
    public static void add(Map<String, Message> map, Message msg) {
        map.put(msg.key, msg);
    }
    
    public static final Message PARENT_MSG = new Message("", "untranslated", "means: not translated", false);
    
    // create test data
    static {
        // 1. direct child node of language node, using sling:key
        add(MESSAGES_DE, new Message("", "kitchen", "KŸche", false));
        // 2. direct child node of language node, using nodename
        add(MESSAGES_DE, new Message("", "plate", "Teller", true));
        // 3. nested node, using sling:key
        add(MESSAGES_DE, new Message("f", "fork", "Gabel", false));
        // 4. nested node, using nodename
        add(MESSAGES_DE, new Message("s/p/o", "spoon", "Lšffel", true));
        
        // 5. not present in DE
        add(MESSAGES_DE, PARENT_MSG);

        // 6. same as 1.-4., but different translations for overwriting into apps
        for (Message msg : MESSAGES_DE.values()) {
            add(MESSAGES_DE_APPS, new Message(msg.path, msg.key, "OTHER", msg.useNodeName));
        }

        // 7. same as 1.-4., but different translations for different sling:basename
        for (Message msg : MESSAGES_DE.values()) {
            add(MESSAGES_DE_BASENAME, new Message(msg.path, msg.key, "BASENAME", msg.useNodeName));
        }
    }
    
    public void createTestContent() throws Exception {
        Node i18n = getSession().getRootNode().addNode("libs", "nt:unstructured").addNode("i18n", "nt:unstructured");
        
        // some DE content
        Node de = i18n.addNode("de", "nt:folder");
        de.addMixin("mix:language");
        de.setProperty("jcr:language", "de");
        for (Message msg : MESSAGES_DE.values()) {
            msg.add(de);
        }
        getSession().save();
        
        // some EN content (for parent bundling)
        Node en = i18n.addNode("en", "nt:folder");
        en.addMixin("mix:language");
        en.setProperty("jcr:language", "en");
        for (Message msg : MESSAGES_EN.values()) {
            msg.add(en);
        }
        getSession().save();
        
        //SlingTestHelper.printJCR(getSession());
    }
    
    // ---------------------------------------------------------------< tests >
    
    public void test_getString() {
        JcrResourceBundle bundle = new JcrResourceBundle(new Locale("de"), null, resolver);
        for (Message msg : MESSAGES_DE.values()) {
            assertEquals(msg.message, bundle.getString(msg.key));
        }
    }
    
    public void test_getObject() {
        JcrResourceBundle bundle = new JcrResourceBundle(new Locale("de"), null, resolver);
        for (Message msg : MESSAGES_DE.values()) {
            assertEquals(msg.message, (String) bundle.getObject(msg.key));
        }
    }
    
    public void test_handle_missing_key() {
        // test if key is returned if no entry found in repo
        JcrResourceBundle bundle = new JcrResourceBundle(new Locale("de"), null, resolver);
        assertEquals("missing", bundle.getString("missing"));
    }
    
    public void test_getKeys() {
        JcrResourceBundle bundle = new JcrResourceBundle(new Locale("de"), null, resolver);
        Enumeration<String> keys = bundle.getKeys();
        int counter = 0;
        while (keys.hasMoreElements()) {
            counter++;
            String key = keys.nextElement();
            assertTrue("bundle returned key that is not supposed to be there: " + key, MESSAGES_DE.containsKey(key));
        }
        assertEquals(MESSAGES_DE.size(), counter);
    }
    
    public void test_bundle_parenting() {
        // set parent of resource bundle, test if passed through
        JcrResourceBundle bundle = new JcrResourceBundle(new Locale("de"), null, resolver);
        JcrResourceBundle parentBundle = new JcrResourceBundle(new Locale("en"), null, resolver);
        bundle.setParent(parentBundle);
        
        assertEquals(PARENT_MSG.message, bundle.getObject(PARENT_MSG.key));
    }

    public void test_search_path() throws Exception {
        // overwrite stuff in apps
        Node appsI18n = getSession().getRootNode().addNode("apps").addNode("i18n", "nt:unstructured");
        Node de = appsI18n.addNode("de", "nt:folder");
        de.addMixin("mix:language");
        de.setProperty("jcr:language", "de");
        for (Message msg : MESSAGES_DE_APPS.values()) {
            msg.add(de);
        }
        getSession().save();
        
        // test getString
        JcrResourceBundle bundle = new JcrResourceBundle(new Locale("de"), null, resolver);
        for (Message msg : MESSAGES_DE_APPS.values()) {
            assertEquals(msg.message, bundle.getString(msg.key));
        }
        
        // test getKeys
        Enumeration<String> keys = bundle.getKeys();
        int counter = 0;
        while (keys.hasMoreElements()) {
            counter++;
            String key = keys.nextElement();
            assertTrue("bundle returned key that is not supposed to be there: " + key, MESSAGES_DE_APPS.containsKey(key));
        }
        assertEquals(MESSAGES_DE.size(), counter);
    }
    

    public void test_basename() throws Exception {
        // create another de lib with a basename set
        Node appsI18n = getSession().getRootNode().getNode("libs/i18n");
        Node de = appsI18n.addNode("de_basename", "nt:unstructured");
        de.addMixin("mix:language");
        de.setProperty("jcr:language", "de");
        de.setProperty("sling:basename", "FOO");
        for (Message msg : MESSAGES_DE_BASENAME.values()) {
            msg.add(de);
        }
        getSession().save();
        
        // test getString
        JcrResourceBundle bundle = new JcrResourceBundle(new Locale("de"), "FOO", resolver);
        for (Message msg : MESSAGES_DE_BASENAME.values()) {
            assertEquals(msg.message, bundle.getString(msg.key));
        }
        
        // test getKeys
        Enumeration<String> keys = bundle.getKeys();
        int counter = 0;
        while (keys.hasMoreElements()) {
            counter++;
            String key = keys.nextElement();
            assertTrue("bundle returned key that is not supposed to be there: " + key, MESSAGES_DE_BASENAME.containsKey(key));
        }
        assertEquals(MESSAGES_DE.size(), counter);
    }
}
