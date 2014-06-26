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
    private final String fileName;
    private final SerializationKind serializationKind;
    private final String folderPath;

    public static SerializationData empty(String folderPath, SerializationKind serializationKind) {
        return new SerializationData(folderPath, null, null, serializationKind);
    }

    public SerializationData(String folderPath, String fileName, byte[] contents,
            SerializationKind serializationKind) {
        this.folderPath = folderPath;
        this.contents = contents;
        this.fileName = fileName;
        this.serializationKind = serializationKind;
    }

    /**
     * @return the path where the serialization data file should be stores, in OS format
     */
    public String getFolderPath() {
        return folderPath;
    }

    /**
     * @return the contents of the serialization data file
     */
    public byte[] getContents() {
        return contents;
    }

    /**
     * 
     * @return the name of the serialization data file
     */
    public String getFileName() {
        return fileName;
    }

    public SerializationKind getSerializationKind() {
        return serializationKind;
    }

    public boolean hasContents() {

        return contents != null && contents.length > 0;
    }

    @Override
    public String toString() {
        return "[SerializationData# folderPath: " + folderPath + ", fileName: " + fileName
                + ", serializationKind: " + serializationKind + ", hasContents: " + (hasContents()) + "]";
    }
}