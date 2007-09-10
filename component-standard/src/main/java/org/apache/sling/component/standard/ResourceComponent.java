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
package org.apache.sling.component.standard;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.sling.RequestUtil;
import org.apache.sling.component.ComponentRequest;
import org.apache.sling.component.ComponentResponse;
import org.apache.sling.component.Content;
import org.apache.sling.components.BaseComponent;

/**
 * The <code>ResourceComponent</code> TODO
 *
 * @scr.component immediate="true" metatype="false"
 * @scr.property name="service.description"
 *          value="Component to handle nt:resource content"
 * @scr.property name="service.vendor" value="The Apache Software Foundation"
 * @scr.service 
 */
public class ResourceComponent extends BaseComponent {

    public static final String ID = ResourceComponent.class.getName();
    
    /**
     * The name of the header used to send the last modification date of the
     * resource (value is "Last-Modified").
     */
    private static final String LAST_MODIFIED = "Last-Modified";
    
    /**
     * The name of the header checked for a conditional modification date to
     * compare to the resource's last modification date (value is
     * "If-Modified-Since").
     */
    private static final String IF_MODIFIED_SINCE = "If-Modified-Since";

    {
        setContentClassName(ResourceContent.class.getName());
        setComponentId(ID);
    }

    /* (non-Javadoc)
     * @see com.day.components.Component#createContentInstance()
     */
    public Content createContentInstance() {
        return new ResourceContent();
    }

    // nothing to do
    protected void doInit() {}
    
    /*
     * (non-Javadoc)
     * @see com.day.components.Component#service(com.day.components.ComponentRequest, com.day.components.ComponentResponse)
     */
    public void service(ComponentRequest request, ComponentResponse response)
            throws IOException {
        
        ResourceContent content = (ResourceContent) request.getContent();

        // check the last modification time and If-Modified-Since header
        long modifTime = content.getLastModificationTime();
        if (unmodified(request, modifTime)) {
            
            response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
            
        } else {
            
            response.setContentType(content.getMimeType());
            response.setHeader(LAST_MODIFIED, RequestUtil.toDateString(modifTime));
         
            OutputStream out = response.getOutputStream();
            InputStream ins = null;
            try {
                ins = content.getValue().getStream();
                IOUtils.copy(ins, out);
            } catch (RepositoryException re) {
                throw (IOException) new IOException("Cannot get content from"
                    + content.getPath()).initCause(re);
            } finally {
                IOUtils.closeQuietly(ins);
            }
        }
        
    }    
    
    /**
     * Returns <code>true</code> if the request has a
     * <code>If-Modified-Since</code> header whose date value is later than
     * the last modification time given as <code>modifTime</code>.
     * 
     * @param request The <code>ComponentRequest</code> checked for the
     *            <code>If-Modified-Since</code> header.
     * @param modifTime The last modification time to compare the header to.
     * @return <code>true</code> if the <code>modifTime</code> is less than
     *         or equal to the time of the <code>If-Modified-Since</code>
     *         header.
     */
    private boolean unmodified(ComponentRequest request, long modifTime) {
        String par = request.getHeader(IF_MODIFIED_SINCE);
        long ims = RequestUtil.toDateValue(par);
        return modifTime <= ims;
    }
}
