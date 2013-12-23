package org.apache.sling.mailarchive.stats;

/** Maps an email address to an organization name */
public interface OrgMapper {
    String mapToOrg(String email); 
}
