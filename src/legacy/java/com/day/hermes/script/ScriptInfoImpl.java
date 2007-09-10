/*
 * $Id: ScriptInfoImpl.java 22189 2006-09-07 11:47:26Z fmeschbe $
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
package com.day.hermes.script;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;

import com.day.hermes.legacy.DeliveryHttpServletRequest;
import com.day.logging.FmtLogger;
import com.day.text.GlobPattern;
import com.day.text.Text;

/**
 * The <code>ScriptInfo</code> contains all the configured properties of
 * one single <code>&lt;script /&gt;</code> element of a
 * <code>&lt;template /&gt;</code>
 *
 * @version $Revision: 1.11 $, $Date: 2006-09-07 13:47:26 +0200 (Don, 07 Sep 2006) $
 * @author fmeschbe
 * @since antbear
 * @audience core
 */
class ScriptInfoImpl extends ScriptInfo implements Comparable {

    /** default logging */
    private static final FmtLogger log =
        (FmtLogger) FmtLogger.getLogger(ScriptInfoImpl.class);

    /**
     * The name of the system property optionally containing a list of
     * methods to restrict the default behaviour. If this property is not set,
     * all script infos containing no <em>Methods</em> property default to only
     * accept requests for one of the listed method names. If the property is
     * not set, script infos containing no <em>Methods</em> property default to
     * accept any HTTP request method.
     * <p>
     * This value of the property is treated like the standard <em>Method</em>
     * property values of script infos : It is assumed to contain a comma
     * separated list of method names. Whitespace around method names is ignored.
     * If any of the methods is the asterisk (<em>*</em>) the list defaults to
     * accept all methods. Note that if the property is defined with an empty
     * value, this in fact forces all script infos to have mandatory
     * <em>Method</em> property settings, elso no requests will ever be handled.
     * <p>
     * <<strong><em>NOTE: THIS IS UNDOCUMENTED INTERMEDIATE BEHAVIOUR AND MAY
     * CHANGE IN FUTURE RELEASES WITHOUT ANY ADVANCE NOTICE. USE OF THIS FEATURE
     * IS NEITHER SUPPORTED NOR GENERALLY ENDORSED BY DAY SOFTWARE,
     * INC.</em></strong>
     */
    private static final String RESTRICTED_METHODS_PROPERTY =
            "com.day.hermes.script.restricted_methods";

    /**
     * Special globbing pattern to match all queries. This is the default
     * globbing pattern if glob is missing or empty upon <code>ScriptInfo</code>
     * construction.
     */
    private static final String MATCH_ALL = "*";

    /** The default method set if not configured */
    private static final SortedSet DEFAULT_METHODS;

    /**
     * Globbing pattern for the page query. This pattern is matched against
     * the query part of a page ruquest to check whether this
     * <code>ScriptInfo</code> may be used to handle the request.
     */
    private String glob;

    /**
     * List of globbing parts from the globbing pattern. Upon creation of the
     * <code>ScriptInfo</code> the globbing pattern (if not MATCH_ALL) is split
     * at dots and the parts stored in the String array. If the globbing pattern
     * equals {@link #MATCH_ALL} <code>globParts</code> is set to null.
     */
    private String[] globParts;

    /**
     * The names of methods, for which this <code>ScriptInfo</code> may handle
     * the request. If this set is <code>null</code> all methods may be handled
     * if the set is empty, no methods are accepted.
     * <p>
     * This set is sorted such that the <code>toString</code> method returns
     * an ordered list of string values such that the {@link #compareTo} method
     * returns a deterministic ordering between two different methods fields.
     */
    private SortedSet methods;

    /**
     * Name of the script to call to handle the request. This name is the name
     * of a 'page' which is got from the contentbus.
     */
    private String name;

    /**
     * Type of the script named by m_name. If this information is missing the
     * type of the script is derived from the extension of the script name. This
     * type identifier should not be treated case-sensitive !
     */
    private String type;

    /** The cached result of the toString() call. */
    private String stringValue;

    /** The number of dot-separated parts, used in {@link #compareTo} */
    private int width;

    /** The number of trailing "*"-only parts */
    private int trailingStars;

    /** The number of "*"-only parts */
    private int numStars;

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
            log.info("ScriptInfoImpl: Default method set: {0}", DEFAULT_METHODS);
        } else {
            log.debug("ScriptInfoImpl: Accepting all methods by default");
            DEFAULT_METHODS = null;
        }
    }

    /**
     * Creates an uninitialized instance of the default {@link ScriptInfo}
     * implementation. This should only be called by the
     * {@link ScriptInfo#getInstance} method, which is why this is protected.
     */
    protected ScriptInfoImpl() {}

    /**
     * Initializes the <code>ScriptInfoImpl</code> instance with the values
     * found in the container. This method depends on the container having at
     * least the followin atoms :
     * <dl>
     * <dt><em>Glob</em><dd>The selector glob pattern. This <code>ScriptInfo</code>
     *      is elligible to handle a request if the selector string of the
     *      request matches this pattern. If the glob pattern atom has an empty
     *      value, the pattern is assumed to match all, that is an empty value
     *      is the same as the string "*"
     * <dt><em>Type</em><dd>The type of the script. The type is used to select
     *      a {@link ScriptHandler} to call the named script. If this atom does
     *      not have a value, the extension of the script name is taken as the
     *      type.
     * <dt><em>Name</em><dd>The (path) name of a script. This defines the script
     *      called by the {@link ScriptHandler} selected by the type. It is up
     *      to the <code>ScriptHandler</code> implementation how relative .path
     *      names are handled. This is the only atom, which must have a non-epmty
     *      value.
     * <dt><em>Methods</em><dd>Contains a possibly empty list of method names. If
     *      a request's method is contained in this list, this
     *      <code>ScriptInfo</code> may be used to handle the request. If this
     *      atom is missing, the list is assumed to match any method name. If
     *      the atom has the empty value, no method names match, that is this
     *      <code>ScriptInfo</code> will never be used to handle requests.
     *      Otherwise the atom is assumed to contain a comma separated list of
     *      method names. Leading and trailing whitespace (see
     *      <code>String.trim()</code>) is ignored. Mtehod names are handled
     *      case insensitive for matching purposes.
     * </dl>
     *
     * @param config The <code>Container</code> containing the configuration
     *      values for this <code>ScriptInfo</code>.
     *
     * @throws ContentBusException if accessing the configuration atoms
     *      throws such.
     * @throws IllegalArgumentException if the script name is missing from the
     *      container.
     */
    protected void init(Node config) throws RepositoryException {
        String glob = config.getProperty("Glob").getString();
        String type = config.getProperty("Type").getString();
        String name = config.getProperty("Name").getString();

        Property methods;
        if (config.hasProperty("Methods")) {
            methods = config.getProperty("Methods");
        } else {
            methods = null;
        }

        // Check for name and glob pattern
        if ((name == null) || (name.length() == 0))
            throw new IllegalArgumentException("Missing name");

        // Check for glob pattern. Use MATCH_ALL if missing or empty
        if (glob.length() == 0) {
            glob = MATCH_ALL;
        }

        // Check for type argument. Derive from script name if not present
        if ((type == null) || (type.length() == 0)) {
            int lastDot = name.lastIndexOf('.');
            if (lastDot >= 0) type = name.substring(lastDot + 1);
        }

        this.glob = glob;
        this.name = name;
        this.type = type;
        this.methods = createMethodSet(methods);
        this.stringValue = null;

        this.globParts = (glob.equals(MATCH_ALL))
                ? null
                : Text.split(glob, '.');

        if (globParts == null) {
            // glob is "*"
            this.width = 0;
            this.trailingStars = 1;
            this.numStars = 1;
        } else {
            this.width = globParts.length;
            this.trailingStars = 0;
            this.numStars = 0;

            // calculate trailing stars
            for (int i = globParts.length - 1; i >= 0
                && globParts[i].equals(MATCH_ALL); i--, trailingStars++) {
            }

            // calculate total number of stars
            for (int i = globParts.length - 1; i >= 0; i--) {
                if (globParts[i].equals(MATCH_ALL)) {
                    numStars++;
                }
            }
        }

        log.trace("init: {0}", this);
    }

    /**
     * Get the script name
     * @return the script name
     */
    public String getScriptName() {
        return name;
    }

    /**
     * Get the type
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * Matches the method against the supported method names (if any) and the
     * query against the globbing pattern returning true if a match
     * is found. A match is found, if one of the following holds true :
     * <ol>
     * <li>the globbing patterns is equal to {@link #MATCH_ALL}
     * <li>the query and globbing pattern contain the same number of parts
     *     and each part is of the query matches the respective part of
     *     the globbing pattern.
     * </ol>
     * <p>
     * If the methods list is <code>null</code> this method matches any method
     * name given else the method must be in the accepted set.
     *
     * @param request The {@link DeliveryHttpServletRequest} containing the
     *      information needed to match this <code>ScriptInfo</code>.
     *
     * @return true if the query matches against the globbing pattern
     */
    public boolean matches(DeliveryHttpServletRequest request) {
        String method = request.getRealMethod();
        String query = request.getSelectorString();
        return matchMethod(method) && matchQuery(query);
    }

    //---------- Comparable interface ------------------------------------------

    /**
     * Compares this object with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.
     * <p>
     * The following cases are treated as follows :
     * <ol>
     * <li>If the other object equals this, 0 is returned
     * <li>If the other object is <code>null</code> a <code>ClassCastException</code>
     *      is thrown
     * <li>If the other object's class is not an extension of the
     *      {@link ScriptInfo} class, a <code>ClassCastException</code> is
     *      thrown, too
     * <li>If the other object's class is not the same class as this, the result
     *      of comparing this object's string representation to the other
     *      object's string representation is returned.
     * <li>If the other object's class is also the <code>ScriptInfoImpl</code>
     *      class (and not an extension of it !), the comparison follows this
     *      pattern (see below for the definition of words) :
     *      <ol>
     *      <li>If this width is not equal to the other object's width, return
     *          a negative integer if this width is higher than the other's,
     *          else return a positive integer.
     *      <li>Else, if this trailingStars is not equal to the other object's
     *          trailingStars, return a negative integer if this trailingStars
     *          is less than the other's, else return a positive integer.
     *      <li>Else, if this numStars is not equal to the other object's
     *          numStars, return a negative integer if this numStars is less
     *          than the other's, else return a positive integer.
     *      <li>Else, if the patterns are not equal, return the result of
     *          comparing this pattern to the other's pattern. This results in
     *          an ordering based on lexical ordering of globbing patterns
     *      <li>Else, if only this or the other have a method set (but not
     *          both), return a negative integer, if this has a method set,
     *          else return a positive integer.
     *      <li>Else, if the two method sets have an intersection. If so, log
     *          a warning message.
     *      <li>And finally return the result of comparing the string
     *          representation of this method set to the other object's method
     *          set. This results in an ordering based on lexical ordering of
     *          the method set's string representation.
     *      </ol>
     * </ol>
     * <p>
     * Some definitions used in above algorithm description :
     * <dl>
     * <dt><em>width</em><dd>The width of a <code>ScriptInfoImpl</code> object
     *      is defined as the number of dot-separated elements contained in the
     *      globbing pattern. E.g.: The pattern <em>Par.*.Img</em> has a width
     *      of three. The special match-all pattern (single <em>*</em>) has a
     *      width of 0.
     * <dt><em>trailingStars</em><dd>The number of dot-separated elements at
     *      the end of the globbing pattern consisting of only stars.
     *      E.g.: The pattern <em>Par.*.Img</em> has no trailing stars, while
     *      the pattern <em>Par.*.*</em> has two trailing stars. The special
     *      match-all pattern (single <em>*</em>) has 1 trailing stars.
     * <dt><em>numStars</em><dd>The number of dot-separated elements in the
     *      globbing pattern consisting of only stars. E.g.: The pattern
     *      <em>Par.*.Img</em> has 1 star, while the pattern <em>Par.*.*</em>
     *      has two stars. The special match-all pattern (single <em>*</em>) has
     *      1 star.
     * </dl>
     * <p>
     * This implementation is consistent with equals as it always returns 0
     * if the other object equals this.
     *
     * @param   o the Object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *		is less than, equal to, or greater than the specified object.
     *
     * @throws ClassCastException if the specified object's type prevents it
     *         from being compared to this Object.
     */
    public int compareTo(Object o) {

        // check for equality and null
        if (equals(o)) {
            return 0;
        } else if (o == null) {
            throw new ClassCastException("Cannot compare to null");
        }

        // check for correct class of o
        if (!(o instanceof ScriptInfo)) {
            throw new ClassCastException(o
                + " is not an implementation of ScriptInfo");
        }

        // if o's class is not a ScriptInfoImpl (not even an extension !)
        if (getClass() != o.getClass()) {
            log.debug("compareTo: Comparing String representation of two"
                + "different implementations");
            return toString().compareTo(o.toString());
        }

        // cast to the correct class for further comparisons
        ScriptInfoImpl other = (ScriptInfoImpl) o;

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
        int globComp = glob.compareTo(other.glob);
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

    // ---------- Object Overwrites
    // ---------------------------------------------

    /**
     * Returns the hash code of this object. The hashcode is based on the
     * hashcode of the glob pattern and the hashcode of the method set.
     */
    public int hashCode() {
        // methods might be null if all methods apply
        int methodsHash = (methods == null) ? 0 : methods.hashCode();
        return 37 * glob.hashCode() + methodsHash;
    }

    /**
     * Returns true if the object can be considered equal to this object. Two
     * objects are equal if they are the same object (i.e. <code>obj == this</code>)
     * or if both are <code>ScriptInfoImpl</code> objects and the glob pattern
     * and the method sets of both objects are equal.
     *
     * @param obj The <code>Object</code> to compare this object to.
     *
     * @return <code>true</code> if this object is equal to the other
     *      object.
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (obj instanceof ScriptInfoImpl) {
            ScriptInfoImpl sii = (ScriptInfoImpl) obj;

            // methods may be null if all methods apply
            boolean methodsEqual = (methods == null)
                    ? sii.methods == null
                    : methods.equals(sii.methods);

            return methodsEqual && glob.equals(sii.glob);
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
            buf.append(glob);
            buf.append(", methods=");
            buf.append(methods);
            buf.append(", name=");
            buf.append(name);
            buf.append(", type=");
            buf.append(type);

            buf.append(", width=").append(width);
            buf.append(", trailingStars=").append(trailingStars);
            buf.append(", numStars=").append(numStars);

            stringValue = buf.toString();
        }

        return stringValue;
    }

    //---------- internal ------------------------------------------------------

    /**
     * Creates the set of accepted method names from the string. The string
     * contains a comma separated list of method names. Case of the names does
     * not matter and whitespace are and method name is ignored (whitespace is
     * defined in the JavaDoc to the <code>String.trim()</code> method).
     * <p>
     * If the list contains the special method <code>*</code> denoting any
     * method, null is returned to indicate this fact. If <code>*</code> is
     * not the only entry in the list, the other entries are ignored. Empty
     * method names in the list are ignored.
     * <p>
     * If the list is <code>null</code>, the methods accepted are the
     * <em>GET</em> and the <em>POST</em> method only. If the list is the empty
     * string, an empty set is returned indicating, that no method names will
     * ever match.
     *
     * @param methodNames The list of method names as specified.
     *
     * @return The method name set, which may be <code>null</code> if all
     * 		method names should be matched.
     */
    private static SortedSet createMethodSet(Property methodNames)
            throws RepositoryException {

        // short cut for special lists <code>null</code> and "*"
        if (methodNames == null) {
            return DEFAULT_METHODS;
        }

        Value[] values = methodNames.getValues();
        SortedSet methods = new TreeSet(); // new HashSet();
        for (int i=0; i < values.length; i++) {
            // get the method name and trim whitespace.
            String token = values[i].getString().trim();

            // if "*" is contained in the list, drop out
            if ("*".equals(token)) {
                return null;
            } else if (token.length() > 0) {
                methods.add(token.toUpperCase());
            }
        }

        return methods;
    }

    /**
     * Matches the query against the globbing pattern returning true if a match
     * is found. A match is found, if one of the following holds true :
     * <ol>
     * <li>the globbing patterns is equal to {@link #MATCH_ALL}
     * <li>the query and globbing pattern contain the same number of parts
     *     and each part is of the query matches the respective part of
     *     the globbing pattern.
     * </ol>
     *
     * @param query the query part of the request URL to match
     *
     * @return true if the query matches against the globbing pattern
     */
    private boolean matchQuery(String query) {

        // if match all, we match regardless of query !
        if (globParts == null) {
            return true;

        // if query is null, we will not be able to match
        } else if (query == null) {
            return false;

        // Normal case : split query on dots and compare each part
        } else {
            StringTokenizer qparts = new StringTokenizer(query, ".");

            // Match at least the number of parts in the globbing !
            if (qparts.countTokens() < globParts.length) return false;

            for (int i = 0; i < globParts.length; i++) {
                if (!GlobPattern.matches(globParts[i], qparts.nextToken()))
                    return false;
            }
            return true;
        }
    }

    /**
     * Matches the method name against the accepted method names. This method
     * returns <code>true</code> for any parameter if the set of method names
     * is <code>null</code>.
     * <p>
     * The name match is handled case insensitive.
     *
     * @param method The name of the method to match in the set of accepted
     * 		method names.
     *
     * @return <code>true</code> if either the set of accepted names is
     * 		<code>null</code> or if the method is contained in the set.
     */
    private boolean matchMethod(String method) {
	return methods == null ||
	    (method != null && methods.contains(method.toUpperCase()));
    }

}
