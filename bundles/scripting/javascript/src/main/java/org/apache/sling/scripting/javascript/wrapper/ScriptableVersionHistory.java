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
package org.apache.sling.scripting.javascript.wrapper;

import javax.jcr.version.VersionHistory;

/** Scriptable wrapper for the JCR VersionHistory class */
@SuppressWarnings("serial")
public class ScriptableVersionHistory extends ScriptableNode {

    public static final String CLASSNAME = "VersionHistory";
    public static final Class<?> [] WRAPPED_CLASSES = { VersionHistory.class };

    private VersionHistory versionHistory;

    @Override
    public void jsConstructor(Object res) {
        super.jsConstructor(res);
        versionHistory = (VersionHistory)res;
    }

    @Override
    protected Class<?> getStaticType() {
        return VersionHistory.class;
    }

    @Override
    public String getClassName() {
        return CLASSNAME;
    }

    @Override
    public Class<?>[] getWrappedClasses() {
        return WRAPPED_CLASSES;
    }

    @Override
    protected Object getWrappedObject() {
        return versionHistory;
    }
}