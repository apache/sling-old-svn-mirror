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
package org.apache.sling.ujax;

/**
 * Records a change that is used by the changelog
 */
public class Change {

    /**
     * available change types
     */
    public enum Type {
        CREATED,
        MODIFIED,
        DELETED,
        MOVED
    }

    /**
     * type of the change
     */
    private final Type type;

    /**
     * arguments
     */
    private final String[] arguments;

    /**
     * Creates a new change with the given type and arguments
     * @param type change type
     * @param arguments arguments of the change
     */
    public Change(Type type, String ... arguments) {
        this.type = type;
        this.arguments = arguments;
    }

    /**
     * Returns the type
     * @return the type
     */
    public Type getType() {
        return type;
    }

    /**
     * Returns the arguments
     * @return the arguments
     */
    public String[] getArguments() {
        return arguments;
    }

    /**
     * {@inheritDoc}
     */
    public String toString() {
        StringBuffer ret = new StringBuffer(type.name().toLowerCase());
        String delim = "(";
        for (String a: arguments) {
            ret.append(delim);
            ret.append('\"');
            ret.append(a);
            ret.append('\"');
            delim = ", ";
        }
        ret.append(");");
        return ret.toString();
    }
}