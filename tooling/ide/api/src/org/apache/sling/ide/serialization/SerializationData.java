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
package org.apache.sling.ide.serialization;

public class SerializationData {

    private final byte[] contents;
    private final String nameHint;
    private final SerializationKind serializationKind;
    private final String fileOrFolderNameHint;

    public static SerializationData empty(String fileOrFolderNameHint, SerializationKind serializationKind) {
        return new SerializationData(fileOrFolderNameHint, null, null, serializationKind);
    }

    public SerializationData(String fileOrFolderNameHint, String nameHint, byte[] contents,
            SerializationKind serializationKind) {
        this.fileOrFolderNameHint = fileOrFolderNameHint;
        this.contents = contents;
        this.nameHint = nameHint;
        this.serializationKind = serializationKind;
    }

    public String getFileOrFolderNameHint() {
        return fileOrFolderNameHint;
    }

    public byte[] getContents() {
        return contents;
    }

    public String getNameHint() {
        return nameHint;
    }

    public SerializationKind getSerializationKind() {
        return serializationKind;
    }

    public boolean hasContents() {

        return contents != null && contents.length > 0;
    }

    @Override
    public String toString() {
        return "[SerializationData# fileOrFolderNameHint: " + fileOrFolderNameHint + ", nameHint: " + nameHint
                + ", serializationKind: " + serializationKind + ", contents?" + (hasContents()) + "]";
    }
}