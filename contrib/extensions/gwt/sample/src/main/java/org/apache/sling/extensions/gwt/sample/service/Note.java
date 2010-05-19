/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sling.extensions.gwt.sample.service;

import java.io.Serializable;

/**
 * This class represents a serializable POJO of a note taken with the application. The POJO is produced by the
 * {@link org.apache.sling.extensions.gwt.sample.server.NotesServiceImpl}.
 * <p/>
 * <p/>
 * The <code>Note</code> class features simple getters and setters for its data.
 */
public class Note implements Serializable {

    /**
     * The String representing the title of the note.
     */
    private String title;

    /**
     * The String representing the text of the note.
     */
    private String text;

    /**
     * The String representing the path of the <code>javax.jcr.Node</code> that this POJO is based on.
     */
    private String path;


    /**
     * The default public constructor.
     */
    public Note() {
    }

    /**
     * The setter method for the <code>String</code> representing the title of the note. The setter is used by
     * the {@link NotesService#createNote(Note)} and {@link NotesService#getNotes()} methods.
     *
     * @param title The <code>String</code> representing the title of the note.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * The setter method for the <code>String</code> representing the text of the note. The setter is used by
     * the {@link NotesService#createNote(Note)} and {@link NotesService#getNotes()} methods.
     *
     * @param text The <code>String</code> representing the text of the note.
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * The setter method for the <code>String</code> representing the path of the note. The setter is used by
     * the {@link NotesService#getNotes()} method.
     *
     * @param path The <code>String</code> representing the path of the <code>javax.jcr.Node</code> corresponding
     *             to this POJO.
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * The getter method for the <code>String</code> representing the title of the note.
     *
     * @return The <code>String</code> representing the title of the note.
     */
    public String getTitle() {
        return title;
    }

    /**
     * The getter method for the <code>String</code> representing the text of the note.
     *
     * @return The <code>String</code> representing the text of the note.
     */
    public String getText() {
        return text;
    }

    /**
     * The getter method for the <code>String</code> representing the path of the <code>javax.jcr.Node</code>
     * corresponding to this POJO.
     *
     * @return The <code>String</code> representing the path of the note.
     */
    public String getPath() {
        return path;
    }
}
