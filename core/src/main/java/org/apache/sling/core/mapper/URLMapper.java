/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.core.mapper;

import javax.jcr.Session;

import org.apache.sling.component.Content;
import org.apache.sling.core.impl.RequestData;


/**
 * The <code>URLMapper</code> interface is implemented by classes
 * configured in the &lt;mapper> element of the <code>DeliveryModule</code>
 * configuration.
 */
public interface URLMapper {

    /**
     * Creates a MappedURL object from the original uri.
     * <p>
     * This method also does:
     * <ul>
     * <li>Resolve the item addressed by the request
     * <li>Resolve the Component used for action handling and rendering
     * <li>Resolve the Content from the addressed request item
     * <li>Checking (read) access to the addressed item by the session
     * </ul>
     *
     * @param RequestData additional request data, which may be updated by
     *      this method.
     * @return The {@link MappedURL} object after the correct mapping of the
     *         URI or <code>null</code> the no such page exists or the ticket
     *         has insufficient access rights to resolve the url.
     */
    public Content getMappedURL(RequestData requestData);

    /**
     * Maps a ContentBus handle to an URI, which when handled by
     * {@link #getMappedURL} returns the original handle. This method may be
     * used to get the external (URI) representation of a ContentBus handle,
     * which is guaranteed to map back to the same handle, when used in a
     * request.
     *
     * @param handle The ContentBus handle to map to an URI
     *
     * @return The external (URI) representation of the ContentBus handle.
     */
    public String handleToURL(String handle);

    /**
     * Maps a ContentBus handle to an URI, which when handled by
     * {@link #getMappedURL} returns the original handle. This method may be
     * used to get the external (URI) representation of a ContentBus handle,
     * which is guaranteed to map back to the same handle, when used in a
     * request.
     *
     * @param prefix A prefix to prepend to the URI
     * @param handle The ContentBus handle to map to an URI
     * @param suffix A suffix to append to the URI
     *
     * @return The external (URI) representation of the ContentBus handle.
     */
    public String handleToURL(String prefix, String handle, String suffix);

    /**
     * Translates a href from the contentbus to a delivery path. the href can
     * have this format:
     * <xmp>
     * /gur/fasel-foo.bar/lustig.Par.0001.Img0.gif/fake.jpg?foo=true
     * </xmp>
     * <p>
     * Externalizes a handle to a common URL, that looks 'best' from outside.
     * When mapping from external URLs to internal handles, the following
     * components are respected: <br>
     * - the webapp context (by the servlet engine) <br>
     * - the fakeurls<br>
     * - the url-mapping settings<br>
     * <br>
     * so for example a request to:<br>
     * /cq3/playground/en.html resuts the handle: /content/playground/en<br>
     *
     * so this method should turn an internal href into the same original
     * href again. it does the follwoing:<br>
     * - respects the url mapping<br>
     * - prepends the webapp context<br>
     * - appends missing suffixes<br>
     *
     * @param ticket the ticket
     * @param prefix a prefix to prepend to the href (webapp context)
     * @param href an internal href
     * @param base the base handle for relative links
     * @param suffix the default suffix to add
     *
     * @return A information object of the performed translation or
     *         <code>null</code> if the node cannot be accessed, if the CSD of
     * 	       the node is not valid, the handle does not address a page and
     * 	       no UUID is contained in the href,
     */
    public URLTranslationInfo externalizeHref(Session session, String prefix,
		String href, String base, String suffix);

}
