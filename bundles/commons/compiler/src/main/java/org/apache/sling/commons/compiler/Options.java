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
package org.apache.sling.commons.compiler;

import java.util.HashMap;

/**
 * Options for the compilation process.
 */
public class Options extends HashMap<String, Object> {

    private static final long serialVersionUID = 1576005888428747431L;

    /** The key for the source version. */
    public static final String KEY_SOURCE_VERSION = "sourceVersion";

    /** The key for the target version. */
    public static final String KEY_TARGET_VERSION = "targetVersion";

    /** The key for the generate debug info flag. */
    public static final String KEY_GENERATE_DEBUG_INFO = "generateDebugInfo";

    public static final String VERSION_RUNTIME = null;
    public static final String VERSION_1_1 = "1.1";
    public static final String VERSION_1_2 = "1.2";
    public static final String VERSION_1_3 = "1.3";
    public static final String VERSION_1_4 = "1.4";
    public static final String VERSION_1_5 = "1.5";
    public static final String VERSION_1_6 = "1.6";
    public static final String VERSION_1_7 = "1.7";
    public static final String VERSION_1_8 = "1.8";

    /** The key for the class loader writer.
     * By default the registered class loader writer service is used. */
    public static final String KEY_CLASS_LOADER_WRITER = "classLoaderWriter";

    /**
     * The key for the class loader.
     * By default the commons dynamic classloader is used.
     * This property overrides the classloader and ignores the
     * {@link #KEY_ADDITIONAL_CLASS_LOADER} completly!
     */
    public static final String KEY_CLASS_LOADER = "classLoader";

    /**
     * The key for the additional class loader.
     * By default the commons dynamic classloader is used.
     * If this property is used and the {@link #KEY_CLASS_LOADER}
     * property is not defined, a classloader with the dynamic
     * class loader (default) and the class loader specified here
     * is used.
     */
    public static final String KEY_ADDITIONAL_CLASS_LOADER = "classLoader";

    /** The key to force the compilation - even if the class files are more recent.
     * The value should be of type Boolean. */
    public static final String KEY_FORCE_COMPILATION = "forceCompilation";

    /** The key to ignore warnings - if this option is turned on, the
     * resulting compilation result does not get the warnings issued
     * by the compiler.
     * The value should be of type Boolean. */
    public static final String KEY_IGNORE_WARNINGS = "ignoreWarnings";

    /**
     * Default options with the following presets:
     * - generate debug info : true
     */
    public Options() {
        this.put(KEY_GENERATE_DEBUG_INFO, true);
    }

    /**
     * Create a new options object based on an existing one.
     */
    public Options(final Options options) {
        super(options);
    }

    public String getSourceVersion() {
        return (String) this.get(KEY_SOURCE_VERSION);
    }

    /**
     * @since 2.0
     */
    public String getTargetVersion() {
        return (String) this.get(KEY_TARGET_VERSION);
    }

    public boolean isGenerateDebugInfo() {
        if ( this.get(KEY_GENERATE_DEBUG_INFO) != null ) {
            return (Boolean) this.get(KEY_GENERATE_DEBUG_INFO);
        }
        return false;
    }
}
