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
package org.apache.sling.atom.taglib;

import javax.servlet.jsp.tagext.BodyTagSupport;

import org.apache.abdera.Abdera;
import org.apache.abdera.ext.media.MediaExtensionFactory;
import org.apache.abdera.ext.opensearch.model.OpenSearchExtensionFactory;
import org.apache.abdera.factory.Factory;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.apache.abdera.parser.stax.FOMFactory;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.scripting.jsp.util.TagUtil;

public class AbstractAbderaHandler extends BodyTagSupport {

    private static final long serialVersionUID = 1L;

    public AbstractAbderaHandler() {
        super();
    }

    protected Abdera getAbdera() {
        final SlingHttpServletRequest request = TagUtil.getRequest(pageContext);
        return getAbdera(request);
    }

    protected Abdera getAbdera(SlingHttpServletRequest request) {
        Abdera abdera;
        if (request.getAttribute("abdera") != null
            && (request.getAttribute("abdera") instanceof Abdera)) {
            abdera = (Abdera) request.getAttribute("abdera");
        } else {

            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            ClassLoader osgiClassloader = getClass().getClassLoader();

            Thread.currentThread().setContextClassLoader(osgiClassloader);

            abdera = new Abdera();
            new FOMFactory();
            Factory f = abdera.getFactory();
            if (f instanceof FOMFactory) {
                FOMFactory ff = (FOMFactory) f;
                // I know this sucks, but due to the OSGi-fication Abdera does
                // not pick up extension factories automatically.
                ff.registerExtension(new MediaExtensionFactory());
                ff.registerExtension(new OpenSearchExtensionFactory());
            }

            Thread.currentThread().setContextClassLoader(classLoader);

            request.setAttribute("abdera", abdera);
        }
        return abdera;
    }

    protected Feed getFeed(SlingHttpServletRequest request) {
        Feed feed;
        if (request.getAttribute("feed") != null
            && (request.getAttribute("feed") instanceof Feed)) {
            feed = (Feed) request.getAttribute("feed");
        } else {
            feed = getAbdera().newFeed();
            request.setAttribute("feed", feed);
        }
        return feed;
    }

    protected Feed getFeed() {
        final SlingHttpServletRequest request = TagUtil.getRequest(pageContext);
        return getFeed(request);
    }

    protected Entry getEntry() {
        final SlingHttpServletRequest request = TagUtil.getRequest(pageContext);
        if (request.getAttribute("entry") instanceof Entry) {
            return (Entry) request.getAttribute("entry");
        }
        return null;
    }

    protected void setEntry(Entry entry) {
        final SlingHttpServletRequest request = TagUtil.getRequest(pageContext);
        request.setAttribute("entry", entry);
    }

    protected void clearEntry() {
        setEntry(null);
    }

    protected boolean hasEntry() {
        return getEntry() != null;
    }

}