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
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.testing.jcr.RepositoryTestBase;
import org.apache.sling.commons.testing.jcr.RepositoryUtil;
import org.apache.sling.jcr.resource.JcrResourceUtil;
import org.apache.sling.jcr.resource.internal.helper.jcr.JcrNodeResource;
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

        RepositoryUtil.registerNodeType(getSession(), getClass()
                .getResourceAsStream("/SLING-INF/nodetypes/jcrlanguage.cnd"));
        RepositoryUtil.registerNodeType(getSession(), getClass()
                .getResourceAsStream("/SLING-INF/nodetypes/message.cnd"));

        resolver = new ResourceResolver() {

            public Iterator<Resource> findResources(String query,
                    String language) {
                try {
                    final Query q = getSession().getWorkspace().getQueryManager().createQuery(query, language);
                    final QueryResult result = q.execute();
                    final NodeIterator nodes = result.getNodes();
                    return new Iterator<Resource>() {
                        public boolean hasNext() {
                            return nodes.hasNext();
                        }

                        public Resource next() {
                            Node node = nodes.nextNode();
                            try {
                                return new JcrNodeResource(resolver, node, null ,null);
                            } catch (RepositoryException e) {
                                throw new IllegalStateException(e);
                            }
                        }

                        public void remove() {
                            throw new UnsupportedOperationException("remove");
                        }
                    };
                } catch (NamingException ne) {
                    return null;
                } catch (RepositoryException re) {
                    return null;
                }
            }

            public Resource getResource(Resource base, String path) {
                // TODO Auto-generated method stub
                return null;
            }

            public Resource getResource(String path) {
                // TODO Auto-generated method stub
                return null;
            }

            public String[] getSearchPath() {
                return new String[] {"/apps/", "/libs/"};
            }

            public Iterator<Resource> listChildren(Resource parent) {
                // TODO Auto-generated method stub
                return null;
            }

            public String map(HttpServletRequest request, String resourcePath) {
                // TODO Auto-generated method stub
                return null;
            }

            public String map(String resourcePath) {
                // TODO Auto-generated method stub
                return null;
            }

            public Iterator<Map<String, Object>> queryResources(String query,
                    String language) {
                try {
                    final Query q = getSession().getWorkspace().getQueryManager().createQuery(query, language);
                    final QueryResult result = q.execute();
                    final String[] colNames = result.getColumnNames();
                    final RowIterator rows = result.getRows();
                    return new Iterator<Map<String, Object>>() {
                        public boolean hasNext() {
                            return rows.hasNext();
                        }

                        public Map<String, Object> next() {
                            Map<String, Object> row = new HashMap<String, Object>();
                            try {
                                Value[] values = rows.nextRow().getValues();
                                for (int i = 0; i < values.length; i++) {
                                    Value v = values[i];
                                    if (v != null) {
                                        row.put(colNames[i],
                                            JcrResourceUtil.toJavaObject(values[i]));
                                    }
                                }
                            } catch (RepositoryException re) {
                                // ignore
                            }
                            return row;
                        }

                        public void remove() {
                            throw new UnsupportedOperationException("remove");
                        }
                    };
                } catch (NamingException ne) {
                    return null;
                } catch (RepositoryException re) {
                    return null;
                }
            }

            public Resource resolve(HttpServletRequest request, String absPath) {
                // TODO Auto-generated method stub
                return null;
            }

            public Resource resolve(HttpServletRequest request) {
                // TODO Auto-generated method stub
                return null;
            }

            public Resource resolve(String absPath) {
                // TODO Auto-generated method stub
                return null;
            }

            public <AdapterType> AdapterType adaptTo(Class<AdapterType> type) {
                // TODO Auto-generated method stub
                return null;
            }

            public void close() {
                // nothing to do
            }

            public String getUserID() {
                return null;
            }

            public boolean isLive() {
                return true;
            }

            public ResourceResolver clone(Map<String, Object> authenticationInfo) {
                return null;
            }

            public Iterator<String> getAttributeNames() {
                return null;
            }

            public Object getAttribute(String name) {
                return null;
            }
        };

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


    // test data to add to the repository (use linked hash map for insertion order)
    public static final Map<String, Message> MESSAGES_DE = new LinkedHashMap<String, Message>();
    public static final Map<String, Message> MESSAGES_EN = new LinkedHashMap<String, Message>();
    public static final Map<String, Message> MESSAGES_EN_DASH_US = new LinkedHashMap<String, Message>();
    public static final Map<String, Message> MESSAGES_EN_UNDERSCORE_UK = new LinkedHashMap<String, Message>();
    public static final Map<String, Message> MESSAGES_EN_UNDERSCORE_AU = new LinkedHashMap<String, Message>();
    public static final Map<String, Message> MESSAGES_DE_APPS = new LinkedHashMap<String, Message>();
    public static final Map<String, Message> MESSAGES_DE_BASENAME = new LinkedHashMap<String, Message>();

    public static void add(Map<String, Message> map, Message msg) {
        map.put(msg.key, msg);
    }

    public static final Message PARENT_MSG = new Message("", "untranslated", "means: not translated", false);

    // create test data
    static {
        // 1. direct child node of language node, using sling:key
        add(MESSAGES_DE, new Message("", "kitchen", "K�che", false));
        // 2. direct child node of language node, using nodename
        add(MESSAGES_DE, new Message("", "plate", "Teller", true));
        // 3. nested node, using sling:key
        add(MESSAGES_DE, new Message("f", "fork", "Gabel", false));
        // 4. nested node, using nodename
        add(MESSAGES_DE, new Message("s/p/o", "spoon", "L�ffel", true));

        // 5. not present in DE
        add(MESSAGES_EN, PARENT_MSG);

        add(MESSAGES_EN_DASH_US, new Message("", "pigment", "color", false));
        add(MESSAGES_EN_UNDERSCORE_UK, new Message("", "pigment", "colour", false));
        add(MESSAGES_EN_UNDERSCORE_AU, new Message("", "pigment", "colour", false));

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

        // some EN US content
        Node enDashUS = i18n.addNode("en-US", "nt:folder");
        enDashUS.addMixin("mix:language");
        enDashUS.setProperty("jcr:language", "en-US");
        for (Message msg : MESSAGES_EN_DASH_US.values()) {
            msg.add(enDashUS);
        }
        getSession().save();

        // some EN UK content
        Node enUnderscoreUK = i18n.addNode("en_UK", "nt:folder");
        enUnderscoreUK.addMixin("mix:language");
        enUnderscoreUK.setProperty("jcr:language", "en_UK");
        for (Message msg : MESSAGES_EN_UNDERSCORE_UK.values()) {
            msg.add(enUnderscoreUK);
        }
        getSession().save();

        // some EN AU content
        Node enUnderscoreAU = i18n.addNode("en_au", "nt:folder");
        enUnderscoreAU.addMixin("mix:language");
        enUnderscoreAU.setProperty("jcr:language", "en_au");
        for (Message msg : MESSAGES_EN_UNDERSCORE_AU.values()) {
            msg.add(enUnderscoreAU);
        }
        getSession().save();
    }

    // ---------------------------------------------------------------< tests >

    public void test_getString() {
        JcrResourceBundle bundle = new JcrResourceBundle(new Locale("de"), null, resolver);
        for (Message msg : MESSAGES_DE.values()) {
            assertEquals(msg.message, bundle.getString(msg.key));
        }
        
        bundle = new JcrResourceBundle(new Locale("en", "us"), null, resolver);
        for (Message msg : MESSAGES_EN_DASH_US.values()) {
            assertEquals(msg.message, bundle.getString(msg.key));
        }
        
        bundle = new JcrResourceBundle(new Locale("en", "uk"), null, resolver);
        for (Message msg : MESSAGES_EN_UNDERSCORE_UK.values()) {
            assertEquals(msg.message, bundle.getString(msg.key));
        }
        
        bundle = new JcrResourceBundle(new Locale("en", "au"), null, resolver);
        for (Message msg : MESSAGES_EN_UNDERSCORE_AU.values()) {
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
        bundle.setParent(new RootResourceBundle());
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
        parentBundle.setParent(new RootResourceBundle());

        assertEquals(PARENT_MSG.message, bundle.getObject(PARENT_MSG.key));
        assertEquals("missing", bundle.getString("missing"));
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
