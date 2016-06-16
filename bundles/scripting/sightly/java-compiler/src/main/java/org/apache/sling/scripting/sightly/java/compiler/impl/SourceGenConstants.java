/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package org.apache.sling.scripting.sightly.java.compiler.impl;

/**
 * Names related to Java source generation.
 */
public final class SourceGenConstants {

    public static final String OUT_BUFFER = "out";

    public static final String WRITE_METHOD = "write";

    public static final String COLLECTION_TYPE = "Collection";

    public static final String BINDINGS_FIELD = "bindings";

    public static final String BINDINGS_GET_METHOD = "get";

    public static final String START_MAP_METHOD = "obj";

    public static final String MAP_TYPE_ADD = "with";

    public static final String CALL_UNIT_METHOD = "callUnit";

    public static final String RENDER_CONTEXT_INSTANCE = "renderContext";
    public static final String RENDER_CONTEXT_GET_OBJECT_MODEL = "getObjectModel";
    public static final String RUNTIME_OBJECT_MODEL = "RuntimeObjectModel";
    public static final String ROM_RESOLVE_PROPERTY = "resolveProperty";
    public static final String ROM_TO_BOOLEAN = "toBoolean";
    public static final String ROM_TO_COLLECTION = "toCollection";
    public static final String ROM_TO_MAP = "toMap";
    public static final String ROM_TO_STRING = "toString";
    public static final String ROM_TO_NUMBER = "toNumber";

    public static final String RUNTIME_CALL_METHOD = "call";

    public static final String COLLECTION_LENGTH_METHOD = "size";

    public static final String MAP_GET = "get";
    public static final String TRIM_METHOD = "trim";
    public static final String STRING_EMPTY = "isEmpty";
    public static final String RECORD_GET_VALUE = "getProperty";
    public static final String ARGUMENTS_FIELD = "arguments";
}
