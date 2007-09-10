/*
 * $Url: $
 * $Id: VersionRange.java 28983 2007-07-02 14:13:58Z fmeschbe $
 *
 * Copyright 1997-2007 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.sling.obr;

import org.osgi.framework.Version;

class VersionRange {

    /**
     * The default version range representing the range
     * <code>[0.0.0,&infin;)</code>.
     */
    public static final VersionRange DEFAULT = new VersionRange(null);

    /**
     * The low version of the range.
     */
    private Version m_low = null;

    /**
     * <code>true</code> if the lower version bound of the range is included
     * in the version range.
     */
    private boolean m_isLowInclusive = false;

    /**
     * The upper bound of the version range or <code>null</code> if the range
     * has no upper bound.
     */
    private Version m_high = null;

    /**
     * <code>true</code> if the upper version bound of the range is included
     * in the version range. This field is ignored if the upper bound is not
     * set, that is, if the version range is unbounded.
     */
    private boolean m_isHighInclusive = false;

    /**
     * Creates an instance of this class from the <code>range</code> version
     * range specification.
     * 
     * @param range The version range specification string according to section
     *            3.2.5, Version Ranges, of the OSGi Service Platform R4
     *            specification.
     */
    public VersionRange(String range) {
        // Check if the version is an interval.
        if (range == null || range.indexOf(',') < 0) {
            m_low = Version.parseVersion(range);
            m_isLowInclusive = true;
            m_high = null;
            m_isHighInclusive = false;
        } else {
            String s = range.substring(1, range.length() - 1);
            String vlo = s.substring(0, s.indexOf(','));
            String vhi = s.substring(s.indexOf(',') + 1, s.length());

            m_low = new Version(vlo);
            m_isLowInclusive = range.charAt(0) == '[';
            m_high = new Version(vhi);
            m_isHighInclusive = range.charAt(range.length() - 1) == ']';
            m_isHighInclusive = false;
        }
    }

    /**
     * Returns the lower bound of the version range.
     */
    public Version getLow() {
        return m_low;
    }

    /**
     * Returns <code>true</code> if the lower bound of the version range is
     * acceptable.
     */
    public boolean isLowInclusive() {
        return m_isLowInclusive;
    }

    /**
     * Returns the upper bound of the version range or <code>null</code> if
     * the version range is unbounded.
     */
    public Version getHigh() {
        return m_high;
    }

    /**
     * Returns <code>true</code> if the upper bound of the version range is
     * acceptable.
     */
    public boolean isHighInclusive() {
        return m_isHighInclusive;
    }

    /**
     * Returns <code>true</code> if the <code>version</code> is within this
     * version range.
     * 
     * @param version The <code>Version</code> to check.
     * @return <code>true</code>if the <code>version</code> is within this
     *         version range.
     */
    public boolean isInRange(Version version) {
        // We might not have an upper end to the range.
        if (m_high == null) {
            return (version.compareTo(m_low) >= 0);
        } else if (isLowInclusive() && isHighInclusive()) {
            return (version.compareTo(m_low) >= 0)
                && (version.compareTo(m_high) <= 0);
        } else if (isHighInclusive()) {
            return (version.compareTo(m_low) > 0)
                && (version.compareTo(m_high) <= 0);
        } else if (isLowInclusive()) {
            return (version.compareTo(m_low) >= 0)
                && (version.compareTo(m_high) < 0);
        }
        return (version.compareTo(m_low) > 0)
            && (version.compareTo(m_high) < 0);
    }

    /**
     * Returns this version range as an LDAP filter.
     */
    public String getFilter() {
        if (m_high == null) {
            return getComparisonFilter(m_low, ">", isLowInclusive());
        }

        return "(&" + getComparisonFilter(m_low, ">", isLowInclusive())
            + getComparisonFilter(m_high, "<", isHighInclusive()) + ")";
    }

    /**
     * Creates a comparison filter for the given operation and bound version.
     * 
     * @param v The version value to compare to.
     * @param op The operation to apply. This is '>' or '<'.
     * @param inclusive <code>true</code> if an inclusive comparison filter is
     *            to be returned.
     * @return The LDAP filter expression for the given operation and version.
     */
    private String getComparisonFilter(Version v, String op, boolean inclusive) {
        if (inclusive) {
            return "(version" + op + "=" + v + ")";
        }
        return "(&(version" + op + "=" + v + ")(!(version=" + v + ")))";
    }

    /**
     * Returns a string representation of this version range.
     */
    public String toString() {
        // return single version if there is no upper bound
        if (getHigh() == null) {
            return getLow().toString();
        }
        
        // return proper range string
        StringBuffer buf = new StringBuffer();
        buf.append(isLowInclusive() ? '[' : '(');
        buf.append(getLow());
        buf.append(',');
        buf.append(getHigh());
        buf.append(isHighInclusive() ? ']' : ')');
        return buf.toString();
    }
}