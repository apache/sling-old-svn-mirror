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