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
