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

import java.util.ArrayList;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * This is the asynchronous service interface as required for GWT RPC operations.
 *
 * @see org.apache.sling.extensions.gwt.sample.service.NotesService
 * @see org.apache.sling.extensions.gwt.sample.client.Notes
 * @see org.apache.sling.extensions.gwt.sample.server.NotesServiceImpl
 */
public interface NotesServiceAsync {

    void createNote(Note note, AsyncCallback<String> async);

    void getNotes(AsyncCallback<ArrayList<Note>> async);

    void deleteNote(String path, AsyncCallback<String> async);
}
