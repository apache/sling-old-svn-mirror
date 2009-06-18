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
package org.apache.sling.samples.webloader.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;

import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.samples.webloader.WebloaderException;
import org.apache.sling.samples.webloader.WebloaderJobStatus;
import org.apache.sling.jcr.api.SlingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A Webloader job, manages retrieval of documents from the web and storage
 *  in the Sling repository. This code is based on the "populate.jsp" example
 *  of the jackrabbit-webapp module.
 */ 
class WebloaderJob extends Thread implements WebloaderJobStatus {
    private Throwable error;
    private int numDocsLoaded;
    private final int maxDocsToRetrieve;
    private final int maxDocSize;
    private String statusInfo = "initialized";
    private String statusDetails = "";
    private boolean running = true;
    private final String jobId;
    private final String webQuery;
    private String storagePath;
    private final SlingRepository repository;
    private final MimeTypeService mimeTypeService;
    private Session session;
    private Node storageRoot;
    private static int idCounter;
    private final String [] filetypes;
    
    public static final String [] DEFAULT_FILETYPES = { "pdf", "rtf", "ppt", "doc", "xls" };
    public static final int URL_RETRIEVE_TIMEOUT_SECONDS = 10;
    
    private static final Logger log = LoggerFactory.getLogger(WebloaderJob.class);
    
    @SuppressWarnings("serial")
    static class DocTooBigException extends IOException {
        DocTooBigException(URL url, int size) {
            super("Document at URL " + url + " too big (" + size + " bytes), will be ignored");
        }
    }
    
    WebloaderJob(SlingRepository repository, MimeTypeService mimeTypeService, 
            String webQuery, String storagePath, String fileExtensions, int maxDocsToRetrieve, int maxDocSize) {
        synchronized (WebloaderJob.class) {
            jobId = String.valueOf(idCounter++);
        }
        
        this.repository = repository;
        this.mimeTypeService = mimeTypeService;
        this.webQuery = webQuery;
        this.storagePath = storagePath;
        this.maxDocsToRetrieve = maxDocsToRetrieve;
        this.maxDocSize = maxDocSize;
        
        final String [] ft = fileExtensions == null ? null : fileExtensions.split(",");
        if(ft!=null && ft.length > 0) {
            filetypes = new String[ft.length];
            for(int i=0; i < ft.length; i++) {
                filetypes[i] = ft[i].trim().toLowerCase();
            }
        } else {
            filetypes = DEFAULT_FILETYPES;
        }
        
        if(mimeTypeService == null) {
            throw new WebloaderException("Missing MimeTypeService");
        }
        if(repository == null) {
            throw new WebloaderException("Missing Repository");
        }
        
        setDaemon(true);
        start();
    }
    
    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        for(String str : filetypes) {
            if(sb.length() > 0) {
                sb.append(",");
            }
            sb.append(str);
        }
        
        return getClass().getSimpleName() + ", webQuery=" + webQuery 
            + ", storagePath=" + storagePath
            + ", fileTypes=" + sb.toString()
            + ", maxDocsToRetrieve=" + maxDocsToRetrieve
            + ", maxDocSize=" + maxDocSize
        ;
    }
    
    @Override
    public void run() {
        log.debug("Job thread starting: {}", this);
        
        // TODO should use a session provided by client, but can we use it
        // safely for our async job?
        session = null;
        
        if(storagePath.charAt(0) == '/') {
            storagePath = storagePath.substring(1);
        }
        final String absStoragePath = "/" + storagePath;
        
        try {
            session = repository.loginAdministrative(null);
            if(session.itemExists(absStoragePath)) {
                final Item i = session.getItem(absStoragePath);
                if(i.isNode()) {
                    storageRoot = (Node)i;
                } else {
                    throw new WebloaderException("Item at " + storagePath + " is not a Node");
                }
            } else {
                // TODO deep-create hierarchy if needed
                storageRoot = session.getRootNode().addNode(storagePath);
                session.save();
            }
            
            int offset = 0;
            for(String type : filetypes) {
                final URL[] urls = getDocumentUrlsFromGoogle(type, offset);
                for(URL url : urls) {
                    try {
                        getAndStoreDocument(url);
                        session.save();
                        numDocsLoaded++;
                        if(numDocsLoaded >= maxDocsToRetrieve) {
                            break;
                        }
                    } catch(DocTooBigException dtb) {
                        log.info(dtb.getMessage());
                    } catch(Exception e) {
                        log.warn("Exception while retrieving url " + url, e);
                    } finally {
                        session.refresh(false);
                    }
                }
                offset += 10;
                
                if(numDocsLoaded >= maxDocsToRetrieve) {
                    break;
                }
            }
            
            statusInfo = "All done.";
            
        } catch(Exception e) {
            error = e;
            log.warn("Exception in WebloaderJob.run()", e);
            statusInfo = "Exception while running job: " + e;
            
        } finally {
            if(session != null) {
                session.logout();
            }
            statusDetails = "";
            running = false;
        }
        
        if(numDocsLoaded >= maxDocsToRetrieve) {
            log.info("Stopped after retrieving maximum number of documents ({})", maxDocsToRetrieve);
        }
        
        log.info("Job thread ends: {}, {} documents loaded", this, numDocsLoaded);
    }
    
    private URL [] getDocumentUrlsFromGoogle(String currentFiletype, int start) throws IOException, BadLocationException {
        final List urls = new ArrayList();
        String query = webQuery + " filetype:" + currentFiletype;
        final URL google = new URL("http://www.google.com/search?q=" +
                URLEncoder.encode(query, "UTF-8") + "&start=" + start);
        log.debug("Querying {}", google.toString());
        statusInfo = "Querying " + google.toString();
        statusDetails = "";
        URLConnection con = google.openConnection();
        con.setRequestProperty("User-Agent", "");
        InputStream in = con.getInputStream();
        try {
            HTMLEditorKit kit = new HTMLEditorKit();
            HTMLDocument doc = new HTMLDocument();
            doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
            kit.read(new InputStreamReader(in, "UTF-8"), doc, 0);
            HTMLDocument.Iterator it = doc.getIterator(HTML.Tag.A);
            while (it.isValid()) {
                if(it.getAttributes() != null) {
                    String href = (String) it.getAttributes().getAttribute(HTML.Attribute.HREF);
                    if (href != null && href.endsWith("." + currentFiletype)) {
                        URL url = new URL(new URL("http", "www.google.com", "dummy"), href);
                        if (url.getHost().indexOf("google") == -1) {
                            log.debug("Got document URL from google: {}", url);
                            statusDetails = "Got URL " + url;
                            urls.add(url);
                        }
                    }
                }
                it.next();
            }
        } finally {
            in.close();
        }
        return (URL[]) urls.toArray(new URL[urls.size()]);

    }
    
    private void getAndStoreDocument(URL currentURL) throws RepositoryException, IOException {

        statusInfo = "Retrieving document " + currentURL;
        statusDetails = "";
        
        // build JCR path for storing document, based on its URL
        String path = currentURL.getPath();
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        final String host = currentURL.getHost();
        final List folderNames = new ArrayList();
        folderNames.addAll(Arrays.asList(host.split("\\.")));
        Collections.reverse(folderNames);
        folderNames.addAll(Arrays.asList(path.split("/", 0)));
        final String filename = URLDecoder.decode((String) folderNames.remove(folderNames.size() - 1), "UTF-8").replaceAll(":", "_");
        Node node = storageRoot;
        for (Iterator fn = folderNames.iterator(); fn.hasNext(); ) {
            String name = URLDecoder.decode((String) fn.next(), "UTF-8");
            name = name.replaceAll(":", "_");
            if (name.length() == 0) {
                continue;
            }
            if (!node.hasNode(name)) {
                node.addNode(name, "nt:folder");
            }
            node = node.getNode(name);
        }
        
        log.debug("Retrieving document {}, will be stored at {}", currentURL, node.getPath() + "/" + filename);
        
        if (!node.hasNode(filename)) {
            Node file = node.addNode(filename, "nt:file");
            final Node resource = file.addNode("jcr:content", "nt:resource");
            getAndStoreContent(currentURL, resource, filename);
        }

    }
    
    private void getAndStoreContent(URL currentURL, Node resource, String filename) 
    throws RepositoryException, IOException {
        statusInfo = "Retrieving content from " + currentURL;
        statusDetails = "";
        
        final URLConnection con = currentURL.openConnection();
        con.setReadTimeout(URL_RETRIEVE_TIMEOUT_SECONDS * 1000);
        InputStream in = con.getInputStream();
        try {
            // Read with a ProgressInputStream, so that our status is updated while
            // downloading
            int length = con.getContentLength();
            if (length != -1) {
                if(length > maxDocSize * 1024) {
                    throw new DocTooBigException(currentURL, length);
                }
                in = new ProgressInputStream(in, length) {
                    int nextReport = 0;
                    protected void reportProgress(int bytesRead, int totalBytesToRead) {
                        if(bytesRead > nextReport) {
                            nextReport += 1024;
                            statusDetails = "Downloaded " + bytesRead + " bytes out of " + totalBytesToRead;
                        }
                    }
                };
            }
            
            resource.setProperty("jcr:data", in);
            final String mimeType = mimeTypeService.getMimeType(filename);
            resource.setProperty("jcr:mimeType", mimeType);
            final Calendar lastModified = Calendar.getInstance();
            lastModified.setTimeInMillis(con.getLastModified());
            resource.setProperty("jcr:lastModified", lastModified);
        } finally {
            if(in != null) {
                in.close();
            }
        }
    }
    
    String getJobId() {
        return jobId;
    }
    
    /** {@inheritDoc} */
    public Throwable getError() {
        return error;
    }

    /** {@inheritDoc} */
    public int getNumberOfDocumentsLoaded() {
        return numDocsLoaded;
    }

    /** {@inheritDoc} */
    public String getStatusInfo() {
        return statusInfo;
    }

    /** {@inheritDoc} */
    public String getStatusDetails() {
        return statusDetails;
    }

    /** {@inheritDoc} */
    public boolean isRunning() {
        return running;
    }

}
