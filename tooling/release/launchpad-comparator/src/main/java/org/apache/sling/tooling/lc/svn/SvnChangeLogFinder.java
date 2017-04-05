/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.sling.tooling.lc.svn;

import java.util.ArrayList;
import java.util.List;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;

public class SvnChangeLogFinder {
    
    private static final String SLING_SVN_REPO_BASE = "https://svn.apache.org/repos/asf/sling";
    
    public static void main(String[] args) throws SVNException {
        
        new SvnChangeLogFinder().getChanges("org.apache.sling.adapter-2.1.2", "org.apache.sling.adapter-2.1.6")
            .stream().forEach(System.out::println);
    }
    
    public List<String> getChanges(String first, String second) throws SVNException {
        
        SVNURL svnUrl = SVNURL.parseURIEncoded(SLING_SVN_REPO_BASE);
        
        List<String> changes = new ArrayList<>();
        
        SVNClientManager manager  = SVNClientManager.newInstance();
        
        SVNRepository repo = manager.getRepositoryPool().createRepository(svnUrl, true);
        
        SVNRevision from = SVNRevision.create(getRevision(first, repo));
        SVNRevision to = SVNRevision.create(getRevision(second, repo));
        
        repo.log(new String[] { "tags/" + second } ,from.getNumber(), to.getNumber(), false, false, (e) -> changes.add(e.getMessage()));
        
        return changes;
    }


    private long getRevision(String tagName, SVNRepository repo) throws SVNException {
        
        return repo.info("tags/" + tagName, -1).getRevision();
    }

}
