/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.scripting.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.sling.component.ComponentRequest;
import org.apache.sling.scripting.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * The <code>ScriptInfo</code> contains all the configured properties of one
 * single <code>&lt;script /&gt;</code> element of a
 * <code>&lt;template /&gt;</code>
 * 
 * @ocm.mapped jcrNodeType="sling:Script" implement=""
 */
public class ScriptImpl implements Comparable, Script {

    /** default log */
    private static final Logger log = LoggerFactory.getLogger(ScriptImpl.class);

    /**
     * The scriptName of the system property optionally containing a list of
     * methods to restrict the default behaviour. If this property is not set,
     * all script infos containing no <em>Methods</em> property default to
     * only accept requests for one of the listed method names. If the property
     * is not set, script infos containing no <em>Methods</em> property
     * default to accept any HTTP request method.
     * <p>
     * This value of the property is treated like the standard <em>Method</em>
     * property values of script infos : It is assumed to contain a comma
     * separated list of method names. Whitespace around method names is
     * ignored. If any of the methods is the asterisk (<em>*</em>) the list
     * defaults to accept all methods. Note that if the property is defined with
     * an empty value, this in fact forces all script infos to have mandatory
     * <em>Method</em> property settings, elso no requests will ever be
     * handled.
     * <p> <<strong><em>NOTE: THIS IS UNDOCUMENTED INTERMEDIATE BEHAVIOUR AND MAY
     * CHANGE IN FUTURE RELEASES WITHOUT ANY ADVANCE NOTICE. USE OF THIS FEATURE
     * IS NEITHER SUPPORTED NOR GENERALLY ENDORSED BY DAY SOFTWARE,
     * INC.</em></strong>
     */
    private static final String RESTRICTED_METHODS_PROPERTY = "com.day.sling.script.restricted_methods";

    /**
     * Special globbing pattern to match all queries. This is the default
     * globbing pattern if glob is missing or empty upon <code>ScriptInfo</code>
     * construction.
     */
    private static final String MATCH_ALL = "*";

    /** The default method set if not configured */
    private static final SortedSet DEFAULT_METHODS;

    /**
     * Name of the script to call to handle the request. This scriptName is the
     * scriptName of a 'page' which is got from the contentbus.
     * 
     * @ocm.field jcrName="sling:name"
     */
    private String scriptName;

    /**
     * Type of the script named by m_name. If this information is missing the
     * type of the script is derived from the extension of the script
     * scriptName. This type identifier should not be treated case-sensitive !
     * 
     * @ocm.field jcrName="sling:type"
     */
    private String type;

    /**
     * Globbing pattern for the page query. This pattern is matched against the
     * query part of a page ruquest to check whether this
     * <code>ScriptInfo</code> may be used to handle the request. *
     * 
     * @ocm.field jcrName="sling:glob"
     */
    private String selectors;

    /**
     * List of globbing parts from the globbing pattern. Upon creation of the
     * <code>ScriptInfo</code> the globbing pattern (if not MATCH_ALL) is
     * split at dots and the parts stored in the String array. If the globbing
     * pattern equals {@link #MATCH_ALL} <code>globParts</code> is set to
     * null.
     */
    private String[] globParts;

    /**
     * The names of methods, for which this <code>ScriptInfo</code> may handle
     * the request. If this set is <code>null</code> all methods may be
     * handled if the set is empty, no methods are accepted.
     * <p>
     * This set is sorted such that the <code>toString</code> method returns
     * an ordered list of string values such that the {@link #compareTo} method
     * returns a deterministic ordering between two different methods fields.
     * 
     * @ocm.collection jcrName="sling:methods"
     *                 elementClassName="java.lang.String"
     *                 collectionConverter="org.apache.jackrabbit.ocm.manager.collectionconverter.impl.MultiValueCollectionConverterImpl"
     */
    private SortedSet methods;

    /** The cached result of the toString() call. */
    private String stringValue;

    /** The number of dot-separated parts, used in {@link #compareTo} */
    private int width;

    /** The number of trailing "*"-only parts */
    private int trailingStars;

    /** The number of "*"-only parts */
    private int numStars;

    /**
     * @ocm.collection jcrName="sling:extensions"
     *                 elementClassName="java.lang.String"
     *                 collectionConverter="org.apache.jackrabbit.ocm.manager.collectionconverter.impl.MultiValueCollectionConverterImpl"
     */
    private String[] extensions;

    /**
     * @ocm.collection jcrName="sling:parameters"
     *                 elementClassName="java.lang.String"
     *                 collectionConverter="org.apache.jackrabbit.ocm.manager.collectionconverter.impl.MultiValueCollectionConverterImpl"
     */
    private Parameter[] parameters;

    static {
        // Build the list of default methods from system property
        String methods = System.getProperty(RESTRICTED_METHODS_PROPERTY);
        if (methods != null) {
            StringTokenizer tokener = new StringTokenizer(methods, ",");
            SortedSet tmp = new TreeSet();
            while (tokener.hasMoreTokens()) {
                String token = tokener.nextToken().trim();
                if ("*".equals(token)) {
                    tmp = null;
                    break;
                } else if (token.length() > 0) {
                    tmp.add(token.toUpperCase());
                }
            }
            DEFAULT_METHODS = tmp;
            log.info("ScriptImpl: Default method set: {0}", DEFAULT_METHODS);
        } else {
            log.debug("ScriptImpl: Accepting all methods by default");
            DEFAULT_METHODS = null;
        }
    }

    /**
     * Creates an uninitialized instance of the default {@link ScriptInfo}
     * implementation. This should only be called by the
     * {@link ScriptInfo#getInstance} method, which is why this is protected.
     * FIXME: adapt JavaDoc, this must be public for the mapper to work !
     */
    public ScriptImpl() {
    }

    /**
     * Get the script scriptName
     * 
     * @return the script scriptName
     */
    public String getScriptName() {
        return scriptName;
    }

    /**
     * Get the type
     * 
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Matches the method against the supported method names (if any) and the
     * query against the globbing pattern returning true if a match is found. A
     * match is found, if one of the following holds true :
     * <ol>
     * <li>the globbing patterns is equal to {@link #MATCH_ALL}
     * <li>the query and globbing pattern contain the same number of parts and
     * each part is of the query matches the respective part of the globbing
     * pattern.
     * </ol>
     * <p>
     * If the methods list is <code>null</code> this method matches any method
     * scriptName given else the method must be in the accepted set.
     * 
     * @param request The {@link DeliveryHttpServletRequest} containing the
     *            information needed to match this <code>ScriptInfo</code>.
     * @return true if the query matches against the globbing pattern
     */
    public boolean matches(ComponentRequest request) {
        return matchMethod(request) && matchSelectors(request)
            && matchExtensions(request) && matchParameters(request);
    }

    // ---------- Comparable interface -----------------------------------------

    /**
     * Compares this object with the specified object for order. Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     * <p>
     * The following cases are treated as follows :
     * <ol>
     * <li>If the other object equals this, 0 is returned
     * <li>If the other object is <code>null</code> a
     * <code>ClassCastException</code> is thrown
     * <li>If the other object's class is not an extension of the
     * {@link ScriptInfo} class, a <code>ClassCastException</code> is thrown,
     * too
     * <li>If the other object's class is not the same class as this, the
     * result of comparing this object's string representation to the other
     * object's string representation is returned.
     * <li>If the other object's class is also the <code>ScriptImpl</code>
     * class (and not an extension of it !), the comparison follows this pattern
     * (see below for the definition of words) :
     * <ol>
     * <li>If this width is not equal to the other object's width, return a
     * negative integer if this width is higher than the other's, else return a
     * positive integer.
     * <li>Else, if this trailingStars is not equal to the other object's
     * trailingStars, return a negative integer if this trailingStars is less
     * than the other's, else return a positive integer.
     * <li>Else, if this numStars is not equal to the other object's numStars,
     * return a negative integer if this numStars is less than the other's, else
     * return a positive integer.
     * <li>Else, if the patterns are not equal, return the result of comparing
     * this pattern to the other's pattern. This results in an ordering based on
     * lexical ordering of globbing patterns
     * <li>Else, if only this or the other have a method set (but not both),
     * return a negative integer, if this has a method set, else return a
     * positive integer.
     * <li>Else, if the two method sets have an intersection. If so, log a
     * warning message.
     * <li>And finally return the result of comparing the string representation
     * of this method set to the other object's method set. This results in an
     * ordering based on lexical ordering of the method set's string
     * representation.
     * </ol>
     * </ol>
     * <p>
     * Some definitions used in above algorithm description :
     * <dl>
     * <dt><em>width</em>
     * <dd>The width of a <code>ScriptImpl</code> object is defined as the
     * number of dot-separated elements contained in the globbing pattern. E.g.:
     * The pattern <em>Par.*.Img</em> has a width of three. The special
     * match-all pattern (single <em>*</em>) has a width of 0.
     * <dt><em>trailingStars</em>
     * <dd>The number of dot-separated elements at the end of the globbing
     * pattern consisting of only stars. E.g.: The pattern <em>Par.*.Img</em>
     * has no trailing stars, while the pattern <em>Par.*.*</em> has two
     * trailing stars. The special match-all pattern (single <em>*</em>) has
     * 1 trailing stars.
     * <dt><em>numStars</em>
     * <dd>The number of dot-separated elements in the globbing pattern
     * consisting of only stars. E.g.: The pattern <em>Par.*.Img</em> has 1
     * star, while the pattern <em>Par.*.*</em> has two stars. The special
     * match-all pattern (single <em>*</em>) has 1 star.
     * </dl>
     * <p>
     * This implementation is consistent with equals as it always returns 0 if
     * the other object equals this.
     * 
     * @param o the Object to be compared.
     * @return a negative integer, zero, or a positive integer as this object is
     *         less than, equal to, or greater than the specified object.
     * @throws ClassCastException if the specified object's type prevents it
     *             from being compared to this Object.
     */
    public int compareTo(Object o) {

        // check for equality and null
        if (equals(o)) {
            return 0;
        } else if (o == null) {
            throw new ClassCastException("Cannot compare to null");
        }

        // check for correct class of o
        if (!(o instanceof ScriptImpl)) {
            throw new ClassCastException(o
                + " is not an implementation of ScriptImpl");
        }

        // if o's class is not a ScriptImpl (not even an extension !)
        if (getClass() != o.getClass()) {
            log.debug("compareTo: Comparing String representation of two"
                + "different implementations");
            return toString().compareTo(o.toString());
        }

        // cast to the correct class for further comparisons
        ScriptImpl other = (ScriptImpl) o;

        // check for width ordering
        if (width > other.width) {
            // this < o
            return -1;
        } else if (width < other.width) {
            // this > o
            return 1;
        }
        // invariant : width are equal

        // check for trailing stars
        if (trailingStars < other.trailingStars) {
            // this < o
            return -1;
        } else if (trailingStars > other.trailingStars) {
            // this > o
            return 1;
        }
        // invariant: width and trailingStars are equal

        // check for total number of stars
        if (numStars < other.numStars) {
            // this < o
            return -1;
        } else if (numStars > other.numStars) {
            // this > o
            return 1;
        }
        // invariant : width, trailingStars, and numStars are equal

        // compare glob patterns for lexical ordering
        int globComp = selectors.compareTo(other.selectors);
        if (globComp != 0) {
            // string order of glob pattern
            return globComp;
        }
        // invariant: width, trailingStars, numStars, and glob are equal

        // compare methods sets
        if (other.methods == null) {
            // this < o
            // this.methods != null - because of !equals(other)
            return -1;
        } else if (methods == null) {
            // this > o
            // other.methods != null - because of !equals(other)
            return 1;
        }
        // invariant: width, trailingStars, numStars, and glob are equal
        // and methods are not empty

        // check method intersection - only to warn, no ordering influence
        Set s = new HashSet(methods);
        if (s.retainAll(other.methods)) {
            // leave all in s which are not in other.methods
            // true if some were removed, thus intersection non-empty
            // we have a collision
            log.warn("compareTo: {0} and {1} have equal "
                + "globs and share some methods ", this, other);
        }

        // return alphabetical order of toString
        return methods.toString().compareTo(other.methods.toString());
    }

    // ---------- Object Overwrites -------------------------------------------

    /**
     * Returns the hash code of this object. The hashcode is based on the
     * hashcode of the glob pattern and the hashcode of the method set.
     */
    public int hashCode() {
        // methods might be null if all methods apply
        int methodsHash = (methods == null) ? 0 : methods.hashCode();
        return 37 * selectors.hashCode() + methodsHash;
    }

    /**
     * Returns true if the object can be considered equal to this object. Two
     * objects are equal if they are the same object (i.e.
     * <code>obj == this</code>) or if both are <code>ScriptImpl</code>
     * objects and the glob pattern and the method sets of both objects are
     * equal.
     * 
     * @param obj The <code>Object</code> to compare this object to.
     * @return <code>true</code> if this object is equal to the other object.
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (obj instanceof ScriptImpl) {
            ScriptImpl sii = (ScriptImpl) obj;

            // methods may be null if all methods apply
            boolean methodsEqual = (methods == null)
                    ? sii.methods == null
                    : methods.equals(sii.methods);

            return methodsEqual && selectors.equals(sii.selectors);
        } else {
            return false;
        }
    }

    /**
     * Returns a string representation of the ScriptInfo object
     * 
     * @return a string representation of the ScriptInfo object
     */
    public String toString() {
        if (stringValue == null) {
            StringBuffer buf = new StringBuffer("ScriptInfo: glob=");
            buf.append(selectors);
            buf.append(", methods=");
            buf.append(methods);
            buf.append(", scriptName=");
            buf.append(scriptName);
            buf.append(", type=");
            buf.append(type);

            buf.append(", width=").append(width);
            buf.append(", trailingStars=").append(trailingStars);
            buf.append(", numStars=").append(numStars);

            stringValue = buf.toString();
        }

        return stringValue;
    }

    // ---------- Object Mapping -----------------------------------------------

    /**
     * @param scriptName the scriptName to set
     */
    public void setScriptName(String name) {
        this.scriptName = name;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the methods
     */
    public List getMethods() {
        if (DEFAULT_METHODS.equals(methods)) {
            return Collections.EMPTY_LIST;
        }

        if (methods == null) {
            return Arrays.asList(new Object[] { MATCH_ALL });
        }

        return new ArrayList(methods);
    }

    /**
     * @param methods the methods to set
     */
    public void setMethods(List methods) {
        this.methods = createMethodSet(methods);
    }

    public List getExtensions() {
        return (extensions == null)
                ? Collections.EMPTY_LIST
                : Arrays.asList(extensions);
    }

    public void setExtensions(List extensions) {
        if (extensions == null || extensions.size() == 0) {
            this.extensions = null;
        } else {
            this.extensions = new String[extensions.size()];
            int i = 0;
            for (Iterator ei = extensions.iterator(); ei.hasNext(); i++) {
                this.extensions[i] = String.valueOf(ei.next()).trim().toLowerCase();
            }
        }
    }

    /**
     * @return the selectors
     */
    public String getSelectors() {
        return selectors;
    }

    /**
     * @param selectors the selectors to set
     */
    public void setSelectors(String selectors) {
        // default selectors to match everything
        if (selectors == null || selectors.length() == 0) {
            selectors = MATCH_ALL;
        }

        this.selectors = selectors;

        // if this is a match all glob, set defaults and return
        if (MATCH_ALL.equals(selectors)) {
            globParts = null;
            width = 0;
            trailingStars = 1;
            numStars = 1;
            return;
        }

        // otherwise split and configure
        width = 0;
        trailingStars = 0;
        numStars = 0;
        StringTokenizer tokener = new StringTokenizer(selectors, ".");
        List parts = new ArrayList();
        while (tokener.hasMoreTokens()) {
            String part = tokener.nextToken().trim();
            parts.add(part);
            if (MATCH_ALL.equals(part)) {
                numStars++;
                trailingStars++;
            } else {
                trailingStars = 0;
            }
        }
        globParts = (String[]) parts.toArray(new String[parts.size()]);
    }

    /**
     * @return the parameters
     */
    public List getParameters() {
        List result = new ArrayList();
        for (int i = 0; parameters != null && i < parameters.length; i++) {
            result.add(parameters[i].toString());
        }
        return result;
    }

    /**
     * @param parameters the parameters to set
     */
    public void setParameters(List parameters) {
        // quick handling of 99% case
        if (parameters == null || parameters.size() == 0) {
            this.parameters = null;
            return;
        }

        List parList = new ArrayList(parameters.size());
        for (Iterator pi = parameters.iterator(); pi.hasNext();) {
            String par = String.valueOf(pi.next());
            parList.add(new Parameter(par));
        }

        this.parameters = parList.isEmpty()
                ? null
                : (Parameter[]) parList.toArray(new Parameter[parList.size()]);
    }

    // ---------- internal -----------------------------------------------------

    /**
     * Creates the set of accepted method names from the string. The string
     * contains a comma separated list of method names. Case of the names does
     * not matter and whitespace are and method scriptName is ignored
     * (whitespace is defined in the JavaDoc to the <code>String.trim()</code>
     * method).
     * <p>
     * If the list contains the special method <code>*</code> denoting any
     * method, null is returned to indicate this fact. If <code>*</code> is
     * not the only entry in the list, the other entries are ignored. Empty
     * method names in the list are ignored.
     * <p>
     * If the list is <code>null</code>, the methods accepted are the
     * <em>GET</em> and the <em>POST</em> method only. If the list is the
     * empty string, an empty set is returned indicating, that no method names
     * will ever match.
     * 
     * @param methodNames The list of method names as specified.
     * @return The method scriptName set, which may be <code>null</code> if
     *         all method names should be matched.
     */
    private static SortedSet createMethodSet(List methodNames) {

        // short cut for special lists <code>null</code> and "*"
        if (methodNames == null || methodNames.size() == 0) {
            return DEFAULT_METHODS;
        }

        SortedSet methods = new TreeSet(); // new HashSet();
        for (Iterator mi = methodNames.iterator(); mi.hasNext();) {
            // get the method scriptName and trim whitespace.
            String token = String.valueOf(mi.next()).trim();

            // if "*" is contained in the list, drop out
            if ("*".equals(token)) {
                return null;
            } else if (token.length() > 0) {
                methods.add(token.toUpperCase());
            }
        }

        return methods;
    }

    // TODO
    private boolean matchExtensions(ComponentRequest request) {
        // match if there is no restriction
        if (extensions == null) {
            return true;
        }

        // check the extension itself, fail if null
        String ext = request.getExtension();
        if (ext == null) {
            return false;
        }

        // otherwise walk the list to compare the request extension
        ext = ext.toLowerCase();
        for (int i = 0; i < extensions.length; i++) {
            if (extensions[i].equals(ext)) {
                return true;
            }
        }

        // not found in the list, fail
        return false;
    }

    /**
     * Matches the selectors against the globbing pattern returning true if a
     * match is found. A match is found, if one of the following holds true :
     * <ol>
     * <li>the globbing patterns is equal to {@link #MATCH_ALL}
     * <li>the selector string and globbing pattern contain the same number of
     * parts and each part is of the selector string matches the respective part
     * of the globbing pattern.
     * </ol>
     * 
     * @param request TODO
     * @return true if the selector string matches against the globbing pattern
     */
    private boolean matchSelectors(ComponentRequest request) {
        // if match all, we match regardless of query !
        if (globParts == null) {
            return true;
        }

        // if query is null, we will not be able to match
        String[] selectors = request.getSelectors();
        if (selectors == null || selectors.length == 0) {
            return false;
        }

        // Match at least the number of parts in the globbing !
        if (selectors.length < globParts.length) return false;

        // match the glob parts against the selectors, fail if any does not
        // match
        for (int i = 0; i < globParts.length; i++) {
            // TODO: Decided whether to use globbings or regular expressions !
            // simple workaround:
            if (!globParts[i].equals("*") && !globParts[i].equals(selectors[i])) {
                return false;
            }
        }

        // matched at least the glob parts (prefix match)
        return true;
    }

    /**
     * Matches the method scriptName against the accepted method names. This
     * method returns <code>true</code> for any parameter if the set of method
     * names is <code>null</code>.
     * <p>
     * The scriptName match is handled case insensitive.
     * 
     * @param method The scriptName of the method to match in the set of
     *            accepted method names.
     * @return <code>true</code> if either the set of accepted names is
     *         <code>null</code> or if the method is contained in the set.
     */
    private boolean matchMethod(ComponentRequest request) {
        // no methods to compare
        if (methods == null) {
            return true;
        }

        // check whether the request has a method (null, would actually be
        // unexpected)
        String method = request.getMethod();
        if (method == null) {
            return false;
        }

        // check whether the set contains the method
        return methods.contains(method.toUpperCase());
    }

    // TODO
    private boolean matchParameters(ComponentRequest request) {
        if (parameters == null) {
            return true;
        }

        OUTER_LOOP: for (int i = 0; i < parameters.length; i++) {
            Parameter par = parameters[i];
            String[] reqPars = request.getParameterValues(par.getName());

            if (!par.matches(reqPars)) {
                // if still here, no value matched, ergo fail
                return false;
            }
        }

        // if looped through all parameters and all matched, succeed
        return true;
    }

    // ---------- internal class -----------------------------------------------

    private static class Parameter {
        private String name;

        private boolean negate;

        private String exactString;

        private Pattern regex;

        Parameter(String config) {
            config = config.trim();
            if (config.startsWith("!")) {
                negate = true;
                config = config.substring(1);
            } else {
                negate = false;
            }

            int eq = config.indexOf('=');
            if (eq < 0) {
                name = config;
                exactString = null;
                regex = null;

            } else {
                name = config.substring(0, eq).trim();
                config = config.substring(eq + 1).trim();
                if (config.startsWith("/")) {
                    config = config.substring(1);
                    if (config.endsWith("/")) {
                        config = config.substring(0, config.length() - 1);
                    }
                    exactString = null;
                    regex = Pattern.compile(config);
                } else {
                    exactString = config;
                    regex = null;
                }
            }
        }

        String getName() {
            return name;
        }

        boolean matches(String[] values) {
            // presence check only
            if (exactString == null && regex == null) {
                return negate ^ (values == null || values.length == 0);
            }

            // value check without values fails
            // fail completely if parameter is missing
            if (values == null || values.length == 0) {
                return false;
            }

            // compare parameter values, succeed on first match
            if (exactString != null) {
                for (int i = 0; i < values.length; i++) {
                    if (negate ^ exactString.equals(values[i])) {
                        return true;
                    }
                }

                // no match found, fail
                return false;
            }

            // regex is not be null here, succeed on first match
            for (int i = 0; i < values.length; i++) {
                if (negate ^ regex.matcher(values[i]).matches()) {
                    return true;
                }
            }

            // no match found, fail
            return false;
        }

        public String toString() {
            StringBuffer buf = new StringBuffer();
            if (negate) buf.append('!');
            buf.append(name);
            if (exactString != null) {
                buf.append('=').append(exactString);
            } else if (regex != null) {
                buf.append("=/").append(regex.pattern()).append('/');
            }
            return buf.toString();
        }
    }
}
