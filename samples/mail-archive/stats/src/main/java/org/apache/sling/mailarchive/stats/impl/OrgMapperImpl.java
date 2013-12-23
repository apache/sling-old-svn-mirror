package org.apache.sling.mailarchive.stats.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.mailarchive.stats.OrgMapper;

@Component
@Service
public class OrgMapperImpl implements OrgMapper {

    @Override
    public String mapToOrg(String email) {
        if(email.contains("@")) {
            return email.split("@")[1];
        }
        return email;
    }
}
