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
package org.apache.sling.slingstart.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A subsystem is a set of run modes and properties.
 * The properties can be used for specifying artifact versions.
 * At least it has a "global" run mode which describes the common subsystem.
 */
public class SSMSubsystem {

    public final List<SSMRunMode> runModes = new ArrayList<SSMRunMode>();

    public Map<String, String> properties = new HashMap<String, String>();

    public SSMSubsystem() {
        this.runModes.add(new SSMRunMode()); // global run mode
    }

    /**
     * Find the run mode if available
     * @param runModes
     * @return The run mode or null.
     */
    private SSMRunMode findRunMode(final String[] runModes) {
        SSMRunMode result = null;
        for(final SSMRunMode current : this.runModes) {
            if ( runModes == null && current.runModes == null ) {
                result = current;
                break;
            }
            if ( runModes != null && current.runModes != null ) {
                final List<String> a = new ArrayList<String>(Arrays.asList(runModes));
                final List<String> b = new ArrayList<String>(Arrays.asList(current.runModes));
                Collections.sort(a);
                Collections.sort(b);
                if ( a.equals(b) ) {
                    result = current;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Get the run mode if available
     * @return The run mode or null
     */
    public SSMRunMode getRunMode(final String runMode) {
       return findRunMode(new String[] {runMode});
    }

    /**
     * Get or create the run mode.
     */
    public SSMRunMode getOrCreateRunMode(final String[] runModes) {
        SSMRunMode result = findRunMode(runModes);
        if ( result == null ) {
            result = new SSMRunMode();
            result.runModes = runModes;
            this.runModes.add(result);
            Collections.sort(this.runModes, new Comparator<SSMRunMode>() {

                @Override
                public int compare(final SSMRunMode o1, final SSMRunMode o2) {
                    if ( o1.runModes == null ) {
                        if ( o2.runModes == null ) {
                            return 0;
                        }
                        return -1;
                    }
                    if ( o2.runModes == null ) {
                        return 1;
                    }
                    final List<String> a = new ArrayList<String>(Arrays.asList(o1.runModes));
                    final List<String> b = new ArrayList<String>(Arrays.asList(o2.runModes));
                    Collections.sort(a);
                    Collections.sort(b);

                    return a.toString().compareTo(b.toString());
                }
            });
        }
        return result;
    }

    /**
     * validates the object and throws an IllegalStateException
     *
     * @throws IllegalStateException
     */
    public void validate() {
        for(final SSMRunMode runMode : this.runModes) {
            runMode.validate();
        }
    }

    /**
     * Replace properties in the string.
     *
     * @throws IllegalArgumentException
     */
    public String getValue(final String v) {
        String msg = v;
        // check for variables
        int pos = -1;
        int start = 0;
        while ( ( pos = msg.indexOf('$', start) ) != -1 ) {
            if ( msg.length() > pos && msg.charAt(pos + 1) == '{' ) {
                final int endPos = msg.indexOf('}', pos);
                if ( endPos == -1 ) {
                    start = pos + 1;
                } else {
                    final String name = msg.substring(pos + 2, endPos);
                    final String value = this.properties.get(name);
                    if ( value == null ) {
                        throw new IllegalArgumentException("Unknown variable: " + name);
                    }
                    msg = msg.substring(0, pos) + value + msg.substring(endPos + 1);
                }
            } else {
                start = pos + 1;
            }
        }
        return msg;
    }

    /**
     * Merge two subsystems.
     */
    public void merge(final SSMSubsystem other) {
        for(final SSMRunMode mode : other.runModes) {
            final SSMRunMode mergeRunMode = this.getOrCreateRunMode(mode.runModes);
            mergeRunMode.merge(mode);
        }
        this.properties.putAll(other.properties);
    }

    @Override
    public String toString() {
        return "SSMSubsystem [runModes=" + runModes + ", properties="
                + properties + "]";
    }
}
