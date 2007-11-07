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

import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * The <code>BundleSpec</code> class represents a bundle specification entry
 * in the Assembly Bundle's <code>Assembly-Bundles</code> manifest header.
 * This header is specified to be formatted according to the following
 * productions:
 *
 * <pre>
 *                  AssemblyBundles = &quot;Assembly-Bundles:&quot; BundleList .
 *                  BundleList = BundleSpec { &quot;,&quot; BundleSpec } .
 *                  BundleSpec = SymbolicName { &quot;;&quot; Parameter } .
 *                  Parameter = ParameterName &quot;=&quot; ParameterValue .
 *                  ParameterName = &quot;version&quot; | &quot;startlevel&quot; | &quot;linked&quot; .
 *                  ParameterValue = // depending on ParameterName
 * </pre>
 *
 * <p>
 * Note that the <code>ParameterValue</code> must be eclosed in double-quotes
 * if the value contains a comma or semi-colon.
 * <p>
 * The <code>SymbolicName</code> is the bundle's symbolic name as returned
 * from the <code>Bundle.getSymbolicName()</code> method when the bundle is
 * installed.
 * <p>
 * The parameters for a bundle specification are as follows: <table>
 * <tr>
 * <th>Name</th>
 * <th>Default Value</th>
 * <th>Description</th>
 * </tr>
 * <tr valign="top">
 * <td><code>version</code>
 * <td><code>[0.0.0,&infin;)</code>
 * <td>The required version of the bundle.</tr>
 * <tr valign="top">
 * <td><code>startlevel</code>
 * <td><code>StartLevel.getInitialBundleStartLevel()</code>
 * <td>The explicit startlevel to assign the bundle. If not specified the
 * default initial bundle start level is assigned.</tr>
 * <tr valign="top">
 * <td><code>link</code>
 * <td><code>true</code>
 * <td>Whether the bundle is started and stopped along with the assembly bundle
 * or not. </tr>
 * </table>
 * <p>
 * Any parameter not listed above is silently ignored.
 *
 * @author fmeschbe
 */
class BundleSpec {

    /**
     * The Location Scheme for a bundle installed from an Assembly Bundle (value
     * is "assembly://").
     */
    public static final String LOCATION_SCHEME = "assembly://";

    /**
     * The Location Scheme for a bundle installed from an OSGi Bundle Repository
     * through the <code>org.apache.felix.bundlerepository</code> bundle
     * (value is "obr://").
     * <p>
     * This value is taken from the source code of the Felix Bundle Repository
     * bundle and is considered internal knowledge.
     */
    public static final String LOCATION_SCHEME_OBR = "obr://";

    /**
     * A simple regular expression pattern to check the syntactic validity of
     * the bundle symbolic name in the bundle specification (value is
     * /[0-9a-zA-Z_.-]&lowast;/). This pattern is just a simple check but should
     * suffice it for most applications.
     */
    private static final Pattern SYMBOLICNAME = Pattern.compile("[0-9a-zA-Z_.-]*");

    /**
     * The bundle symbolic name extracted from the bundle specification.
     */
    private String symbolicName;

    /**
     * The version specification of the bundle to install. Default value is
     * {@link VersionRange#DEFAULT}.
     */
    private VersionRange version = VersionRange.DEFAULT;

    /**
     * The start level to assign the bundle after installation. Default value is
     * <code>-1</code> to indicate to use the initial bundle start level of
     * the StartLevel service.
     */
    private int startLevel = -1;

    /**
     * If the bundle data is embedded in the assembly bundle, this field
     * contains the path to the bundle entry. If this field is empty or
     * <code>null</code> the bundle will be installed from an OSGi repository.
     */
    private String entry;

    /**
     * Whether the bundle is started and stopped when the Assembly Bundle is
     * started and stopped. Default value is <code>true</code>.
     */
    private boolean linked = true;

    /**
     * The cached common location returned from the {@link #getCommonLocation()}
     * method. This method is used to get the partial identity of a bundle
     * installed from an Assembly Bundle and as such is used many times
     * throughout the Assembly Manager.
     */
    private String commonLocation;

    /**
     * Creates an instance of this class setting fields from the
     * <code>bundleSpec</code> string as specified with the
     * <code>BundleSpec</code> production above.
     *
     * @param bundleSpec The bundle specification string as defined above.
     * @throws IllegalArgumentException If the <code>bundleSpec</code> is
     *             empty or the symbolic name is not a valid symbolic name.
     */
    BundleSpec(String bundleSpec) {
        StringTokenizer tokener = new StringTokenizer(bundleSpec, ";");

        // If the bundle
        if (!tokener.hasMoreTokens()) {
            throw new IllegalArgumentException("Illegal Bundle specification: "
                + bundleSpec);
        }

        this.symbolicName = this.checkSymbolicName(tokener.nextToken().trim());

        while (tokener.hasMoreTokens()) {
            String parm = tokener.nextToken().trim();
            int eq = parm.indexOf('=');
            if (eq <= 0 || eq >= parm.length() - 1) {
                // ignore this
                continue;
            }

            String name = parm.substring(0, eq);
            String value = this.unquote(parm.substring(eq + 1));

            if ("version".equals(name)) {
                this.version = new VersionRange(value);

            } else if ("startlevel".equals(name)) {
                try {
                    int startLevel = Integer.parseInt(value);
                    if (startLevel > 0) {
                        this.startLevel = startLevel;
                    }
                } catch (NumberFormatException nfe) {
                    // don't care or throw IllegalArgumentException
                }

            } else if ("entry".equals(name)) {
                this.entry = value;
            } else if ("linked".equals(name)) {
                this.linked = Boolean.valueOf(value).booleanValue();
            }
        }
    }

    /**
     * Returns the symbolic name of this bundle specification.
     */
    public String getSymbolicName() {
        return this.symbolicName;
    }

    /**
     * Returns the version requirement of this bundle specification.
     */
    public VersionRange getVersion() {
        return this.version;
    }

    /**
     * Returns the bundle's start level or <code>-1</code> to use the initial
     * bundle start level from the StartLevel service.
     */
    public int getStartLevel() {
        return this.startLevel;
    }

    /**
     * Returns the entry path for this bundle spec. If <code>null</code> or
     * empty, the bundle is to be installed from an OSGi Bundle Repository.
     */
    public String getEntry() {
        return this.entry;
    }

    /**
     * Returns <code>true</code> if the bundle is to be started and stopped
     * when the Assembly Bundle to which the bundle belongs is started and
     * stopped.
     */
    public boolean isLinked() {
        return this.linked;
    }

    /**
     * Returns an URL string representing the common bundle location prefix. All
     * bundles installed from Assembly Bundle entries are given a location
     * starting with this common location.
     * <p>
     * This common loction is appended an installation timestamp to create a
     * truly unique location name.
     *
     * @return The bundle location URL prefix
     */
    public String getCommonLocation() {
        if (this.commonLocation == null) {
            this.commonLocation = LOCATION_SCHEME + this.getSymbolicName() + "/";
        }

        return this.commonLocation;
    }

    /**
     * Returns an URL string representing the bundle location prefix used by the
     * Felix Bundle Repository bundle. All bundles installed through the Felix
     * Bunlde Repository bundle from an OSGi Bundle Repository are given a
     * location starting with this OBR location.
     *
     * @return The bundle location URL prefix for bundles installed from an OSGi
     *         bundle repository by the Felix Bundle Repository bundle.
     */
    public String getObrLocation() {
        return LOCATION_SCHEME_OBR + this.getSymbolicName() + "/";
    }

    /**
     * Returns an LDAD filter according to the Filter Syntax specified in
     * section 3.2.6 of the OSGi Service Platform R4 specification. The filter
     * includes the bundle symolic name and - if not the default - the version
     * range filter. This filter may be used to find the bundles in the OSGi
     * Bundle Repository.
     */
    public String toFilter() {
        if (this.version == null || VersionRange.DEFAULT.equals(this.version)) {
            return "(symbolicname=" + this.getSymbolicName() + ")";
        }

        return "(&(symbolicname=" + this.getSymbolicName() + ")"
            + this.getVersion().getFilter() + ")";
    }

    /**
     * Returns a string representation of this instance containing the symbolic
     * name and the version specification.
     */
    public String toString() {
        return "Bundle " + this.getSymbolicName() + ", " + this.version;
    }

    /**
     * Checks whether the <code>symbolicName</code> is not empty and conforms
     * to the {@link #SYMBOLICNAME symbolic name regular expression}.
     *
     * @param symbolicName The symbolic name to check.
     * @return The valid symbolic name.
     * @throws IllegalArgumentException If the symbolic name is not valid.
     */
    private String checkSymbolicName(String symbolicName) {
        if (symbolicName == null || symbolicName.length() == 0) {
            throw new IllegalArgumentException(
                "Symbolic Name must not be empty");
        }

        if (!SYMBOLICNAME.matcher(symbolicName).matches()) {
            throw new IllegalArgumentException("Symbolic Name " + symbolicName
                + " contains illegal characters");
        }

        return symbolicName;
    }

    /**
     * Removes any leading and trailing double-quotes from the
     * <code>value</code> and returns the modified (or unmodified if no
     * double-quotes had to be removed) value.
     *
     * @param value The value to unquote.
     *
     * @return The unquoted value.
     */
    private String unquote(String value) {
        if (value.startsWith("\"")) {
            value = value.substring(1);
        }

        if (value.endsWith("\"")) {
            value = value.substring(0, value.length() - 1);
        }

        return value;
    }
}
