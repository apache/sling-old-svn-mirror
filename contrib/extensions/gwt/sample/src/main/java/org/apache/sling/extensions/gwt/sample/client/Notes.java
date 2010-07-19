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
package org.apache.sling.extensions.gwt.sample.client;

import java.util.ArrayList;

import org.apache.sling.extensions.gwt.sample.service.Note;
import org.apache.sling.extensions.gwt.sample.service.NotesService;
import org.apache.sling.extensions.gwt.sample.service.NotesServiceAsync;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * This class is the starting point for the Sling GWT Sample, a GWT client application that enables the user
 * to store notes on the server and read them through the GWT widgets.
 * <p/>
 * The <code>Notes</code> class represents the entry point and top level widget of the GWT client application.
 */
public class Notes implements EntryPoint {

    private static final NotesServiceAsync service = (NotesServiceAsync) GWT.create(NotesService.class);

    final TextBox inputNoteTitle = new TextBox();
    final TextArea inputNoteText = new TextArea();
    final VerticalPanel notesPanel = new VerticalPanel();

    {
        inputNoteTitle.setStyleName("formField");
        inputNoteText.setStyleName("formField");
        inputNoteText.setVisibleLines(3);
    }

    /**
     * This method is called when GWT loads the Notes application (as defined in the Notes.gwt.xml).
     */
    public void onModuleLoad() {

        final HorizontalPanel mainpanel = new HorizontalPanel();

        final HTML displayTitle = new HTML("Existing Notes");
        displayTitle.setStyleName("displayTitle");
        final HTML entryTitle = new HTML("Create A Note");
        entryTitle.setStyleName("entryTitle");

        final VerticalPanel displayPanel = new VerticalPanel();
        displayPanel.setVerticalAlignment(VerticalPanel.ALIGN_TOP);
        displayPanel.setStyleName("displayPanel");
        displayPanel.add(displayTitle);

        final VerticalPanel entryPanel = new VerticalPanel();
        entryPanel.setVerticalAlignment(VerticalPanel.ALIGN_TOP);
        entryPanel.setStyleName("entryPanel");
        entryPanel.add(entryTitle);

        displayPanel.add(notesPanel);

        final VerticalPanel form = createForm();
        entryPanel.add(form);

        mainpanel.add(displayPanel);
        mainpanel.add(entryPanel);

        RootPanel.get("notes").add(mainpanel);

        getNotes();
    }

    private VerticalPanel createForm() {

        final VerticalPanel form = new VerticalPanel();
        form.setStyleName("formPanel");

        final HorizontalPanel titleLine = new HorizontalPanel();
        final HTML textNoteTitle = new HTML("Title: ");
        textNoteTitle.setWidth("50px");
        titleLine.add(textNoteTitle);
        titleLine.add(inputNoteTitle);

        final HorizontalPanel textLine = new HorizontalPanel();
        final HTML textNoteText = new HTML("Note: ");
        textNoteText.setWidth("50px");
        textLine.add(textNoteText);
        textLine.add(inputNoteText);

        form.add(titleLine);
        form.add(textLine);
        form.add(createButtons());

        return form;
    }

    private Panel createButtons() {

        final HorizontalPanel panel = new HorizontalPanel();

        Button save = new Button("Save");
        save.setStyleName("button");
        save.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                if (validateFormInput()) {
                    createNote(inputNoteTitle.getText(), inputNoteText.getText());
                    resetForm();
                }

            }
        });
        Button clear = new Button("Clear");
        save.setStyleName("button");
        clear.addClickHandler(new ClickHandler() {

            public void onClick(ClickEvent event) {
                resetForm();
            }
        });

        panel.add(save);
        panel.add(clear);

        return panel;
    }

    private boolean validateFormInput() {

        if (inputNoteTitle.getText().trim().length() == 0) {
            Window.alert("Please enter a title for the new note!");
            inputNoteTitle.setFocus(true);
            return false;
        }
        if (inputNoteText.getText().trim().length() == 0) {
            Window.alert("Please enter a text for the new note!");
            inputNoteText.setFocus(true);
            return false;
        }
        return true;
    }

    private void resetForm() {

        inputNoteTitle.setText("");
        inputNoteText.setText("");
    }

    private void createNote(String title, String text) {

        final Note note = new Note();
        note.setTitle(title);
        note.setText(text);

        service.createNote(note, new AsyncCallback<String>() {

            public void onFailure(Throwable throwable) {
                Window.alert("Failed to created note: " + throwable.getMessage());
            }

            public void onSuccess(String o) {
                getNotes();
            }
        });
    }

    private void deleteNote(String path) {
        service.deleteNote(path, new AsyncCallback<String>() {

            public void onFailure(Throwable throwable) {
                Window.alert("Failed to delete note: " + throwable.getMessage());
            }

            public void onSuccess(String o) {
                getNotes();
            }
        });
    }

    private void getNotes() {

        notesPanel.clear();

        service.getNotes(new AsyncCallback<ArrayList<Note>>() {
            public void onFailure(Throwable throwable) {
                notesPanel.add(new HTML("No notes stored so far."));
                Window.alert("Could not retrieve notes: " + throwable.getMessage());
            }

            public void onSuccess(ArrayList<Note> notesList) {
                for (int i = 0; i < notesList.size(); i++) {
                    final Note note = (Note) notesList.get(i);

                    final HorizontalPanel noteEntry = new HorizontalPanel();
                    noteEntry.setStyleName("noteEntry");

                    final HTML noteTitle = new HTML(note.getTitle());
                    noteTitle.setStyleName("noteTitle");

                    final HTML noteText = new HTML(note.getText());
                    noteText.setStyleName("noteText");

                    final Button delButton = new Button("Delete");
                    delButton.setStyleName("noteControls");
                    delButton.addClickHandler(new ClickHandler() {

                        public void onClick(ClickEvent event) {
                            deleteNote(note.getPath());
                        }
                    });
                    noteEntry.add(noteTitle);
                    noteEntry.add(noteText);
                    noteEntry.add(delButton);

                    notesPanel.add(noteEntry);
                }

                if (notesList.size() == 0) {
                    notesPanel.add(new HTML("No notes stored so far."));
                }
            }
        });
    }
}
