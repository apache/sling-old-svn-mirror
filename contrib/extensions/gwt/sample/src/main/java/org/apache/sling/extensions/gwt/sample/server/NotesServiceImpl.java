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
package org.apache.sling.extensions.gwt.sample.server;

import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.extensions.gwt.sample.service.Note;
import org.apache.sling.extensions.gwt.sample.service.NotesService;
import org.apache.sling.extensions.gwt.user.server.rpc.SlingRemoteServiceServlet;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a servlet-based RPC remote service for handling RPC calls from the GWT client application.
 * <p/>
 * It registers as an OSGi service and component, under the <code>javax.servlet.Servlet</code> interface. It thus
 * acts as a servlet, registered under the path specified  by the <code>sling.servlet.paths</code> scr property.
 * The path under which the servlet is registered must correspond to the GWT module's base url.
 * <p/>
 * The NotesServiceImpl class handles the creation, retrieval and deletion of {@link Note}s, as POJOs and as
 * <code>javax.jcr.Node</code>s in the repository.
 * <p/>
 * The class is an implementation of the <code>SlingRemoteServiceServlet</code> and is as such able to handle
 * GWT RPC calls in a Sling environment. The servlet must be registered with the GWT client application in the
 * <code>Notes.gwt.xml</code> module configuration file.
 *
 */
@Component
@Service(value=javax.servlet.Servlet.class)
@Property(name="sling.servlet.paths", value="/gwt/org.apache.sling.extensions.gwt.sample.Notes/notesservice")
public class NotesServiceImpl extends SlingRemoteServiceServlet implements NotesService {

    /**
     * The logging facility.
     */
    private static final Logger log = LoggerFactory.getLogger(NotesServiceImpl.class);

    /**
     * The <code>String</code> constant representing the name of the <code>javax.jcr.Property</code> in which the
     * title of a note is stored.
     */
    private static final String PN_NOTETITLE = "noteTitle";

    /**
     * The <code>String</code> constant representing the name of the <code>javax.jcr.Property</code> in which the
     * text of a note is stored.
     */
    private static final String PN_NOTETEXT = "noteText";

    /**
     * The <code>String</code> constant representing the name of the path in the repository under which the notes are
     * stored.
     */
    private static final String PATH_DEMOCONTENT = "/gwt/demo/notes";

    /**
     * This is the <code>SlingRepository</code> as provided by the Sling environment. It is used for repository
     * access/operations.
     *
     */
    @Reference
    private SlingRepository repository;

    /**
     * This is the <code>javax.jcr.Session</code> used for repository operations. It is retrieved from the repository
     * through an administrative login.
     */
    private Session session;

    /**
     * The <code>javax.jcr.Node</code> representing the root of the demo content. See {@link NotesServiceImpl#repository}.
     */
    private Node root;

    /**
     * This is the OSGi component/service activation method. It initializes this service.
     *
     * @param context The OSGi context provided by the activator.
     */
    protected void activate(ComponentContext context) {
        /**
         * GWT normally uses Thread.getCurrentThread().getContextClassLoader() as its class loader. This is illegal
         * in the OSGi environment, as GWT then cannot access the service implementation classes of this bundle
         * during an RPC call. As such we have explicitly hand over our bundle's class loader to GWT. For this purpose
         * this class extends <code>SlingRemoteServiceServlet</code> instead of only GWT's <code>RemoteServiceServlet</code>.
         * The <code>SlingRemoteServiceServlet</code> has been extended to set a correct classloader and to provide
         * resources via bundles.
         */
        super.setClassLoader(this.getClass().getClassLoader());
        super.setBundle(context.getBundleContext().getBundle());
        log.info("activate: initialized and provided classloader {} to GWT.", this.getClass().getClassLoader());

        try {
            // retrieve a session from the repository
            session = repository.loginAdministrative(repository.getDefaultWorkspace());
        } catch (RepositoryException e) {
            log.error("activate: repository unavailable: " + context + ": ", e);
        }

        try {
            // get the demo content root node from the session
            root = (Node) session.getItem(PATH_DEMOCONTENT);
        } catch (RepositoryException e) {
            log.error("activate: error while getting demo content path " + PATH_DEMOCONTENT + ": "
                    + session.getWorkspace().getName() + ": ", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String createNote(Note note) {

        String message = "The note was successfully created!";

        log.info("createNote: creating note with title {}...", note.getTitle());
        if (root != null) {
            try {

                // add a node to the root of the demo content and set the properties via the POJO's getters
                final Node node = root.addNode("note");
                node.setProperty(PN_NOTETITLE, note.getTitle());
                node.setProperty(PN_NOTETEXT, note.getText());
                root.save();
                log.info("createNote: successfully saved note {} to repository.", node.getPath());

            } catch (RepositoryException e) {

                log.error("createNote: error while creating note in repository: ", e);
                message = "Failed to create note! Error: " + e.getMessage();
                try {
                    // if the node creation failed, try to roll-back the repository changes accumulated
                    if (session.hasPendingChanges()) {
                        session.refresh(false);
                    }
                } catch (RepositoryException e1) {
                    log.error("createNote: error while reverting changes after trying to save note to repository: ", e1);
                    message = "Failed to create note! Error: " + e1.getMessage();
                }
            }

        } else {
            log.error("createNote: cannot create note, demo content path {} unavailable!", PATH_DEMOCONTENT);
            message = "Failed to create note! Error: demo content path unavailable";
        }

        return message;
    }

    /**
     * {@inheritDoc}
     */
    public ArrayList<Note> getNotes() {

        final ArrayList<Note> notes = new ArrayList<Note>();

        if (root != null) {
            try {

                // get all child nodes of the demo content root node
                final NodeIterator nodes = root.getNodes();
                while (nodes.hasNext()) {

                    Node node = nodes.nextNode();
                    // for every child node, that has the correct properties set, create a POJO and add it to the list
                    if (node.hasProperty(PN_NOTETITLE) && node.hasProperty(PN_NOTETEXT)) {

                        final Note note = new Note();
                        note.setTitle(node.getProperty(PN_NOTETITLE).getString());
                        note.setText(node.getProperty(PN_NOTETEXT).getString());
                        // set the node's path, so that the GWT client app can use it as the parameter for the
                        // deleteNote(String path) method.
                        note.setPath(node.getPath());

                        notes.add(note);
                        log.info("getNotes: found note {}, adding to list...", node.getPath());
                    }
                }

            } catch (RepositoryException e) {
                log.error("getNotes: error while getting list of notes from " + PATH_DEMOCONTENT + ": ", e);
            }

        } else {
            log.error("getNotes: error while getting notes, demo content path {} unavailable!", PATH_DEMOCONTENT);
        }

        return notes;
    }

    /**
     * {@inheritDoc}
     */
    public String deleteNote(String path) {

        String message = "The note was successfully deleted!";

        try {
            session.getItem(path).remove();
            session.save();
        } catch (RepositoryException e) {
            log.error("deleteNote: error while deleting note {}: ", e);
            message = "Failed to delete to note! Error: " + e.getMessage();
        }

        return message;
    }
}
