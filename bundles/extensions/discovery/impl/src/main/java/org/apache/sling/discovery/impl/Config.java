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
package org.apache.sling.discovery.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration object used as a central config point for the discovery service
 * implementation
 * <p>
 * The properties are described below under.
 */
@Component(metatype = true, label="%config.name", description="%config.description")
@Service(value = { Config.class })
public class Config {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** resource used to keep instance information such as last heartbeat, properties, incoming announcements **/
    private static final String CLUSTERINSTANCES_RESOURCE = "clusterInstances";

    /** resource used to keep the currently established view **/
    private static final String ESTABLISHED_VIEW_RESOURCE = "establishedView";

    /** resource used to keep the previously established view **/
    private static final String PREVIOUS_VIEW_RESOURCE = "previousView";

    /** resource used to keep ongoing votings **/
    private static final String ONGOING_VOTING_RESOURCE = "ongoingVotings";

    /** Configure the timeout (in seconds) after which an instance is considered dead/crashed. */
    public static final long DEFAULT_HEARTBEAT_TIMEOUT = 120;
    @Property(longValue=DEFAULT_HEARTBEAT_TIMEOUT)
    public static final String HEARTBEAT_TIMEOUT_KEY = "heartbeatTimeout";
    private long heartbeatTimeout = DEFAULT_HEARTBEAT_TIMEOUT;

    /** Configure the interval (in seconds) according to which the heartbeats are exchanged in the topology. */
    public static final long DEFAULT_HEARTBEAT_INTERVAL = 30;
    @Property(longValue=DEFAULT_HEARTBEAT_INTERVAL)
    public static final String HEARTBEAT_INTERVAL_KEY = "heartbeatInterval";
    private long heartbeatInterval = DEFAULT_HEARTBEAT_INTERVAL;

    /** Configure the time (in seconds) which must be passed at minimum between sending TOPOLOGY_CHANGING/_CHANGED (avoid flooding). */
    public static final int DEFAULT_MIN_EVENT_DELAY = 3;
    @Property(intValue=DEFAULT_MIN_EVENT_DELAY)
    public static final String MIN_EVENT_DELAY_KEY = "minEventDelay";
    private int minEventDelay = DEFAULT_MIN_EVENT_DELAY;

    /** Configure the socket connect timeout for topology connectors. */
    public static final int DEFAULT_CONNECTION_TIMEOUT = 10;
    @Property(intValue=DEFAULT_CONNECTION_TIMEOUT)
    public static final String CONNECTION_TIMEOUT_KEY = "connectionTimeout";
    private int connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;

    /** Configure the socket read timeout (SO_TIMEOUT) for topology connectors. */
    public static final int DEFAULT_SO_TIMEOUT = 10;
    @Property(intValue=DEFAULT_SO_TIMEOUT)
    public static final String SO_TIMEOUT_KEY = "soTimeout";
    private int soTimeout = DEFAULT_SO_TIMEOUT;

    /** Name of the repository descriptor to be taken into account for leader election:
        those instances have preference to become leader which have the corresponding descriptor value of 'false' */
    @Property
    public static final String LEADER_ELECTION_REPOSITORY_DESCRIPTOR_NAME_KEY = "leaderElectionRepositoryDescriptor";
    
    /** URLs where to join a topology, eg http://localhost:4502/libs/sling/topology/connector */
    @Property(cardinality=1024)
    public static final String TOPOLOGY_CONNECTOR_URLS_KEY = "topologyConnectorUrls";
    private URL[] topologyConnectorUrls = {null};

    /** list of ips and/or hostnames which are allowed to connect to /libs/sling/topology/connector */
    private static final String[] DEFAULT_TOPOLOGY_CONNECTOR_WHITELIST = {"localhost","127.0.0.1"};
    @Property(value={"localhost","127.0.0.1"})
    public static final String TOPOLOGY_CONNECTOR_WHITELIST_KEY = "topologyConnectorWhitelist";
    private String[] topologyConnectorWhitelist = DEFAULT_TOPOLOGY_CONNECTOR_WHITELIST;

    /** Path of resource where to keep discovery information, e.g /var/discovery/impl/ */
    private static final String DEFAULT_DISCOVERY_RESOURCE_PATH = "/var/discovery/impl/";
    @Property(value=DEFAULT_DISCOVERY_RESOURCE_PATH, propertyPrivate=true)
    public static final String DISCOVERY_RESOURCE_PATH_KEY = "discoveryResourcePath";
    private String discoveryResourcePath = DEFAULT_DISCOVERY_RESOURCE_PATH;

    /**
     * If set to true, local-loops of topology connectors are automatically stopped when detected so.
     */
    @Property(boolValue=false)
    private static final String AUTO_STOP_LOCAL_LOOP_ENABLED = "autoStopLocalLoopEnabled";

    /**
     * If set to true, request body will be gzipped - only works if counter-part accepts gzip-requests!
     */
    @Property(boolValue=false)
    private static final String GZIP_CONNECTOR_REQUESTS_ENABLED = "gzipConnectorRequestsEnabled";

    /**
     * If set to true, hmac is enabled and the white list is disabled.
     */
    @Property(boolValue=false)
    private static final String HMAC_ENABLED = "hmacEnabled";

    /**
     * If set to true, and the whitelist is disabled, messages will be encrypted.
     */
    @Property(boolValue=false)
    private static final String ENCRYPTION_ENABLED = "enableEncryption";

    /**
     * The value fo the shared key, shared amongst all instances in the same cluster.
     */
    @Property
    private static final String SHARED_KEY = "sharedKey";

    /**
     * The default lifetime of a HMAC shared key in ms. (4h)
     */
    private static final long DEFAULT_SHARED_KEY_INTERVAL = 3600*1000*4;

    @Property(longValue=DEFAULT_SHARED_KEY_INTERVAL)
    private static final String SHARED_KEY_INTERVAL = "hmacSharedKeyTTL";
    
    /**
     * The property for defining the backoff factor for standby (loop) connectors
     */
    @Property
    private static final String BACKOFF_STANDBY_FACTOR = "backoffStandbyFactor";
    private static final int DEFAULT_BACKOFF_STANDBY_FACTOR = 5;
    
    /**
     * The property for defining the maximum backoff factor for stable connectors
     */
    @Property
    private static final String BACKOFF_STABLE_FACTOR = "backoffStableFactor";
    private static final int DEFAULT_BACKOFF_STABLE_FACTOR = 5;

    private String leaderElectionRepositoryDescriptor ;

    /** True when auto-stop of a local-loop is enabled. Default is false. **/
    private boolean autoStopLocalLoopEnabled;
    
    /**
     * True when the hmac is enabled and signing is disabled.
     */
    private boolean hmacEnabled;

    /**
     * the shared key.
     */
    private String sharedKey;

    /**
     * The key interval.
     */
    private long keyInterval;

    /**
     * true when encryption is enabled.
     */
    private boolean encryptionEnabled;
    
    /**
     * true when topology connector requests should be gzipped
     */
    private boolean gzipConnectorRequestsEnabled;

    /** the backoff factor to be used for standby (loop) connectors **/
    private int backoffStandbyFactor = DEFAULT_BACKOFF_STANDBY_FACTOR;
    
    /** the maximum backoff factor to be used for stable connectors **/
    private int backoffStableFactor = DEFAULT_BACKOFF_STABLE_FACTOR;
    
    @Activate
    protected void activate(final Map<String, Object> properties) {
		logger.debug("activate: config activated.");
        configure(properties);
    }

    protected void configure(final Map<String, Object> properties) {
        this.heartbeatTimeout = PropertiesUtil.toLong(
                properties.get(HEARTBEAT_TIMEOUT_KEY),
                DEFAULT_HEARTBEAT_TIMEOUT);
        logger.debug("configure: heartbeatTimeout='{}'", this.heartbeatTimeout);

        this.heartbeatInterval = PropertiesUtil.toLong(
                properties.get(HEARTBEAT_INTERVAL_KEY),
                DEFAULT_HEARTBEAT_INTERVAL);
        logger.debug("configure: heartbeatInterval='{}'",
                this.heartbeatInterval);

        this.minEventDelay = PropertiesUtil.toInteger(
                properties.get(MIN_EVENT_DELAY_KEY),
                DEFAULT_MIN_EVENT_DELAY);
        logger.debug("configure: minEventDelay='{}'",
                this.minEventDelay);
        
        this.connectionTimeout = PropertiesUtil.toInteger(
                properties.get(CONNECTION_TIMEOUT_KEY),
                DEFAULT_CONNECTION_TIMEOUT);
        logger.debug("configure: connectionTimeout='{}'",
                this.connectionTimeout);
        
        this.soTimeout = PropertiesUtil.toInteger(
                properties.get(SO_TIMEOUT_KEY),
                DEFAULT_SO_TIMEOUT);
        logger.debug("configure: soTimeout='{}'",
                this.soTimeout);
        
        
        String[] topologyConnectorUrlsStr = PropertiesUtil.toStringArray(
                properties.get(TOPOLOGY_CONNECTOR_URLS_KEY), null);
        if (topologyConnectorUrlsStr!=null && topologyConnectorUrlsStr.length > 0) {
            List<URL> urls = new LinkedList<URL>();
            for (int i = 0; i < topologyConnectorUrlsStr.length; i++) {
                String anUrlStr = topologyConnectorUrlsStr[i];
                try {
                	if (anUrlStr!=null && anUrlStr.length()>0) {
	                    URL url = new URL(anUrlStr);
	                    logger.debug("configure: a topologyConnectorbUrl='{}'",
	                            url);
	                    urls.add(url);
                	}
                } catch (MalformedURLException e) {
                    logger.error("configure: could not set a topologyConnectorUrl: " + e,
                            e);
                }
            }
            if (urls.size()>0) {
                this.topologyConnectorUrls = urls.toArray(new URL[urls.size()]);
                logger.debug("configure: number of topologyConnectorUrls='{}''",
                        urls.size());
            } else {
                this.topologyConnectorUrls = null;
                logger.debug("configure: no (valid) topologyConnectorUrls configured");
            }
        } else {
            this.topologyConnectorUrls = null;
            logger.debug("configure: no (valid) topologyConnectorUrls configured");
        }
        this.topologyConnectorWhitelist = PropertiesUtil.toStringArray(
                properties.get(TOPOLOGY_CONNECTOR_WHITELIST_KEY),
                DEFAULT_TOPOLOGY_CONNECTOR_WHITELIST);
        logger.debug("configure: topologyConnectorWhitelist='{}'",
                this.topologyConnectorWhitelist);

        this.discoveryResourcePath = PropertiesUtil.toString(
                properties.get(DISCOVERY_RESOURCE_PATH_KEY),
                "");
        while(this.discoveryResourcePath.endsWith("/")) {
            this.discoveryResourcePath = this.discoveryResourcePath.substring(0,
                    this.discoveryResourcePath.length()-1);
        }
        this.discoveryResourcePath = this.discoveryResourcePath + "/";
        if (this.discoveryResourcePath==null || this.discoveryResourcePath.length()<=1) {
            // if the path is empty, or /, then use the default
            this.discoveryResourcePath = DEFAULT_DISCOVERY_RESOURCE_PATH;
        }
        logger.debug("configure: discoveryResourcePath='{}'",
                this.discoveryResourcePath);

        this.leaderElectionRepositoryDescriptor = PropertiesUtil.toString(
                properties.get(LEADER_ELECTION_REPOSITORY_DESCRIPTOR_NAME_KEY),
                null);
        logger.debug("configure: leaderElectionRepositoryDescriptor='{}'",
                this.leaderElectionRepositoryDescriptor);

        autoStopLocalLoopEnabled = PropertiesUtil.toBoolean(properties.get(AUTO_STOP_LOCAL_LOOP_ENABLED), false);
        gzipConnectorRequestsEnabled = PropertiesUtil.toBoolean(properties.get(GZIP_CONNECTOR_REQUESTS_ENABLED), false);
        
        hmacEnabled = PropertiesUtil.toBoolean(properties.get(HMAC_ENABLED), true);
        encryptionEnabled = PropertiesUtil.toBoolean(properties.get(ENCRYPTION_ENABLED), false);
        sharedKey = PropertiesUtil.toString(properties.get(SHARED_KEY), null);
        keyInterval = PropertiesUtil.toLong(SHARED_KEY_INTERVAL, DEFAULT_SHARED_KEY_INTERVAL);
        
        backoffStandbyFactor = PropertiesUtil.toInteger(properties.get(BACKOFF_STANDBY_FACTOR), 
                DEFAULT_BACKOFF_STANDBY_FACTOR);
        backoffStableFactor = PropertiesUtil.toInteger(properties.get(BACKOFF_STABLE_FACTOR), 
                DEFAULT_BACKOFF_STABLE_FACTOR);
    }

    /**
     * Returns the timeout (in seconds) after which an instance or voting is considered invalid/timed out
     * @return the timeout (in seconds) after which an instance or voting is considered invalid/timed out
     */
    public long getHeartbeatTimeout() {
        return heartbeatTimeout;
    }
    
    /**
     * Returns the timeout (in milliseconds) after which an instance or voting is considered invalid/timed out
     * @return the timeout (in milliseconds) after which an instance or voting is considered invalid/timed out
     */
    public long getHeartbeatTimeoutMillis() {
        return getHeartbeatTimeout() * 1000;
    }
    
    /**
     * Returns the socket connect() timeout used by the topology connector, 0 disables the timeout
     * @return the socket connect() timeout used by the topology connector, 0 disables the timeout
     */
    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    /**
     * Returns the socket read timeout (SO_TIMEOUT) used by the topology connector, 0 disables the timeout
     * @return the socket read timeout (SO_TIMEOUT) used by the topology connector, 0 disables the timeout
     */
    public int getSoTimeout() {
        return soTimeout;
    }
    
    /**
     * Returns the interval (in seconds) in which heartbeats are sent
     * @return the interval (in seconds) in which heartbeats are sent
     */
    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }
    
    /**
     * Returns the minimum time (in seconds) between sending TOPOLOGY_CHANGING/_CHANGED events - to avoid flooding
     * @return the minimum time (in seconds) between sending TOPOLOGY_CHANGING/_CHANGED events - to avoid flooding
     */
    public int getMinEventDelay() {
        return minEventDelay;
    }

    /**
     * Returns the URLs to which to open a topology connector - or null/empty if no topology connector
     * is configured (default is null)
     * @return the URLs to which to open a topology connector - or null/empty if no topology connector
     * is configured
     */
    public URL[] getTopologyConnectorURLs() {
        return topologyConnectorUrls;
    }

    /**
     * Returns a comma separated list of hostnames and/or ip addresses which are allowed as
     * remote hosts to open connections to the topology connector servlet
     * @return a comma separated list of hostnames and/or ip addresses which are allowed as
     * remote hosts to open connections to the topology connector servlet
     */
    public String[] getTopologyConnectorWhitelist() {
        return topologyConnectorWhitelist;
    }

    /**
     * Returns the resource path where cluster instance informations are stored.
     * @return the resource path where cluster instance informations are stored
     */
    public String getClusterInstancesPath() {
        return discoveryResourcePath + CLUSTERINSTANCES_RESOURCE;
    }

    /**
     * Returns the resource path where the established view is stored.
     * @return the resource path where the established view is stored
     */
    public String getEstablishedViewPath() {
        return discoveryResourcePath + ESTABLISHED_VIEW_RESOURCE;
    }

    /**
     * Returns the resource path where ongoing votings are stored.
     * @return the resource path where ongoing votings are stored
     */
    public String getOngoingVotingsPath() {
        return discoveryResourcePath + ONGOING_VOTING_RESOURCE;
    }

    /**
     * Returns the resource path where the previous view is stored.
     * @return the resource path where the previous view is stored
     */
    public String getPreviousViewPath() {
        return discoveryResourcePath + PREVIOUS_VIEW_RESOURCE;
    }

    /**
     * Returns the repository descriptor key which is to be included in the
     * cluster leader election - or null.
     * <p>
     * When set, the value (treated as a boolean) of the repository descriptor
     * is prepended to the leader election id.
     * @return the repository descriptor key which is to be included in the
     * cluster leader election - or null
     */
    public String getLeaderElectionRepositoryDescriptor() {
        return leaderElectionRepositoryDescriptor;
    }

    /**
     * @return true if hmac is enabled.
     */
    public boolean isHmacEnabled() {
        return hmacEnabled;
    }

    /**
     * @return the shared key
     */
    public String getSharedKey() {
        return sharedKey;
    }

    /**
     * @return the interval of the shared key for hmac.
     */
    public long getKeyInterval() {
        return keyInterval;
    }

    /**
     * @return true if encryption is enabled.
     */
    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }
    
    /**
     * @return true if requests on the topology connector should be gzipped
     * (which only works if the server accepts that.. ie discovery.impl 1.0.4+)
     */
    public boolean isGzipConnectorRequestsEnabled() {
        return gzipConnectorRequestsEnabled;
    }
    
    /**
     * @return true if the auto-stopping of local-loop topology connectors is enabled.
     */
    public boolean isAutoStopLocalLoopEnabled() {
        return autoStopLocalLoopEnabled;
    }

    /**
     * Returns the backoff factor to be used for standby (loop) connectors
     * @return the backoff factor to be used for standby (loop) connectors
     */
    public int getBackoffStandbyFactor() {
        return backoffStandbyFactor;
    }

    /**
     * Returns the (maximum) backoff factor to be used for stable connectors
     * @return the (maximum) backoff factor to be used for stable connectors
     */
    public int getBackoffStableFactor() {
        return backoffStableFactor;
    }

    /**
     * Returns the backoff interval for standby (loop) connectors in seconds
     * @return the backoff interval for standby (loop) connectors in seconds
     */
    public long getBackoffStandbyInterval() {
        final int factor = getBackoffStandbyFactor();
        if (factor<=1) {
            return -1;
        } else {
            return factor * getHeartbeatInterval();
        }
    }
}
