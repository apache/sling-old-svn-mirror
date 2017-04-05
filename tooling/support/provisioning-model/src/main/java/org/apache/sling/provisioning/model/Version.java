/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.provisioning.model;

/**
 * Version object supporting Maven and OSGi versions.
 * @since 1.4
 */
public class Version implements Comparable<Version> {

    private final int majorVersion;
	private final int minorVersion;
	private final int microVersion;
	private final String qualifier;

	/**
	 * Creates a version identifier from the specified string.
	 * @throws IllegalArgumentException if the version string can't be parsed
	 */
	public Version(final String version) {
	    String parts[] = version.split("\\.");
	    if ( parts.length > 4 ) {
	        throw new IllegalArgumentException("Invalid version " + version);
	    }
	    if ( parts.length < 4) {
    	    final int pos = parts[parts.length - 1].indexOf('-');
    	    if ( pos != -1 ) {
    	        final String[] newParts = new String[4];
    	        newParts[0] = parts.length > 1 ? parts[0] : parts[0].substring(0, pos);
                newParts[1] = parts.length > 2 ? parts[1] : (parts.length > 1 ? parts[1].substring(0, pos) : "0");
                newParts[2] = parts.length > 3 ? parts[2] : (parts.length > 2 ? parts[2].substring(0, pos) : "0");
                newParts[3] = parts[parts.length - 1].substring(pos + 1);
                parts = newParts;
    	    }
	    }
	    this.majorVersion = parseInt(parts[0], version);
	    if ( parts.length > 1 ) {
	        this.minorVersion = parseInt(parts[1], version);
	    } else {
	        this.minorVersion = 0;
	    }
        if ( parts.length > 2 ) {
            this.microVersion = parseInt(parts[2], version);
        } else {
            this.microVersion = 0;
        }
        this.qualifier = (parts.length > 3 ? parts[3] : "");
	}

	/**
	 * Get the major version
	 * @return The major version
	 * @since 1.8.0
	 */
	public int getMajorVersion() {
        return majorVersion;
    }

    /**
     * Get the major version
     * @return The major version
     * @since 1.8.0
     */
    public int getMinorVersion() {
        return minorVersion;
    }

    /**
     * Get the minor version
     * @return The minor version
     * @since 1.8.0
     */
    public int getMicroVersion() {
        return microVersion;
    }

    /**
     * Get the qualifier
     * @return The qualifier, the qualifier might be the empty string.
     * @since 1.8.0
     */
    public String getQualifier() {
        return qualifier;
    }

    /**
	 * Parse an integer.
	 */
	private static int parseInt(final String value, final String version) {
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid version " + version);
		}
	}

	@Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + majorVersion;
        result = prime * result + microVersion;
        result = prime * result + minorVersion;
        result = prime * result + ((qualifier == null) ? 0 : qualifier.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Version other = (Version) obj;
        if (majorVersion != other.majorVersion)
            return false;
        if (microVersion != other.microVersion)
            return false;
        if (minorVersion != other.minorVersion)
            return false;
        if (qualifier == null) {
            if (other.qualifier != null)
                return false;
        } else if (!qualifier.equals(other.qualifier))
            return false;
        return true;
    }

    /**
	 * Compares this {@code Version} object to another {@code Version}.
	 */
	@Override
    public int compareTo(final Version other) {
	    int result = 0;
		if (other != this) {

	        result = majorVersion - other.majorVersion;
	        if (result == 0) {
	            result = minorVersion - other.minorVersion;
	            if (result == 0) {
	                result = microVersion - other.microVersion;
	                if (result == 0) {
	                    result = qualifier.compareTo(other.qualifier);
	                    if ( result != 0 ) {
	                        if ( "SNAPSHOT".equals(qualifier) ) {
	                            result = -1;
	                        } else if ( "SNAPSHOT".equals(other.qualifier) ) {
	                            result = 1;
	                        }
	                    }
	                }
	            }

	        }

		}
		return result;
	}

    @Override
    public String toString() {
        return String.valueOf(this.majorVersion) + "."
                + String.valueOf(this.minorVersion + "."
                + String.valueOf(this.microVersion) +
                (this.qualifier.length() == 0 ? "" : "." + this.qualifier));
    }
}
