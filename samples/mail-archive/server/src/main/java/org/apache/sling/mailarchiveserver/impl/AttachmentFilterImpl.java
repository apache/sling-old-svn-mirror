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
package org.apache.sling.mailarchiveserver.impl;

import java.io.IOException;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.james.mime4j.dom.BinaryBody;
import org.apache.james.mime4j.dom.Body;
import org.apache.james.mime4j.dom.TextBody;
import org.apache.james.mime4j.message.BodyPart;
import org.apache.sling.mailarchiveserver.api.AttachmentFilter;

@Component
@Service(AttachmentFilter.class)
public class AttachmentFilterImpl implements AttachmentFilter {

    private Set<String> eligibleExtensions = null;
    private long maxSize = (long) 5e6; // 5 Mb

    @Override
    public boolean isEligible(BodyPart attachment) {
        // extension check
        final String filename = attachment.getFilename();
        String ext = "";
        int idx = filename.lastIndexOf('.');
        if (idx > -1) {
            ext = filename.substring(idx + 1);
        }
        if (eligibleExtensions != null && !eligibleExtensions.contains(ext)) {
            return false;
        }
        
        // size check
        final Body body = attachment.getBody();
        try {
            if (
                    body instanceof BinaryBody 
                    && IOUtils.toByteArray(((BinaryBody) body).getInputStream()).length > maxSize
                    || 
                    body instanceof TextBody
                    && IOUtils.toByteArray(((TextBody) body).getInputStream()).length > maxSize ) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        // true, if nothing wrong
        return true;
    }

}
