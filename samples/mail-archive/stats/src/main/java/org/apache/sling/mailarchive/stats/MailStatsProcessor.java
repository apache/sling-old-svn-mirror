package org.apache.sling.mailarchive.stats;

import java.util.Date;

/** Compute and store stats */
public interface MailStatsProcessor {
    /**
     * @param date Message timestamp
     * @param from "From "address of the message
     * @param to Optional "to" addresses
     * @param cc Option "Cc" addresses
     */
    void computeStats(Date d, String from, String [] to, String [] cc);
    
    /** Flush in-memory data to permanent storage */
    void flush();
}
