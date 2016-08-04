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
package org.apache.sling.discovery.oak;

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
import org.apache.sling.discovery.base.connectors.BaseConfig;
import org.apache.sling.discovery.commons.providers.spi.base.DiscoveryLiteConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration object used as a central config point for the discovery service
 * implementation
 * <p>
 * The properties are described below under.
 */
@Component(metatype = true, label="%config.name", description="%config.description")
@Service(value = { Config.class, BaseConfig.class, DiscoveryLiteConfig.class })
public class Config implements BaseConfig, DiscoveryLiteConfig {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** resource used to keep instance information such as last heartbeat, properties, incoming announcements **/
    private static final String CLUSTERINSTANCES_RESOURCE = "clusterInstances";

    /** resource used to store the sync tokens as part of a topology change **/
    private static final String SYNC_TOKEN_RESOURCE = "syncTokens";

    /** resource used to store the clusterNodeIds to slingIds map **/
    private static final String ID_MAP_RESOURCE = "idMap";

    /** Configure the timeout (in seconds) after which an instance is considered dead/crashed. */
    public static final long DEFAULT_TOPOLOGY_CONNECTOR_TIMEOUT = 120;
    @Property(longValue=DEFAULT_TOPOLOGY_CONNECTOR_TIMEOUT)
    public static final String TOPOLOGY_CONNECTOR_TIMEOUT_KEY = "connectorPingTimeout";
    protected long connectorPingTimeout = DEFAULT_TOPOLOGY_CONNECTOR_TIMEOUT;

    /** Configure the interval (in seconds) according to which the heartbeats are exchanged in the topology. */
    public static final long DEFAULT_TOPOLOGY_CONNECTOR_INTERVAL = 30;
    @Property(longValue=DEFAULT_TOPOLOGY_CONNECTOR_INTERVAL)
    public static final String TOPOLOGY_CONNECTOR_INTERVAL_KEY = "connectorPingInterval";
    protected long connectorPingInterval = DEFAULT_TOPOLOGY_CONNECTOR_INTERVAL;
    
    /** Configure the interval (in seconds) according to which the heartbeats are exchanged in the topology. */
    public static final long DEFAULT_DISCOVERY_LITE_CHECK_INTERVAL = 2;
    @Property(longValue=DEFAULT_DISCOVERY_LITE_CHECK_INTERVAL)
    public static final String DISCOVERY_LITE_CHECK_INTERVAL_KEY = "discoveryLiteCheckInterval";
    protected long discoveryLiteCheckInterval = DEFAULT_DISCOVERY_LITE_CHECK_INTERVAL;
    
    public static final long DEFAULT_CLUSTER_SYNC_SERVICE_TIMEOUT = 120;
    @Property(longValue=DEFAULT_CLUSTER_SYNC_SERVICE_TIMEOUT)
    public static final String CLUSTER_SYNC_SERVICE_TIMEOUT_KEY = "clusterSyncServiceTimeout";
    protected long clusterSyncServiceTimeout = DEFAULT_CLUSTER_SYNC_SERVICE_TIMEOUT;

    public static final long DEFAULT_CLUSTER_SYNC_SERVICE_INTERVAL = 2;
    @Property(longValue=DEFAULT_CLUSTER_SYNC_SERVICE_INTERVAL)
    public static final String CLUSTER_SYNC_SERVICE_INTERVAL_KEY = "clusterSyncServiceInterval";
    protected long clusterSyncServiceInterval = DEFAULT_CLUSTER_SYNC_SERVICE_INTERVAL;

    /**
     * If set to true a syncToken will be used on top of waiting for
     * deactivating instances to be fully processed.
     * If set to false, only deactivating instances will be waited for
     * to be fully processed.
     */
    @Property(boolValue=true)
    private static final String SYNC_TOKEN_ENABLED = "enableSyncToken";

    /** Configure the time (in seconds) which must be passed at minimum between sending TOPOLOGY_CHANGING/_CHANGED (avoid flooding). */
    public static final int DEFAULT_MIN_EVENT_DELAY = 3;
    @Property(intValue=DEFAULT_MIN_EVENT_DELAY)
    public static final String MIN_EVENT_DELAY_KEY = "minEventDelay";
    protected int minEventDelay = DEFAULT_MIN_EVENT_DELAY;

    /** Configure the socket connect timeout for topology connectors. */
    public static final int DEFAULT_SOCKET_CONNECT_TIMEOUT = 10;
    @Property(intValue=DEFAULT_SOCKET_CONNECT_TIMEOUT)
    public static final String SOCKET_CONNECT_TIMEOUT_KEY = "socketConnectTimeout";
    private int socketConnectTimeout = DEFAULT_SOCKET_CONNECT_TIMEOUT;

    /** Configure the socket read timeout (SO_TIMEOUT) for topology connectors. */
    public static final int DEFAULT_SO_TIMEOUT = 10;
    @Property(intValue=DEFAULT_SO_TIMEOUT)
    public static final String SO_TIMEOUT_KEY = "soTimeout";
    private int soTimeout = DEFAULT_SO_TIMEOUT;

    /** URLs where to join a topology, eg http://localhost:4502/libs/sling/topology/connector */
    @Property(cardinality=1024)
    public static final String TOPOLOGY_CONNECTOR_URLS_KEY = "topologyConnectorUrls";
    private URL[] topologyConnectorUrls = {null};

    /** list of ips and/or hostnames which are allowed to connect to /libs/sling/topology/connector */
    private static final String[] DEFAULT_TOPOLOGY_CONNECTOR_WHITELIST = {"localhost","127.0.0.1"};
    @Property(value={"localhost","127.0.0.1"})
    public static final String TOPOLOGY_CONNECTOR_WHITELIST_KEY = "topologyConnectorWhitelist";
    protected String[] topologyConnectorWhitelist = DEFAULT_TOPOLOGY_CONNECTOR_WHITELIST;

    /** Path of resource where to keep discovery information, e.g /var/discovery/oak/ */
    private static final String DEFAULT_DISCOVERY_RESOURCE_PATH = "/var/discovery/oak/";
    @Property(value=DEFAULT_DISCOVERY_RESOURCE_PATH, propertyPrivate=true)
    public static final String DISCOVERY_RESOURCE_PATH_KEY = "discoveryResourcePath";
    protected String discoveryResourcePath = DEFAULT_DISCOVERY_RESOURCE_PATH;

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
    
    /**
     * Whether, on top of waiting for deactivating instances,
     * a syncToken should also be used
     */
    private boolean syncTokenEnabled;
    
    @Activate
    protected void activate(final Map<String, Object> properties) {
		logger.debug("activate: config activated.");
        configure(properties);
    }

    protected void configure(final Map<String, Object> properties) {
        this.connectorPingTimeout = PropertiesUtil.toLong(
                properties.get(TOPOLOGY_CONNECTOR_TIMEOUT_KEY),
                DEFAULT_TOPOLOGY_CONNECTOR_TIMEOUT);
        logger.debug("configure: connectorPingTimeout='{}'", 
                this.connectorPingTimeout);

        this.connectorPingInterval = PropertiesUtil.toLong(
                properties.get(TOPOLOGY_CONNECTOR_INTERVAL_KEY),
                DEFAULT_TOPOLOGY_CONNECTOR_INTERVAL);
        logger.debug("configure: connectorPingInterval='{}'",
                this.connectorPingInterval);
        
        this.discoveryLiteCheckInterval = PropertiesUtil.toLong(
                properties.get(DISCOVERY_LITE_CHECK_INTERVAL_KEY),
                DEFAULT_DISCOVERY_LITE_CHECK_INTERVAL);
        logger.debug("configure: discoveryLiteCheckInterval='{}'",
                this.discoveryLiteCheckInterval);
                
        this.clusterSyncServiceTimeout = PropertiesUtil.toLong(
                properties.get(CLUSTER_SYNC_SERVICE_TIMEOUT_KEY),
                DEFAULT_CLUSTER_SYNC_SERVICE_TIMEOUT);
        logger.debug("configure: clusterSyncServiceTimeout='{}'",
                this.clusterSyncServiceTimeout);

        this.clusterSyncServiceInterval = PropertiesUtil.toLong(
                properties.get(CLUSTER_SYNC_SERVICE_INTERVAL_KEY),
                DEFAULT_CLUSTER_SYNC_SERVICE_TIMEOUT);
        logger.debug("configure: clusterSyncServiceInterval='{}'",
                this.clusterSyncServiceInterval);

        this.minEventDelay = PropertiesUtil.toInteger(
                properties.get(MIN_EVENT_DELAY_KEY),
                DEFAULT_MIN_EVENT_DELAY);
        logger.debug("configure: minEventDelay='{}'",
                this.minEventDelay);
        
        this.socketConnectTimeout = PropertiesUtil.toInteger(
                properties.get(SOCKET_CONNECT_TIMEOUT_KEY),
                DEFAULT_SOCKET_CONNECT_TIMEOUT);
        logger.debug("configure: socketConnectTimeout='{}'",
                this.socketConnectTimeout);
        
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

        autoStopLocalLoopEnabled = PropertiesUtil.toBoolean(properties.get(AUTO_STOP_LOCAL_LOOP_ENABLED), false);
        gzipConnectorRequestsEnabled = PropertiesUtil.toBoolean(properties.get(GZIP_CONNECTOR_REQUESTS_ENABLED), false);
        
        hmacEnabled = PropertiesUtil.toBoolean(properties.get(HMAC_ENABLED), true);
        encryptionEnabled = PropertiesUtil.toBoolean(properties.get(ENCRYPTION_ENABLED), false);
        syncTokenEnabled = PropertiesUtil.toBoolean(properties.get(SYNC_TOKEN_ENABLED), true);
        sharedKey = PropertiesUtil.toString(properties.get(SHARED_KEY), null);
        keyInterval = PropertiesUtil.toLong(SHARED_KEY_INTERVAL, DEFAULT_SHARED_KEY_INTERVAL);
        
        backoffStandbyFactor = PropertiesUtil.toInteger(properties.get(BACKOFF_STANDBY_FACTOR), 
                DEFAULT_BACKOFF_STANDBY_FACTOR);
        backoffStableFactor = PropertiesUtil.toInteger(properties.get(BACKOFF_STABLE_FACTOR), 
                DEFAULT_BACKOFF_STABLE_FACTOR);
    }

    /**
     * Returns the socket connect() timeout used by the topology connector, 0 disables the timeout
     * @return the socket connect() timeout used by the topology connector, 0 disables the timeout
     */
    public int getSocketConnectTimeout() {
        return socketConnectTimeout;
    }

    /**
     * Returns the socket read timeout (SO_TIMEOUT) used by the topology connector, 0 disables the timeout
     * @return the socket read timeout (SO_TIMEOUT) used by the topology connector, 0 disables the timeout
     */
    public int getSoTimeout() {
        return soTimeout;
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
    
    protected String getDiscoveryResourcePath() {
        return discoveryResourcePath;
    }

    /**
     * Returns the resource path where cluster instance informations are stored.
     * @return the resource path where cluster instance informations are stored
     */
    public String getClusterInstancesPath() {
        return getDiscoveryResourcePath() + CLUSTERINSTANCES_RESOURCE;
    }

    @Override
    public String getSyncTokenPath() {
        return getDiscoveryResourcePath() + SYNC_TOKEN_RESOURCE;
    }

    @Override
    public String getIdMapPath() {
        return getDiscoveryResourcePath() + ID_MAP_RESOURCE;
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
            return factor * getConnectorPingInterval();
        }
    }

    @Override
    public long getConnectorPingInterval() {
        return connectorPingInterval;
    }
    
    @Override
    public long getConnectorPingTimeout() {
        return connectorPingTimeout;
    }
    
    public long getDiscoveryLiteCheckInterval() {
        return discoveryLiteCheckInterval;
    }
    
    @Override
    public long getClusterSyncServiceTimeoutMillis() {
        return clusterSyncServiceTimeout * 1000;
    }

    @Override
    public long getClusterSyncServiceIntervalMillis() {
        return clusterSyncServiceInterval * 1000;
    }
    
    public boolean getSyncTokenEnabled() {
        return syncTokenEnabled;
    }
}
