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
package org.apache.sling.samples.espblog.internal;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.observation.ObservationManager;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Observe the espblog content for changes, and generate
 * thumbnails when images are added.
 *
 */
@Component(immediate=true)
@Property(name="service.description", value="Sling ESP blog sample thumbnails generator")
public class ThumbnailGenerator implements EventListener {

    private Session session;
    private ObservationManager observationManager;

	@Reference
	private SlingRepository repository;

    @Property(value="/content/espblog")
    private static final String CONTENT_PATH_PROPERTY = "content.path";

	private static final Logger log = LoggerFactory
			.getLogger(ThumbnailGenerator.class);

	private Map<String, String> supportedMimeTypes = new HashMap<String, String>();

	protected void activate(ComponentContext context)  throws Exception {
	    supportedMimeTypes.put("image/jpeg", ".jpg");
        supportedMimeTypes.put("image/png", ".png");

        String contentPath = (String)context.getProperties().get(CONTENT_PATH_PROPERTY);

		session = repository.loginAdministrative(null);
		if (repository.getDescriptor(Repository.OPTION_OBSERVATION_SUPPORTED).equals("true")) {
			observationManager = session.getWorkspace().getObservationManager();
			String[] types = { "nt:file" };
			observationManager.addEventListener(this, Event.NODE_ADDED, contentPath, true, null, types, false);
		}
	}

    protected void deactivate(ComponentContext componentContext) throws RepositoryException {
        if(observationManager != null) {
            observationManager.removeEventListener(this);
        }
        if (session != null) {
            session.logout();
            session = null;
        }
    }

	public void onEvent(EventIterator it) {
        while (it.hasNext()) {
            Event event = it.nextEvent();
            try {
                if (event.getType() == Event.NODE_ADDED && !(event.getPath().contains("thumbnails"))) {
                    log.info("new upload: {}", event.getPath());
                    Node addedNode = session.getRootNode().getNode(event.getPath().substring(1));
                    processNewNode(addedNode);
                    log.info("finished processing of {}", event.getPath());
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private String getMimeType(Node n) throws RepositoryException {
	    String result = null;
		final String mimeType = n.getProperty("jcr:mimeType").getString();

		for(String key : supportedMimeTypes.keySet()) {
		    if(mimeType!=null && mimeType.startsWith(key)) {
		        result = key;
		        break;
		    }
		}

		if(result == null) {
			log.info("Node {} rejected, unsupported mime-type {}", n.getPath(), mimeType);
		}

		if(n.getName().startsWith(".")) {
			log.info("Node {} rejected, name starts with '.'", n.getPath(), mimeType);
			result = null;
		}

		return result;
	}

	private void processNewNode(Node addedNode) throws Exception {
	    final String mimeType = getMimeType(addedNode);
		if (mimeType == null) {
			return;
		}
		final String suffix = supportedMimeTypes.get(mimeType);

		// Scale to a temp file for simplicity
		log.info("Creating thumbnails for node {}", addedNode.getPath());
		final int [] widths = { 50, 100, 250 };
		for(int width : widths) {
		    createThumbnail(addedNode, width, mimeType, suffix);
		}
	}

	private void createThumbnail(Node image, int scalePercent, String mimeType, String suffix) throws Exception {
        final File tmp = File.createTempFile(getClass().getSimpleName(), suffix);
        try {
            scale(image.getProperty("jcr:data").getStream(), scalePercent, new FileOutputStream(tmp), suffix);

            // Create thumbnail node and set the mandatory properties
            Node thumbnailFolder = getThumbnailFolder(image);
            Node thumbnail = thumbnailFolder.addNode(image.getParent().getName() + "_" + scalePercent + suffix, "nt:file");
            Node contentNode = thumbnail.addNode("jcr:content", "nt:resource");
            contentNode.setProperty("jcr:data", new FileInputStream(tmp));
            contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
            contentNode.setProperty("jcr:mimeType", mimeType);

            session.save();

            log.info("Created thumbnail " + contentNode.getPath());
        } finally {
            if(tmp != null) {
                tmp.delete();
            }
        }

	}

	private Node getThumbnailFolder(Node addedNode) throws Exception {
		Node post = addedNode.getParent().getParent().getParent();
		if (post.hasNode("thumbnails")) {
			log.info("thumbnails node exists already at " + post.getPath());
			return post.getNode("thumbnails");
		} else {
			Node t = post.addNode("thumbnails", "nt:folder");
			session.save();
			return t;
		}
	}

	public void scale(InputStream inputStream, int width, OutputStream outputStream, String suffix) throws IOException {
		if(inputStream == null) {
			throw new IOException("InputStream is null");
		}

        final BufferedImage src = ImageIO.read(inputStream);
		if(src == null) {
		    final StringBuffer sb = new StringBuffer();
		    for(String fmt : ImageIO.getReaderFormatNames()) {
		        sb.append(fmt);
		        sb.append(' ');
		    }
			throw new IOException("Unable to read image, registered formats: " + sb);
		}

        final double scale = (double)width / src.getWidth();

		int destWidth = width;
		int destHeight = new Double(src.getHeight() * scale).intValue();
		log.debug("Generating thumbnail, w={}, h={}", destWidth, destHeight);
		BufferedImage dest = new BufferedImage(destWidth, destHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = dest.createGraphics();
		AffineTransform at = AffineTransform.getScaleInstance((double) destWidth / src.getWidth(), (double) destHeight	/ src.getHeight());
		g.drawRenderedImage(src, at);
		ImageIO.write(dest, suffix.substring(1), outputStream);
	}
}