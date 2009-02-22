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
package org.apache.sling.osgi.obr;

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
            this.m_low = Version.parseVersion(range);
            this.m_isLowInclusive = true;
            this.m_high = null;
            this.m_isHighInclusive = false;
        } else {
            String s = range.substring(1, range.length() - 1);
            String vlo = s.substring(0, s.indexOf(','));
            String vhi = s.substring(s.indexOf(',') + 1, s.length());

            this.m_low = new Version(vlo);
            this.m_isLowInclusive = range.charAt(0) == '[';
            this.m_high = new Version(vhi);
            this.m_isHighInclusive = range.charAt(range.length() - 1) == ']';
            this.m_isHighInclusive = false;
        }
    }

    /**
     * Returns the lower bound of the version range.
     */
    public Version getLow() {
        return this.m_low;
    }

    /**
     * Returns <code>true</code> if the lower bound of the version range is
     * acceptable.
     */
    public boolean isLowInclusive() {
        return this.m_isLowInclusive;
    }

    /**
     * Returns the upper bound of the version range or <code>null</code> if
     * the version range is unbounded.
     */
    public Version getHigh() {
        return this.m_high;
    }

    /**
     * Returns <code>true</code> if the upper bound of the version range is
     * acceptable.
     */
    public boolean isHighInclusive() {
        return this.m_isHighInclusive;
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
        if (this.m_high == null) {
            return (version.compareTo(this.m_low) >= 0);
        } else if (this.isLowInclusive() && this.isHighInclusive()) {
            return (version.compareTo(this.m_low) >= 0)
                && (version.compareTo(this.m_high) <= 0);
        } else if (this.isHighInclusive()) {
            return (version.compareTo(this.m_low) > 0)
                && (version.compareTo(this.m_high) <= 0);
        } else if (this.isLowInclusive()) {
            return (version.compareTo(this.m_low) >= 0)
                && (version.compareTo(this.m_high) < 0);
        }
        return (version.compareTo(this.m_low) > 0)
            && (version.compareTo(this.m_high) < 0);
    }

    /**
     * Returns this version range as an LDAP filter.
     */
    public String getFilter() {
        if (this.m_high == null) {
            return this.getComparisonFilter(this.m_low, ">", this.isLowInclusive());
        }

        return "(&" + this.getComparisonFilter(this.m_low, ">", this.isLowInclusive())
            + this.getComparisonFilter(this.m_high, "<", this.isHighInclusive()) + ")";
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
        if (this.getHigh() == null) {
            return this.getLow().toString();
        }

        // return proper range string
        StringBuffer buf = new StringBuffer();
        buf.append(this.isLowInclusive() ? '[' : '(');
        buf.append(this.getLow());
        buf.append(',');
        buf.append(this.getHigh());
        buf.append(this.isHighInclusive() ? ']' : ')');
        return buf.toString();
    }
}