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
package org.apache.sling.tooling.lc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.sling.maven.projectsupport.BundleListUtils;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.Bundle;
import org.apache.sling.maven.projectsupport.bundlelist.v1_0_0.BundleList;
import org.apache.sling.provisioning.model.Artifact;
import org.apache.sling.provisioning.model.Model;
import org.apache.sling.provisioning.model.ModelUtility;
import org.apache.sling.provisioning.model.io.ModelReader;
import org.apache.sling.tooling.lc.aether.AetherSetup;
import org.apache.sling.tooling.lc.aether.ArtifactKey;
import org.apache.sling.tooling.lc.aether.Artifacts;
import org.apache.sling.tooling.lc.aether.VersionChange;
import org.apache.sling.tooling.lc.jira.IssueFinder;
import org.apache.sling.tooling.lc.svn.SvnChangeLogFinder;
import org.tmatesoft.svn.core.SVNException;

import com.google.common.collect.Sets;

public class LaunchpadComparer {
    
    private static final Pattern JIRA_KEY_PATTERN = Pattern.compile("^(SLING-\\d+).*");

    private final String firstVersion;
    private final String secondVersion;

    public LaunchpadComparer(String firstVersion, String secondVersion) {
        this.firstVersion = firstVersion;
        this.secondVersion = secondVersion;
    }

    public void run() throws Exception {
        
        System.out.format("Computing differences between Launchpad versions %s and %s...%n", 
                firstVersion, secondVersion);
        
        // 1. download artifacts
        AetherSetup aether = new AetherSetup();

        File fromFile = aether.download(Artifacts.launchpadCoordinates(firstVersion));
        File toFile = aether.download(Artifacts.launchpadCoordinates(secondVersion));

        // 2. parse artifact definitions
        Model model;
        try (BufferedReader reader = Files.newBufferedReader(toFile.toPath())) {
            model = ModelUtility.getEffectiveModel(ModelReader.read(reader, null));
        }
        
        Map<ArtifactKey, Artifact> to = model.getFeatures().stream()
            .flatMap( f -> f.getRunModes().stream())
            .flatMap( r -> r.getArtifactGroups().stream())
            .flatMap( g -> StreamSupport.stream(g.spliterator(), false))
            .collect(Collectors.toMap( a -> new ArtifactKey(a), Function.identity()));
        
        BundleList readBundleList = BundleListUtils.readBundleList(fromFile);
        
        Map<ArtifactKey, Artifact> from = readBundleList.getStartLevels().stream()
            .flatMap( sl -> sl.getBundles().stream() )
            .collect(Collectors.toMap( b -> new ArtifactKey(b), LaunchpadComparer::newArtifact));

        // 3. generate added / removed / changed
        Set<Artifact> removed = Sets.difference(from.keySet(), to.keySet()).stream()
            .map( k -> from.get(k))
            .collect(Collectors.toSet());

        Set<Artifact> added = Sets.difference(to.keySet(), from.keySet()).stream()
                .map( k -> to.get(k))
                .collect(Collectors.toSet());

        Map<ArtifactKey, VersionChange> changed = to.values().stream()
                .filter( k -> !added.contains(k) && !removed.contains(k))
                .map( k -> new ArtifactKey(k))
                .filter( k -> !Objects.equals(to.get(k).getVersion(), from.get(k).getVersion()))
                .collect(Collectors.toMap( Function.identity(), k -> new VersionChange(from.get(k).getVersion(), to.get(k).getVersion())));

        // 4. output changes
        
        System.out.println("Added ");
        added.stream().sorted().forEach(LaunchpadComparer::outputFormatted);
        
        System.out.println("Removed ");
        removed.stream().sorted().forEach(LaunchpadComparer::outputFormatted);
        
        System.out.println("Changed");
        changed.entrySet().stream()
            .sorted( (a, b) -> a.getKey().compareTo(b.getKey()) )
            .forEach(LaunchpadComparer::outputFormatted);        
        
    }
    
    private static Artifact newArtifact(Bundle bundle) {
        
        return new Artifact(bundle.getGroupId(), bundle.getArtifactId(), bundle.getVersion(), bundle.getClassifier(), bundle.getType());
    }
    
    private static void outputFormatted(Artifact a) {
        
        System.out.format("    %-30s : %-55s : %s%n", a.getGroupId(), a.getArtifactId(), a.getVersion());
        
    }

    private static void outputFormatted(Map.Entry<ArtifactKey, VersionChange> e) {
        
        ArtifactKey artifact = e.getKey();
        VersionChange versionChange = e.getValue();
        
        System.out.format("    %-30s : %-55s : %s -> %s%n", artifact.getGroupId(), artifact.getArtifactId(), versionChange.getFrom(), versionChange.getTo());
        
        if ( !artifact.getGroupId().equals("org.apache.sling"))  {
            return;
        }
        
        SvnChangeLogFinder svn = new SvnChangeLogFinder();
        
        String fromTag = artifact.getArtifactId()+"-"+versionChange.getFrom();
        String toTag = artifact.getArtifactId()+"-"+ versionChange.getTo();
        try {
            List<String> issues = svn.getChanges(fromTag, toTag)
                .stream()
                .map(LaunchpadComparer::toJiraKey)
                .filter( k -> k != null)
                .collect(Collectors.toList());
            
            IssueFinder issueFinder = new IssueFinder();
            issueFinder.findIssues(issues).
                forEach( i -> System.out.format("        %-10s - %s%n", i.getKey(), i.getSummary()));
            
        } catch (SVNException | IOException e1) {
            System.err.println("Failed retrieving changes : " + e1.getMessage());
        }
    }
    
    private static String toJiraKey(String message) {
        Matcher matcher = JIRA_KEY_PATTERN.matcher(message);
        if ( !matcher.matches() ) {
            return null;
        }
        
        return matcher.group(1);
    }    
}
