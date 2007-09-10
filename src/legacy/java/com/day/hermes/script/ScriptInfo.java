/*
 * $Id: ScriptInfo.java 22189 2006-09-07 11:47:26Z fmeschbe $
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import com.day.hermes.legacy.DeliveryHttpServletRequest;
import com.day.logging.FmtLogger;

/**
 * The <code>ScriptInfo</code> abstract defines an interface for container
 * classes. These container classes serve two purposes : During the script
 * arbitration phase, the {@link #matches} method is used to decide on which
 * script to execute. In the second phase, the <code>ScriptInfo</code>
 * instance is used to convey the name of the script to execute and the type
 * of this script. This type is then used by the {@link com.day.hermes.ScriptHandlerService}
 * to select the {@link ScriptHandler} to use for the execution of the script.
 *
 * @version $Revision: 1.9 $, $Date: 2006-09-07 13:47:26 +0200 (Don, 07 Sep 2006) $
 * @author fmeschbe
 * @since coati
 * @audience wad
 */
public abstract class ScriptInfo implements Comparable {

    /**
     * The <code>ScriptInfo</code> class is prepared for dynamic implementation
     * selection in a later phase. It will then have to be decided, how this
     * selection is handled. One possibility would be to store the fully
     * qualified class name in an atom of the configuration container. Yet
     * another possibility would be to list implementation classes in some
     * configuration file, while the atom in the container would contain some
     * generic name referring to an entry in the configuration list.
     *
     * For now the use of the {@link ScriptInfoImpl} class is fixed.
     */

    /** default logging */
    private static final FmtLogger log =
        (FmtLogger) FmtLogger.getLogger(ScriptInfo.class);

    /** The default implementation class */
    private static final Class defaultImplementation = ScriptInfoImpl.class;

    /**
     * Creates a new instance of a <code>ScriptInfo</code> implementation which
     * is configured from the configuration <code>Container</code>. This
     * container must not be null and contain all the needed configuration
     * <code>ContentElement</code>s for the instance to configure itself.
     *
     * @param config The configuraion <code>Container</code>.
     *
     * @return A new <code>ScriptInfo</code> instance or <code>null</code> if
     *      the instantiation or configuration failed.
     */
    public static ScriptInfo getInstance(Node config) {

        // todo decide on class to use, for now only default
        Class implementation = defaultImplementation;

        try {
            ScriptInfo si = (ScriptInfo) implementation.newInstance();
            si.init(config);
            return si;

        } catch (RepositoryException re) {

            // si.init()
            log.error("getInstance: Cannot initialize script info {0}",
                implementation.getName(), re);

        } catch (InstantiationException ie) {

            // newInstance()
            log.error("getInstance: Cannot instantiate class {0}",
                implementation.getName(), ie);

        } catch (IllegalAccessException iae) {

            // newInstance()
            log.error("getInstance: Cannot access class {0}",
                implementation.getName(), iae);

        } catch (IllegalArgumentException iae) {

            // init() - if script name is empty
            log.error("getInstance: Failed configuring class {0}",
                implementation.getName(), iae);

        }

        // no script info instance here
        return null;
    }

    /**
     * Configures the instance from the configuration <code>Container</code>.
     * This method is called by the {@link #getInstance} method to initialize
     * the newly created instance.
     *
     * @param config The configuraion <code>Container</code>.
     *
     * @throws IllegalArgumentException may be thrown if any configuration value
     *      does not meet the needs of the implementation. The message should
     *      convey information as to what was wrong.
     * @throws NullPointerException may be thrown if a configuration value is
     *      <code>null</code>. The message should contain the name of the
     *      configuration atom being <code>null</code>.
     */
    protected abstract void init(Node config) throws RepositoryException;

    /**
     * Returns the name of the script configured for this <code>ScriptInfo</code>.
     *
     * @return The name of the script configured for this <code>ScriptInfo</code>.
     */
    public abstract String getScriptName();

    /**
     * Returns the type of the script. Used to select the {@link ScriptHandler}
     * to handle the execution of the script.
     *
     * @return The type of the script for {@link ScriptHandler} selection.
     */
    public abstract String getType();

    /**
     * Matches against the request. This method is used during script
     * arbitration to find a script to execute for a request. The implementation
     * has the complete request at its disposal to find a match. If the
     * request matches with this instance, the script is eligible to be called
     * to handle the request.
     *
     * @param request The {@link DeliveryHttpServletRequest} to match against.
     *
     * @return <code>true</code> if the implementation decides, this
     *      <code>ScriptInfo</code> should handle the request.
     */
    public abstract boolean matches(DeliveryHttpServletRequest request);

    /**
     * Returns the hash code of instance. This hash code must be based on the
     * same objects, which contribute to the result of the {@link #equals}
     * method.
     */
    public abstract int hashCode();

    /**
     * Returns <code>true</code> if two instances are considered equal. It is
     * up to the implementation to define what is considered equality.
     * Specifically it is permissible to limit equality to instances of the
     * same implementing class. This method should be based on the same instance
     * fields for the decision as is the {@link #hashCode} method for the
     * calculation of the hash code.
     *
     * @param obj The <code>Object</code> to compare this instance to.
     */
    public abstract boolean equals(Object obj);

    //---------- Comparable interface ------------------------------------------

    /**
     * Compares this to another object. The other object must be an instance of
     * an implementation of the <code>ScriptInfo</code> class. If this and the
     * other object are of the same class (that is if <code>getClass() ==
     * o.getClass()</code>), the comparison is up to the implementation.
     * Otherwise the result of the comparison is expected to be the comparison
     * result of comparing the string representations, that is
     * <code>this.toString().compareTo(o.toString())</code>, which results in
     * lexical ordering of the string representations in this case.
     *
     * @param   o the Object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *		is less than, equal to, or greater than the specified object.
     *
     * @throws ClassCastException if the specified object's type is not an
     *      implementation of the <code>ScriptInfo</code> abstract class.
     *
     * @see "The JavaDoc of the <code>Comparable.compareTo(Object)</code>
     *      method for a discussion of comparison and especially, the
     *      recommended consistency with equality"
     */
    public abstract int compareTo(Object o);
}