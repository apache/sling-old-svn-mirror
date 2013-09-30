package org.apache.sling.ide.serialization;

/**
 * The <tt>SerializationKind</tt> lists the various rules for serializing repository content on disk
 * 
 */
public enum SerializationKind {
    FILE, FOLDER, METADATA_PARTIAL, METADATA_FULL;
}