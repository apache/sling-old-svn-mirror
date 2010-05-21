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

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import java.util.ArrayList;

/**
 * This is the interface to be implemented by the class responsible for handling the GWT RPC calls.
 * <p/>
 * It defines the API for creating, deleting and retrieving notes.
 */
@RemoteServiceRelativePath("notesservice")
public interface NotesService extends RemoteService {

    /**
     * This method is called to create a <code>Note</code>. The note will be persisted into the repository.
     *
     * @param note The <code>Note</code> to be created and stored in the repository.
     * @return The <code>String</code> representing the status message of the successful RPC operation.
     */
    public String createNote(Note note);

    /**
     * This method is called to retrieve an <code>ArrayList</code> of all notes stored in the repository.
     *
     * @return The <code>ArrayList</code> containing all <code>Note</code>s stored on the server.
     */
    public ArrayList<Note> getNotes();

    /**
     * This method is called to delete a <code>Note</code> from the repository. To identify the <code>javax.jcr.Node</code>
     * associated with the <code>Note</code>-POJO, its repository path has to be provided.
     *
     * @param path The <code>String</code> representing the path of the <code>javax.jcr.Node</code> this note is associated with.
     * @return The <code>String</code> representing the status message of the successful RPC operation.
     */
    String deleteNote(String path);
}
