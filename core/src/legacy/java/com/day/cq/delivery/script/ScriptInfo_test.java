/**
 * $Id: ScriptInfo_test.java 23668 2006-11-28 07:22:27Z fmeschbe $
 *
 * Copyright 1997-2004 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package com.day.cq.delivery.script;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.MergeException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import javax.security.auth.Subject;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import junit.framework.TestCase;

import com.day.sling.CmsService;
import com.day.sling.legacy.DeliveryHttpServletRequest;
import com.day.sling.legacy.MappedURL;
import com.day.sling.script.ScriptInfo;
import com.day.util.Finalizer;

/**
 *
 * @version $Revision$
 * @author fmeschbe
 * @since coati
 * @audience core
 */
public class ScriptInfo_test extends TestCase {

    private final TestDHSRequest testReq = new TestDHSRequest();

    private String[] nullMethod = null;
    private String[] starMethod = { "*" };
    private String[] star2Method = { "get", " post", "*", "head" };
    private String[] listMethod = { "\t\nGET\r", "Post", "order" };

    private static final String[] testMethods = { null, "", "GET", "get", "gEt",
	"PUT", "post", "head", "options" };

    private ScriptInfo nullMethods;
    private ScriptInfo starMethods;
    private ScriptInfo star2Methods;
    private ScriptInfo listMethods;

    private SortedMap set0;
    private SortedMap set1;
    private SortedMap set2;
    private SortedMap set3;
    private SortedMap set4;

    int[] order0 = { 2, 0, 1, 3 };
    int[] order1 = { 12, 2, 0, 10, 11, 13};
    int[] order2 = { 20, 23, 22, 12, 2, 0, 10, 11, 21, 13 };
    int[] order3 = { 30, 31, 32, 33, 34, 35, 36, 37 };
    int[] order4 = { 30, 31, 23, 32, 33, 34, 35, 36, 12, 2, 0, 10, 11, 37, 13 };

    public ScriptInfo_test(String name) {
	super(name);
    }

    private boolean doMatch(ScriptInfo si, String method, String query) {
        return si.matches(testReq.t(method, query));
    }

    public void testnullMethods0() {
        assertFalse(testMethods[0], doMatch(nullMethods, testMethods[0], "Par.0001.Image"));
    }

    public void testnullMethods1() {
        assertFalse(testMethods[1], doMatch(nullMethods, testMethods[1], "Par.0001.Image"));
    }

    public void testnullMethods2() {
        assertTrue(testMethods[2], doMatch(nullMethods, testMethods[2], "Par.0001.Image"));
    }

    public void testnullMethods3() {
        assertTrue(testMethods[3], doMatch(nullMethods, testMethods[3], "Par.0001.Image"));
    }

    public void testnullMethods4() {
        assertTrue(testMethods[4], doMatch(nullMethods, testMethods[4], "Par.0001.Image"));
    }

    public void testnullMethods5() {
        assertFalse(testMethods[5], doMatch(nullMethods, testMethods[5], "Par.0001.Image"));
    }

    public void testnullMethods6() {
        assertTrue(testMethods[6], doMatch(nullMethods, testMethods[6], "Par.0001.Image"));
    }

    public void testnullMethods7() {
        assertFalse(testMethods[7], doMatch(nullMethods, testMethods[7], "Par.0001.Image"));
    }

    public void testnullMethods8() {
        assertFalse(testMethods[8], doMatch(nullMethods, testMethods[8], "Par.0001.Image"));
    }

    public void teststarMethods0() {
        assertTrue(testMethods[0], doMatch(starMethods, testMethods[0], "Par.0001.Image"));
    }

    public void teststarMethods1() {
        assertTrue(testMethods[1], doMatch(starMethods, testMethods[1], "Par.0001.Image"));
    }

    public void teststarMethods2() {
        assertTrue(testMethods[2], doMatch(starMethods, testMethods[2], "Par.0001.Image"));
    }

    public void teststarMethods3() {
        assertTrue(testMethods[3], doMatch(starMethods, testMethods[3], "Par.0001.Image"));
    }

    public void teststarMethods4() {
        assertTrue(testMethods[4], doMatch(starMethods, testMethods[4], "Par.0001.Image"));
    }

    public void teststarMethods5() {
        assertTrue(testMethods[5], doMatch(starMethods, testMethods[5], "Par.0001.Image"));
    }

    public void teststarMethods6() {
        assertTrue(testMethods[6], doMatch(starMethods, testMethods[6], "Par.0001.Image"));
    }

    public void teststarMethods7() {
        assertTrue(testMethods[7], doMatch(starMethods, testMethods[7], "Par.0001.Image"));
    }

    public void teststarMethods8() {
        assertTrue(testMethods[8], doMatch(starMethods, testMethods[8], "Par.0001.Image"));
    }

    public void teststar2Methods0() {
        assertTrue(testMethods[0], doMatch(star2Methods, testMethods[0], "Par.0001.Image"));
    }

    public void teststar2Methods1() {
        assertTrue(testMethods[1], doMatch(star2Methods, testMethods[1], "Par.0001.Image"));
    }

    public void teststar2Methods2() {
        assertTrue(testMethods[2], doMatch(star2Methods, testMethods[2], "Par.0001.Image"));
    }

    public void teststar2Methods3() {
        assertTrue(testMethods[3], doMatch(star2Methods, testMethods[3], "Par.0001.Image"));
    }

    public void teststar2Methods4() {
        assertTrue(testMethods[4], doMatch(star2Methods, testMethods[4], "Par.0001.Image"));
    }

    public void teststar2Methods5() {
        assertTrue(testMethods[5], doMatch(star2Methods, testMethods[5], "Par.0001.Image"));
    }

    public void teststar2Methods6() {
        assertTrue(testMethods[6], doMatch(star2Methods, testMethods[6], "Par.0001.Image"));
    }

    public void teststar2Methods7() {
        assertTrue(testMethods[7], doMatch(star2Methods, testMethods[7], "Par.0001.Image"));
    }

    public void teststar2Methods8() {
        assertTrue(testMethods[8], doMatch(star2Methods, testMethods[8], "Par.0001.Image"));
    }

    public void testlistMethods0() {
        assertTrue(testMethods[0], !doMatch(listMethods, testMethods[0], "Par.0001.Image"));
    }

    public void testlistMethods1() {
        assertTrue(testMethods[1], !doMatch(listMethods, testMethods[1], "Par.0001.Image"));
    }

    public void testlistMethods2() {
        assertTrue(testMethods[2], doMatch(listMethods, testMethods[2], "Par.0001.Image"));
    }

    public void testlistMethods3() {
        assertTrue(testMethods[3], doMatch(listMethods, testMethods[3], "Par.0001.Image"));
    }

    public void testlistMethods4() {
        assertTrue(testMethods[4], doMatch(listMethods, testMethods[4], "Par.0001.Image"));
    }

    public void testlistMethods5() {
        assertTrue(testMethods[5], !doMatch(listMethods, testMethods[5], "Par.0001.Image"));
    }

    public void testlistMethods6() {
        assertTrue(testMethods[6], doMatch(listMethods, testMethods[6], "Par.0001.Image"));
    }

    public void testlistMethods7() {
        assertTrue(testMethods[7], !doMatch(listMethods, testMethods[7], "Par.0001.Image"));
    }

    public void testlistMethods8() {
        assertTrue(testMethods[8], !doMatch(listMethods, testMethods[8], "Par.0001.Image"));
    }

    public void testSet0() {
        testSet("Set0", set0, order0);
    }

    public void testSet1() {
        testSet("Set1", set1, order1);
    }

    public void testSet2() {
        testSet("Set2", set2, order2);
    }

    public void testSet3() {
        testSet("Set3", set3, order3);
    }

    public void testSet4() {
        testSet("Set4", set4, order4);
    }


    private void testSet(String name, SortedMap map, int[] order) {
        ScriptInfo[] infos = (ScriptInfo[]) map.values().toArray(new ScriptInfo[map.size()]);
        int i = 0;
        int j = 0;
        for (; i < order.length && j < infos.length; i++,j++) {
            ScriptInfo si = infos[j];
            assertEquals(name, String.valueOf(order[i])+".esp", si.getScriptName());
        }

        // make sure all values have been tested
        assertEquals(name + ": order list contains more entries than the map", i, order.length);
        assertEquals(name + ": map contains more elements than order list", j, infos.length);
    }

    private ScriptInfo getScriptInfo(String glob, String name, String type, String[] methods) {
        Node tc = new TestNode(glob, name, type, methods);
        return ScriptInfo.getInstance(tc);
    }

    private void addElement(SortedMap m, Object e) {
        m.put(e, e);
    }

    protected void setUp() throws Exception {
	super.setUp();

	nullMethods = getScriptInfo(null, "script.esp", null, nullMethod);
	starMethods = getScriptInfo(null, "script.esp", null, starMethod);
	star2Methods = getScriptInfo(null, "script.esp", null, star2Method);
	listMethods = getScriptInfo(null, "script.esp", null, listMethod);

        // base template
        set0 = new TreeMap();
        addElement(set0, getScriptInfo("Grumbl", "0.esp", null, new String[]{ "post", "options" }));
        addElement(set0, getScriptInfo("Img", "1.esp", null, null));
        addElement(set0, getScriptInfo("Grumbl", "2.esp", null, null));
        addElement(set0, getScriptInfo("*", "3.esp", null, null));

        // ext template
        set1 = new TreeMap(set0);
        addElement(set1, getScriptInfo("Heintje", "10.esp", null, null));
        addElement(set1, getScriptInfo("Img", "11.esp", null, null));
        addElement(set1, getScriptInfo("Grumbl", "12.esp", null, new String[]{ "get", "put", "options" }));
        addElement(set1, getScriptInfo("*", "13.esp", null, null));

        // special stuff
        set2 = new TreeMap(set1);
        addElement(set2, getScriptInfo("Par.*.Img", "20.esp", null, null));
        addElement(set2, getScriptInfo("Par", "21.esp", null, null));
        addElement(set2, getScriptInfo("Par.*.*", "22.esp", null, null));
        addElement(set2, getScriptInfo("*.*.Img", "23.esp", null, null));

        // more special stuff
        set3 = new TreeMap();
        addElement(set3, getScriptInfo("Par.Single.Img", "30.esp", null, null));
        addElement(set3, getScriptInfo("Par.*.Img", "31.esp", null, null));
        addElement(set3, getScriptInfo("Par.Single.*", "32.esp", null, null));
        addElement(set3, getScriptInfo("Par.*.*", "33.esp", null, null));
        addElement(set3, getScriptInfo("Par.Single", "34.esp", null, null));
        addElement(set3, getScriptInfo("*.Single", "35.esp", null, null));
        addElement(set3, getScriptInfo("Par.*", "36.esp", null, null));
        addElement(set3, getScriptInfo("Par", "37.esp", null, null));

        // special - combine set2 and set3
        set4 = new TreeMap(set2);
        set4.putAll(set3);
    }

    protected void tearDown() throws Exception {
	super.tearDown();

	nullMethods = null;
	starMethods = null;
	star2Methods = null;
	listMethods = null;

        set0 = null;
        set1 = null;
        set2 = null;
        set3 = null;
    }

    public static void main(String[] args) throws Exception {
        ScriptInfo_test t = new ScriptInfo_test("ScriptInfo_test");
        t.setUp();

        t.testSet("Set0", t.set0, t.order0);
        t.testSet("Set1", t.set1, t.order1);
        t.testSet("Set2", t.set2, t.order2);
        t.testSet("Set3", t.set3, t.order3);
        t.testSet("Set4", t.set4, t.order4);

        System.out.println("set0:");
        for (Iterator i = t.set0.values().iterator(); i.hasNext();) {
            ScriptInfo scriptInfo = (ScriptInfo) i.next();
            System.out.println(scriptInfo);
        }

        System.out.println("set1:");
        for (Iterator i = t.set1.values().iterator(); i.hasNext();) {
            ScriptInfo scriptInfo = (ScriptInfo) i.next();
            System.out.println(scriptInfo);
        }

        System.out.println("set2:");
        for (Iterator i = t.set2.values().iterator(); i.hasNext();) {
            ScriptInfo scriptInfo = (ScriptInfo) i.next();
            System.out.println(scriptInfo);
        }

        System.out.println("set3:");
        for (Iterator i = t.set3.values().iterator(); i.hasNext();) {
            ScriptInfo scriptInfo = (ScriptInfo) i.next();
            System.out.println(scriptInfo);
        }

        System.out.println("set4:");
        for (Iterator i = t.set4.values().iterator(); i.hasNext();) {
            ScriptInfo scriptInfo = (ScriptInfo) i.next();
            System.out.println(scriptInfo);
        }
    }

    private static class TestDHSRequest implements DeliveryHttpServletRequest {

        private String method;
        private String query;

        private TestDHSRequest t(String method, String query) {
            this.method = method;
            this.query = query;
            return this;
        }

        public HttpServletRequest getRequest() {
            return null;
        }

        public CmsService getCmsService() {
            return null;
        }

        public String getAuthType() {
            return null;
        }

        public Cookie[] getCookies() {
            return new Cookie[0];
        }

        public long getDateHeader(String s) {
            return 0;
        }

        public String getHeader(String s) {
            return null;
        }

        public Enumeration getHeaders(String s) {
            return null;
        }

        public Enumeration getHeaderNames() {
            return null;
        }

        public int getIntHeader(String s) {
            return 0;
        }

        public String getMethod() {
            return method;
        }

        public String getPathInfo() {
            return null;
        }

        public String getPathTranslated() {
            return null;
        }

        public String getContextPath() {
            return null;
        }

        public String getQueryString() {
            return null;
        }

        public String getRemoteUser() {
            return null;
        }

        public boolean isUserInRole(String s) {
            return false;
        }

        public Principal getUserPrincipal() {
            return null;
        }

        public String getRequestedSessionId() {
            return null;
        }

        public String getRequestURI() {
            return null;
        }

        public StringBuffer getRequestURL() {
            return null;
        }

        public String getServletPath() {
            return null;
        }

        public HttpSession getSession(boolean b) {
            return null;
        }

        public HttpSession getSession() {
            return null;
        }

        public boolean isRequestedSessionIdValid() {
            return false;
        }

        public boolean isRequestedSessionIdFromCookie() {
            return false;
        }

        public boolean isRequestedSessionIdFromURL() {
            return false;
        }

        public boolean isRequestedSessionIdFromUrl() {
            return false;
        }

        public Object getAttribute(String s) {
            return null;
        }

        public Enumeration getAttributeNames() {
            return null;
        }

        public String getCharacterEncoding() {
            return null;
        }

        public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
        }

        public int getContentLength() {
            return 0;
        }

        public String getContentType() {
            return null;
        }

        public ServletInputStream getInputStream() throws IOException {
            return null;
        }

        public String getParameter(String s) {
            return null;
        }

        public Enumeration getParameterNames() {
            return null;
        }

        public String[] getParameterValues(String s) {
            return new String[0];
        }

        public String getParameterType(String name) {
            return null;
        }

        public String[] getParameterTypes(String name) {
            return new String[0];
        }

        public Map getParameterMap() {
            return null;
        }

        public String getProtocol() {
            return null;
        }

        public String getScheme() {
            return null;
        }

        public String getServerName() {
            return null;
        }

        public int getServerPort() {
            return 0;
        }

        public BufferedReader getReader() throws IOException {
            return null;
        }

        public String getRemoteAddr() {
            return null;
        }

        public String getRemoteHost() {
            return null;
        }

        public void setAttribute(String s, Object o) {
        }

        public void removeAttribute(String s) {
        }

        public Locale getLocale() {
            return null;
        }

        public Enumeration getLocales() {
            return null;
        }

        public boolean isSecure() {
            return false;
        }

        public RequestDispatcher getRequestDispatcher(String s) {
            return null;
        }

        public String getRealPath(String s) {
            return null;
        }

        public String getUserId() {
            return null;
        }

        public String getURLPath() {
            return null;
        }

        public MappedURL getMappedURL() {
            return null;
        }

        public String getHandle() {
            return null;
        }

        public String getExtension() {
            return null;
        }

        public String getSuffix() {
            return null;
        }

        public boolean isIncluded() {
            return false;
        }

        public String getRealRequestURI() {
            return null;
        }

        public String getRealContextPath() {
            return null;
        }

        public String getRealServletPath() {
            return null;
        }

        public String getRealPathInfo() {
            return null;
        }

        public String getRealQueryString() {
            return null;
        }

        public String getCombinedQuery() {
            return query;
        }

        public String getSelectorString() {
            return query;
        }

        public String[] getQuery() {
            return new String[0];
        }

        public String[] getSelectors() {
            return new String[0];
        }

        public boolean hasParameter(String name) {
            return false;
        }

        public String getParameter(String name, String encoding)
            throws UnsupportedEncodingException {
            return null;
        }

        public String[] getParameterValues(String name, String encoding)
            throws UnsupportedEncodingException {
            return new String[0];
        }

        public byte[] getParameterBytes(String name) {
            return new byte[0];
        }

        public byte[][] getParameterValuesBytes(String name) {
            return new byte[0][];
        }

        public Enumeration getFileParameterNames() {
            return null;
        }

        public boolean isFileParameter(String name) {
            return false;
        }

        public File getFileParameter(String name) {
            return null;
        }

        public File[] getFileParameterValues(String name) {
            return new File[0];
        }

        public String getRealMethod() {
            return method;
        }

        public boolean forceDebug() {
            return false;
        }

        public void registerObject(Finalizer object) {
        }

        public void addTempFile(File file) {
        }

        public String externalizeHref(String href) {
            return null;
        }

        public String externalizeHandle(String handle) {
            return null;
        }

        public Session getRepositorySession() {
            return null;
        }

        public Subject getUser() {
            return null;
        }

        public Node getPageNode() {
            return null;
        }

        public Item getItem() {
            return null;
        }
    }

    private static class TestValue implements Value {

        private final String value;
        private TestValue(String value) {
            this.value = value;
        }

        public String getString() {
            return value;
        }

        public InputStream getStream() {
            return null;
        }

        public long getLong() {
            return 0;
        }

        public double getDouble() {
            return 0;
        }

        public Calendar getDate() {
            return null;
        }

        public boolean getBoolean() {
            return false;
        }

        public int getType() {
            return PropertyType.STRING;
        }
    }

    private static class TestProperty implements Property {

        private final TestValue value;
        private final TestValue[] values;

        private TestProperty(String value) {
            this.value = value == null ? null : new TestValue(value);
            this.values = null;
        }

        private TestProperty(String[] value) {
            if (value == null) {
                values = new TestValue[0];
            } else {
                values = new TestValue[value.length];
                for (int i=0; i < values.length; i++) {
                    values[i] = new TestValue(value[i]);
                }
            }
            this.value = null;
        }

        public String getString() {
            return value == null ? null : value.getString();
        }

        public void setValue(Value arg0) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        }

        public void setValue(Value[] arg0) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        }

        public void setValue(String arg0) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        }

        public void setValue(String[] arg0) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        }

        public void setValue(InputStream arg0) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        }

        public void setValue(long arg0) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        }

        public void setValue(double arg0) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        }

        public void setValue(Calendar arg0) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        }

        public void setValue(boolean arg0) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        }

        public void setValue(Node arg0) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        }

        public Value getValue() throws ValueFormatException, RepositoryException {
            return value;
        }

        public Value[] getValues() throws ValueFormatException, RepositoryException {
            return values;
        }

        public InputStream getStream() throws ValueFormatException, RepositoryException {
            return null;
        }

        public long getLong() throws ValueFormatException, RepositoryException {
            return 0;
        }

        public double getDouble() throws ValueFormatException, RepositoryException {
            return 0;
        }

        public Calendar getDate() throws ValueFormatException, RepositoryException {
            return null;
        }

        public boolean getBoolean() throws ValueFormatException, RepositoryException {
            return false;
        }

        public Node getNode() throws ValueFormatException, RepositoryException {
            return null;
        }

        public long getLength() throws ValueFormatException, RepositoryException {
            return 0;
        }

        public long[] getLengths() throws ValueFormatException, RepositoryException {
            return null;
        }

        public PropertyDefinition getDefinition() throws RepositoryException {
            return new PropertyDefinition() {
                public int getRequiredType() {
                    return PropertyType.STRING;
                }
                public String[] getValueConstraints() {
                    return null;
                }
                public Value[] getDefaultValues() {
                    return null;
                }
                public boolean isMultiple() {
                    return values != null;
                }
                public NodeType getDeclaringNodeType() {
                    return null;
                }
                public String getName() {
                    return null;
                }
                public boolean isAutoCreated() {
                    return false;
                }
                public boolean isMandatory() {
                    return false;
                }
                public int getOnParentVersion() {
                    return 0;
                }
                public boolean isProtected() {
                    return false;
                }
            };
        }

        public int getType() throws RepositoryException {
            return 0;
        }

        public String getPath() throws RepositoryException {
            return null;
        }

        public String getName() throws RepositoryException {
            return null;
        }

        public Item getAncestor(int arg0) throws ItemNotFoundException, AccessDeniedException, RepositoryException {
            return null;
        }

        public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
            return null;
        }

        public int getDepth() throws RepositoryException {
            return 0;
        }

        public Session getSession() throws RepositoryException {
            return null;
        }

        public boolean isNode() {
            return false;
        }

        public boolean isNew() {
            return false;
        }

        public boolean isModified() {
            return false;
        }

        public boolean isSame(Item arg0) throws RepositoryException {
            return false;
        }

        public void accept(ItemVisitor arg0) throws RepositoryException {
        }

        public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException, ReferentialIntegrityException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
        }

        public void refresh(boolean arg0) throws InvalidItemStateException, RepositoryException {
        }

        public void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        }
    }

    private static class TestNode implements Node {

        private final HashMap atoms = new HashMap();

        private TestNode(String glob, String name, String type, String[] methods) {
            atoms.put("Glob", new TestProperty(glob));
            atoms.put("Type", new TestProperty(type));
            atoms.put("Name", new TestProperty(name));
            atoms.put("Methods", new TestProperty(methods));
        }

        public Property getProperty(String label) {
            return (Property) atoms.get(label);
        }

        public boolean hasProperty(String label) throws RepositoryException {
            return atoms.containsKey(label);
        }

        public Node addNode(String arg0) throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException, RepositoryException {
            return null;
        }

        public Node addNode(String arg0, String arg1) throws ItemExistsException, PathNotFoundException, NoSuchNodeTypeException, LockException, VersionException, ConstraintViolationException, RepositoryException {
            return null;
        }

        public void orderBefore(String arg0, String arg1) throws UnsupportedRepositoryOperationException, VersionException, ConstraintViolationException, ItemNotFoundException, LockException, RepositoryException {
        }

        public Property setProperty(String arg0, Value arg1) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
            return null;
        }

        public Property setProperty(String arg0, Value arg1, int arg2) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
            return null;
        }

        public Property setProperty(String arg0, Value[] arg1) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
            return null;
        }

        public Property setProperty(String arg0, Value[] arg1, int arg2) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
            return null;
        }

        public Property setProperty(String arg0, String[] arg1) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
            return null;
        }

        public Property setProperty(String arg0, String[] arg1, int arg2) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
            return null;
        }

        public Property setProperty(String arg0, String arg1) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
            return null;
        }

        public Property setProperty(String arg0, String arg1, int arg2) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
            return null;
        }

        public Property setProperty(String arg0, InputStream arg1) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
            return null;
        }

        public Property setProperty(String arg0, boolean arg1) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
            return null;
        }

        public Property setProperty(String arg0, double arg1) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
            return null;
        }

        public Property setProperty(String arg0, long arg1) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
            return null;
        }

        public Property setProperty(String arg0, Calendar arg1) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
            return null;
        }

        public Property setProperty(String arg0, Node arg1) throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
            return null;
        }

        public Node getNode(String arg0) throws PathNotFoundException, RepositoryException {
            return null;
        }

        public NodeIterator getNodes() throws RepositoryException {
            return null;
        }

        public NodeIterator getNodes(String arg0) throws RepositoryException {
            return null;
        }

        public PropertyIterator getProperties() throws RepositoryException {
            return null;
        }

        public PropertyIterator getProperties(String arg0) throws RepositoryException {
            return null;
        }

        public Item getPrimaryItem() throws ItemNotFoundException, RepositoryException {
            return null;
        }

        public String getUUID() throws UnsupportedRepositoryOperationException, RepositoryException {
            return null;
        }

        public int getIndex() throws RepositoryException {
            return 0;
        }

        public PropertyIterator getReferences() throws RepositoryException {
            return null;
        }

        public boolean hasNode(String arg0) throws RepositoryException {
            return false;
        }

        public boolean hasNodes() throws RepositoryException {
            return false;
        }

        public boolean hasProperties() throws RepositoryException {
            return !atoms.isEmpty();
        }

        public NodeType getPrimaryNodeType() throws RepositoryException {
            return null;
        }

        public NodeType[] getMixinNodeTypes() throws RepositoryException {
            return null;
        }

        public boolean isNodeType(String arg0) throws RepositoryException {
            return false;
        }

        public void addMixin(String arg0) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        }

        public void removeMixin(String arg0) throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        }

        public boolean canAddMixin(String arg0) throws NoSuchNodeTypeException, RepositoryException {
            return false;
        }

        public NodeDefinition getDefinition() throws RepositoryException {
            return null;
        }

        public Version checkin() throws VersionException, UnsupportedRepositoryOperationException, InvalidItemStateException, LockException, RepositoryException {
            return null;
        }

        public void checkout() throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        }

        public void doneMerge(Version arg0) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        }

        public void cancelMerge(Version arg0) throws VersionException, InvalidItemStateException, UnsupportedRepositoryOperationException, RepositoryException {
        }

        public void update(String arg0) throws NoSuchWorkspaceException, AccessDeniedException, LockException, InvalidItemStateException, RepositoryException {
        }

        public NodeIterator merge(String arg0, boolean arg1) throws NoSuchWorkspaceException, AccessDeniedException, MergeException, LockException, InvalidItemStateException, RepositoryException {
            return null;
        }

        public String getCorrespondingNodePath(String arg0) throws ItemNotFoundException, NoSuchWorkspaceException, AccessDeniedException, RepositoryException {
            return null;
        }

        public boolean isCheckedOut() throws RepositoryException {
            return false;
        }

        public void restore(String arg0, boolean arg1) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        }

        public void restore(Version arg0, boolean arg1) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, RepositoryException {
        }

        public void restore(Version arg0, String arg1, boolean arg2) throws PathNotFoundException, ItemExistsException, VersionException, ConstraintViolationException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        }

        public void restoreByLabel(String arg0, boolean arg1) throws VersionException, ItemExistsException, UnsupportedRepositoryOperationException, LockException, InvalidItemStateException, RepositoryException {
        }

        public VersionHistory getVersionHistory() throws UnsupportedRepositoryOperationException, RepositoryException {
            return null;
        }

        public Version getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException {
            return null;
        }

        public Lock lock(boolean arg0, boolean arg1) throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
            return null;
        }

        public Lock getLock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, RepositoryException {
            return null;
        }

        public void unlock() throws UnsupportedRepositoryOperationException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        }

        public boolean holdsLock() throws RepositoryException {
            return false;
        }

        public boolean isLocked() throws RepositoryException {
            return false;
        }

        public String getPath() throws RepositoryException {
            return null;
        }

        public String getName() throws RepositoryException {
            return null;
        }

        public Item getAncestor(int arg0) throws ItemNotFoundException, AccessDeniedException, RepositoryException {
            return null;
        }

        public Node getParent() throws ItemNotFoundException, AccessDeniedException, RepositoryException {
            return null;
        }

        public int getDepth() throws RepositoryException {
            return 0;
        }

        public Session getSession() throws RepositoryException {
            return null;
        }

        public boolean isNode() {
            return false;
        }

        public boolean isNew() {
            return false;
        }

        public boolean isModified() {
            return false;
        }

        public boolean isSame(Item arg0) throws RepositoryException {
            return false;
        }

        public void accept(ItemVisitor arg0) throws RepositoryException {
        }

        public void save() throws AccessDeniedException, ItemExistsException, ConstraintViolationException, InvalidItemStateException, ReferentialIntegrityException, VersionException, LockException, NoSuchNodeTypeException, RepositoryException {
        }

        public void refresh(boolean arg0) throws InvalidItemStateException, RepositoryException {
        }

        public void remove() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        }
    }
}

