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
package org.apache.sling.provisioning.model;

import static org.apache.sling.provisioning.model.ModelResolveUtility.getProcessedConfiguration;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Utility for merging two models.
 *
 * @since 1.4
 */
public abstract class MergeUtility {

    /**
     * Options for specifying some parts of the merge operation.
     */
    public static class MergeOptions {

        private boolean handleRemoveRunMode = true;

        private boolean latestArtifactWins = true;

        /**
         * Returns {@code true} if the remove run mode should be respected.
         * @return {@code true} or {@code false}
         */
        public boolean isHandleRemoveRunMode() {
            return handleRemoveRunMode;
        }

        /**
         * Set to {@code true} if the remove run mode should be respected.
         * @param handleRemoveRunMode Whether the remove run mode should be respected.
         * @return This instance.
         */
        public MergeOptions setHandleRemoveRunMode(boolean handleRemoveRunMode) {
            this.handleRemoveRunMode = handleRemoveRunMode;
            return this;
        }

        /**
         * Returns {@code true} if the latest artifact should win on a merge.
         * @return {@code true} or {@code false} if the artifact with the
         *         highest version should win
         */
        public boolean isLatestArtifactWins() {
            return latestArtifactWins;
        }

        /**
         * Set to {@code true} if the latest artifact should win on a merge.
         * Set to {@code false} if the artifact with the highest version should win
         * @param latestArtifactWins Whether the latest artifact should win
         * @return This instance.
         */
        public MergeOptions setLatestArtifactWins(boolean latestArtifactWins) {
            this.latestArtifactWins = latestArtifactWins;
            return this;
        }
    }

    /**
     * Merge the additional model into the base model.
     * @param base The base model.
     * @param additional The additional model.
     */
    public static void merge(final Model base, final Model additional) {
        merge(base, additional, new MergeOptions());
    }

    /**
     * Merge the additional model into the base model.
     * <p>
     * Merging is performed feature by feature. Each feature is treated separately.
     * If the base model does not have a feature from the additional model, the complete
     * feature is added. If the base model has a feature which is not in the additional model,
     * the feature is left as is.
     * <p>
     * For each feature, the following actions are performed:
     * <ul>
     *   <li>If either the base feature or the additional feature has a version, then
     *    the one with the higher version is used, the other one is skipped. A missing
     *    version is considered the lowest possible version.</li>
     *   <li>The feature type of the base feature is set to the type of the additional feature.</li>
     *   <li>All additional sections of the additional feature are added to the base feature.</li>
     *   <li>All variables from the additional feature are set on the base feature, overriding
     *    values if already present.</li>
     *   <li>Each run mode of the additional feature is merged into the base feature.</li>
     * </ul>
     * <p>
     * @param base The base model.
     * @param additional The additional model.
     * @param options The merge options
     */
    public static void merge(final Model base, final Model additional, final MergeOptions options) {
        // features
        for(final Feature feature : additional.getFeatures()) {
            final Feature baseFeature = base.getOrCreateFeature(feature.getName());

            // version check first
            boolean overwrite = false;
            if ( baseFeature.getVersion() != null ) {
                if ( feature.getVersion() == null ) {
                    continue;
                }
                final Version baseVersion = new Version(baseFeature.getVersion());
                final Version addVersion = new Version(feature.getVersion());
                if ( baseVersion.compareTo(addVersion) >= 0 ) {
                    continue;
                }
                overwrite = true;
            } else {
                if ( feature.getVersion() != null ) {
                    overwrite = true;
                }
            }
            if ( overwrite ) {
                // set version
                baseFeature.setVersion(feature.getVersion());
                // remove everything from base feature
                baseFeature.getRunModes().clear();
                baseFeature.getAdditionalSections().clear();
                baseFeature.getVariables().clear();
                baseFeature.setComment(null);
            }

            mergeComments(baseFeature, feature);
            baseFeature.setType(feature.getType());

            // additional sections (sections are not cloned, therefore comments do not need to be merged)
            baseFeature.getAdditionalSections().addAll(feature.getAdditionalSections());

            // variables
            baseFeature.getVariables().putAll(feature.getVariables());
            mergeComments(baseFeature.getVariables(), feature.getVariables());

            // run modes
            for(final RunMode runMode : feature.getRunModes()) {
                // check for special remove run mode
                final String names[] = runMode.getNames();
                if ( options.isHandleRemoveRunMode() && names != null ) {
                    if ( handleRemoveRunMode(baseFeature, runMode) ) {
                        continue;
                    }
                }
                final RunMode baseRunMode = baseFeature.getOrCreateRunMode(names);

                // artifact groups
                for(final ArtifactGroup group : runMode.getArtifactGroups()) {
                    final ArtifactGroup baseGroup = baseRunMode.getOrCreateArtifactGroup(group.getStartLevel());

                    mergeComments(baseGroup, group);

                    int foundStartLevel = 0;

                    for(final Artifact artifact : group) {
                        boolean addArtifact = true;
                        for(final ArtifactGroup searchGroup : baseRunMode.getArtifactGroups()) {
                            final Artifact found = searchGroup.search(artifact);
                            if ( found != null ) {
                                if ( options.isLatestArtifactWins() ) {
                                    searchGroup.remove(found);
                                    foundStartLevel = searchGroup.getStartLevel();
                                } else {
                                    try {
                                        final Version baseVersion = new Version(found.getVersion());
                                        final Version mergeVersion = new Version(artifact.getVersion());
                                        if ( baseVersion.compareTo(mergeVersion) <= 0 ) {
                                            searchGroup.remove(found);
                                            foundStartLevel = searchGroup.getStartLevel();
                                        } else {
                                            addArtifact = false;
                                        }
                                    } catch ( final IllegalArgumentException iae) {
                                        // if at least one version is not a valid maven version
                                        if ( found.getVersion().compareTo(artifact.getVersion()) <= 0 ) {
                                            searchGroup.remove(found);
                                            foundStartLevel = searchGroup.getStartLevel();
                                        } else {
                                            addArtifact = false;
                                        }
                                    }
                                }
                            }
                        }
                        if ( addArtifact ) {
                            // artifacts are not cloned, therefore comments do not need to be merged
                            if ( group.getStartLevel() == 0 && foundStartLevel != 0 ) {
                                baseRunMode.getOrCreateArtifactGroup(foundStartLevel).add(artifact);
                            } else {
                                baseGroup.add(artifact);
                            }
                        }
                    }
                }

                // configurations
                for(final Configuration config : runMode.getConfigurations()) {
                    final Configuration found = baseRunMode.getOrCreateConfiguration(config.getPid(), config.getFactoryPid());

                    mergeConfiguration(found, config);
                    mergeComments(found, config);
                }

                // settings
                for(final Map.Entry<String, String> entry : runMode.getSettings() ) {
                    baseRunMode.getSettings().put(entry.getKey(), entry.getValue());
                }
                mergeComments(baseRunMode.getSettings(), runMode.getSettings());
            }

        }
    }

    /**
     * Handle the remove run mode
     * @param baseFeature The base feature
     * @param runMode The current run mode
     * @return {@code true} if the current run mode is a remove run mode
     */
    private static boolean handleRemoveRunMode(final Feature baseFeature, final RunMode runMode) {
        String names[] = runMode.getNames();
        int removeIndex = -1;
        int index = 0;
        for(final String name : names) {
            if ( name.equals(ModelConstants.RUN_MODE_REMOVE) ) {
                removeIndex = index;
                break;
            }
            index++;
        }
        if ( removeIndex != -1 ) {
            String[] newNames = null;
            if ( names.length > 1 ) {
                newNames = new String[names.length - 1];
                index = 0;
                for(final String name : names) {
                    if ( !name.equals(ModelConstants.RUN_MODE_REMOVE) ) {
                        newNames[index++] = name;
                    }
                }
            }
            names = newNames;
            final RunMode baseRunMode = baseFeature.getRunMode(names);
            if ( baseRunMode != null ) {

                // artifact groups
                for(final ArtifactGroup group : runMode.getArtifactGroups()) {
                    for(final Artifact artifact : group) {
                        for(final ArtifactGroup searchGroup : baseRunMode.getArtifactGroups()) {
                            final Artifact found = searchGroup.search(artifact);
                            if ( found != null ) {
                                searchGroup.remove(found);
                            }
                        }
                    }
                }

                // configurations
                for(final Configuration config : runMode.getConfigurations()) {
                    final Configuration found = baseRunMode.getConfiguration(config.getPid(), config.getFactoryPid());
                    if ( found != null ) {
                        baseRunMode.getConfigurations().remove(found);
                    }
                }

                // settings
                for(final Map.Entry<String, String> entry : runMode.getSettings() ) {
                    baseRunMode.getSettings().remove(entry.getKey());
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Merge two configurations
     * @param baseConfig The base configuration.
     * @param mergeConfig The merge configuration.
     */
    private static void mergeConfiguration(final Configuration baseConfig, final Configuration mergeConfig) {
        // check for merge mode
        final boolean isNew = baseConfig.getProperties().isEmpty();
        if ( isNew ) {
            copyConfigurationProperties(baseConfig, mergeConfig);
            final Object mode = mergeConfig.getProperties().get(ModelConstants.CFG_UNPROCESSED_MODE);
            if ( mode != null ) {
                baseConfig.getProperties().put(ModelConstants.CFG_UNPROCESSED_MODE, mode);
            }
        } else {
            final boolean baseIsRaw = baseConfig.getProperties().get(ModelConstants.CFG_UNPROCESSED) != null;
            final boolean mergeIsRaw = mergeConfig.getProperties().get(ModelConstants.CFG_UNPROCESSED) != null;
            // simplest case, both are raw
            if ( baseIsRaw && mergeIsRaw ) {
                final String cfgMode = (String)mergeConfig.getProperties().get(ModelConstants.CFG_UNPROCESSED_MODE);
                if ( cfgMode == null || ModelConstants.CFG_MODE_OVERWRITE.equals(cfgMode) ) {
                    copyConfigurationProperties(baseConfig, mergeConfig);
                } else {
                    final Configuration newConfig = new Configuration(baseConfig.getPid(), baseConfig.getFactoryPid());
                    getProcessedConfiguration(null, newConfig, baseConfig, false, null);
                    clearConfiguration(baseConfig);
                    copyConfigurationProperties(baseConfig, newConfig);

                    clearConfiguration(newConfig);
                    getProcessedConfiguration(null, newConfig, mergeConfig, false, null);

                    if ( baseConfig.isSpecial() ) {
                        final String baseValue = baseConfig.getProperties().get(baseConfig.getPid()).toString();
                        final String mergeValue = newConfig.getProperties().get(baseConfig.getPid()).toString();
                        baseConfig.getProperties().put(baseConfig.getPid(), baseValue + "\n" + mergeValue);
                    } else {
                        copyConfigurationProperties(baseConfig, newConfig);
                    }
                }

            // another simple case, both are not raw
            } else if ( !baseIsRaw && !mergeIsRaw ) {
                // merge mode is always overwrite
                clearConfiguration(baseConfig);
                copyConfigurationProperties(baseConfig, mergeConfig);

            // base is not raw but merge is
            } else if ( !baseIsRaw && mergeIsRaw ) {
                final String cfgMode = (String)mergeConfig.getProperties().get(ModelConstants.CFG_UNPROCESSED_MODE);
                if ( cfgMode == null || ModelConstants.CFG_MODE_OVERWRITE.equals(cfgMode) ) {
                    clearConfiguration(baseConfig);
                    copyConfigurationProperties(baseConfig, mergeConfig);
                } else {
                    final Configuration newMergeConfig = new Configuration(mergeConfig.getPid(), mergeConfig.getFactoryPid());
                    getProcessedConfiguration(null, newMergeConfig, mergeConfig, false, null);

                    if ( baseConfig.isSpecial() ) {
                        final String baseValue = baseConfig.getProperties().get(baseConfig.getPid()).toString();
                        final String mergeValue = newMergeConfig.getProperties().get(baseConfig.getPid()).toString();
                        baseConfig.getProperties().put(baseConfig.getPid(), baseValue + "\n" + mergeValue);
                    } else {
                        copyConfigurationProperties(baseConfig, newMergeConfig);
                    }
                }

                // base is raw, but merge is not raw
            } else {
                // merge mode is always overwrite
                clearConfiguration(baseConfig);
                copyConfigurationProperties(baseConfig, mergeConfig);
            }
        }
    }

    private static void clearConfiguration(final Configuration cfg) {
        final Set<String> keys = new HashSet<String>();
        final Enumeration<String> e = cfg.getProperties().keys();
        while ( e.hasMoreElements() ) {
            keys.add(e.nextElement());
        }

        for(final String key : keys) {
            cfg.getProperties().remove(key);
        }
    }

    private static void copyConfigurationProperties(final Configuration baseConfig, final Configuration mergeConfig) {
        final Enumeration<String> e = mergeConfig.getProperties().keys();
        while ( e.hasMoreElements() ) {
            final String key = e.nextElement();
            if ( !key.equals(ModelConstants.CFG_UNPROCESSED_MODE) ) {
                baseConfig.getProperties().put(key, mergeConfig.getProperties().get(key));
            }
        }
    }

    /**
     * Merge the comments
     * @param base The base model object
     * @param additional The additional model object
     * @since 1.9.0
     */
    public static void mergeComments(final Commentable base, final Commentable additional) {
        if ( base.getComment() == null ) {
            base.setComment(additional.getComment());
        } else if ( additional.getComment() != null ) {
            base.setComment(base.getComment() + "\n" + additional.getComment());
        }
    }
}
