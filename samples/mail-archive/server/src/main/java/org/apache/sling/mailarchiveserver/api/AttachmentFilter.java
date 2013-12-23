package org.apache.sling.mailarchiveserver.api;

import org.apache.james.mime4j.message.BodyPart;

public interface AttachmentFilter {
    
    boolean isEligible(BodyPart attachment);
    
}
