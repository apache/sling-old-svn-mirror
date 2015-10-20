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
package org.apache.sling.discovery.base.connectors.ping;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Iterator;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.http.Header;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.discovery.ClusterView;
import org.apache.sling.discovery.InstanceDescription;
import org.apache.sling.discovery.base.commons.ClusterViewService;
import org.apache.sling.discovery.base.commons.UndefinedClusterViewException;
import org.apache.sling.discovery.base.connectors.BaseConfig;
import org.apache.sling.discovery.base.connectors.announcement.Announcement;
import org.apache.sling.discovery.base.connectors.announcement.AnnouncementFilter;
import org.apache.sling.discovery.base.connectors.announcement.AnnouncementRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A topology connector client is used for sending (pinging) a remote topology
 * connector servlet and exchanging announcements with it
 */
public class TopologyConnectorClient implements
        TopologyConnectorClientInformation {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /** the endpoint url **/
    private final URL connectorUrl;

    /** the cluster view service **/
    private final ClusterViewService clusterViewService;

    /** the config service to user **/
    private final BaseConfig config;

    /** the id of this connection **/
    private final UUID id;

    /** the announcement registry **/
    private final AnnouncementRegistry announcementRegistry;

    /** the last inherited announcement **/
    private Announcement lastInheritedAnnouncement;

    /** the time when the last announcement was inherited - for webconsole use only **/
    private long lastPingedAt;
    
    /** the information about this server **/
    private final String serverInfo;
    
    /** the status code of the last post **/
    private int lastStatusCode = -1;
    
    /** SLING-3316: whether or not this connector was auto-stopped **/
    private boolean autoStopped = false;
    
    /** more details about connection failures **/
    private String statusDetails = null;
    
    /** SLING-2882: whether or not to suppress ping warnings **/
    private boolean suppressPingWarnings_ = false;

    private TopologyRequestValidator requestValidator;

    /** value of Content-Encoding of the last request **/
    private String lastRequestEncoding;

    /** value of Content-Encoding of the last repsonse **/
    private String lastResponseEncoding;

    /** SLING-3382: unix-time at which point the backoff-period ends and pings can be sent again **/
    private long backoffPeriodEnd = -1;
    
    TopologyConnectorClient(final ClusterViewService clusterViewService,
            final AnnouncementRegistry announcementRegistry, final BaseConfig config,
            final URL connectorUrl, final String serverInfo) {
        if (clusterViewService == null) {
            throw new IllegalArgumentException(
                    "clusterViewService must not be null");
        }
        if (announcementRegistry == null) {
            throw new IllegalArgumentException(
                    "announcementRegistry must not be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        if (connectorUrl == null) {
            throw new IllegalArgumentException("connectorUrl must not be null");
        }
        this.requestValidator = new TopologyRequestValidator(config);
        this.clusterViewService = clusterViewService;
        this.announcementRegistry = announcementRegistry;
        this.config = config;
        this.connectorUrl = connectorUrl;
        this.serverInfo = serverInfo;
        this.id = UUID.randomUUID();
    }

    /** ping the server and pass the announcements between the two **/
    void ping(final boolean force) {
    	if (autoStopped) {
    		// then we suppress any further pings!
    		logger.debug("ping: autoStopped=true, hence suppressing any further pings.");
    		return;
    	}
    	if (force) {
    	    backoffPeriodEnd = -1;
    	} else if (backoffPeriodEnd>0) {
    	    if (System.currentTimeMillis()<backoffPeriodEnd) {
    	        logger.debug("ping: not issueing a heartbeat due to backoff instruction from peer.");
    	        return;
    	    } else {
                logger.debug("ping: backoff period ended, issuing another ping now.");
    	    }
    	}
        final String uri = connectorUrl.toString()+"."+clusterViewService.getSlingId()+".json";
    	if (logger.isDebugEnabled()) {
    		logger.debug("ping: connectorUrl=" + connectorUrl + ", complete uri=" + uri);
    	}
    	final HttpClientContext clientContext = HttpClientContext.create();
    	final CloseableHttpClient httpClient = createHttpClient();
    	final HttpPut putRequest = new HttpPut(uri);

    	// setting the connection timeout (idle connection, configured in seconds)
    	putRequest.setConfig(RequestConfig.
    			custom().
    			setConnectTimeout(1000*config.getSocketConnectTimeout()).
    			build());

        Announcement resultingAnnouncement = null;
        try {
            String userInfo = connectorUrl.getUserInfo();
            if (userInfo != null) {
                Credentials c = new UsernamePasswordCredentials(userInfo);
            	clientContext.getCredentialsProvider().setCredentials(
                        new AuthScope(putRequest.getURI().getHost(), putRequest
                                .getURI().getPort()), c);
            }

            Announcement topologyAnnouncement = new Announcement(
                    clusterViewService.getSlingId());
            topologyAnnouncement.setServerInfo(serverInfo);
            final ClusterView clusterView;
            try {
                clusterView = clusterViewService
                        .getLocalClusterView();
            } catch (UndefinedClusterViewException e) {
                // SLING-5030 : then we cannot ping
                logger.warn("ping: no clusterView available at the moment, cannot ping others now: "+e);
                return;
            }
            topologyAnnouncement.setLocalCluster(clusterView);
            if (force) {
                logger.debug("ping: sending a resetBackoff");
                topologyAnnouncement.setResetBackoff(true);
            }
            announcementRegistry.addAllExcept(topologyAnnouncement, clusterView, new AnnouncementFilter() {
                
                public boolean accept(final String receivingSlingId, final Announcement announcement) {
                    // filter out announcements that are of old cluster instances
                    // which I dont really have in my cluster view at the moment
                    final Iterator<InstanceDescription> it = 
                            clusterView.getInstances().iterator();
                    while(it.hasNext()) {
                        final InstanceDescription instance = it.next();
                        if (instance.getSlingId().equals(receivingSlingId)) {
                            // then I have the receiving instance in my cluster view
                            // all fine then
                            return true;
                        }
                    }
                    // looks like I dont have the receiving instance in my cluster view
                    // then I should also not propagate that announcement anywhere
                    return false;
                }
            });
            final String p = requestValidator.encodeMessage(topologyAnnouncement.asJSON());
            
            if (logger.isDebugEnabled()) {
                logger.debug("ping: topologyAnnouncement json is: " + p);
            }
            requestValidator.trustMessage(putRequest, p);
            if (config.isGzipConnectorRequestsEnabled()) {
                // tell the server that the content is gzipped:
                putRequest.addHeader("Content-Encoding", "gzip");
                // and gzip the body:
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
                gzipOut.write(p.getBytes("UTF-8"));
                gzipOut.close();
                final byte[] gzippedEncodedJson = baos.toByteArray();
                putRequest.setEntity(new ByteArrayEntity(gzippedEncodedJson, ContentType.APPLICATION_JSON));
                lastRequestEncoding = "gzip";
            } else {
                // otherwise plaintext:
            	final StringEntity plaintext = new StringEntity(p, "UTF-8");
            	plaintext.setContentType(ContentType.APPLICATION_JSON.getMimeType());
            	putRequest.setEntity(plaintext);
                lastRequestEncoding = "plaintext";
            }
            // independent of request-gzipping, we do accept the response to be gzipped,
            // so indicate this to the server:
            putRequest.addHeader("Accept-Encoding", "gzip");
            final CloseableHttpResponse response = httpClient.execute(putRequest, clientContext);
        	if (logger.isDebugEnabled()) {
	            logger.debug("ping: done. code=" + response.getStatusLine().getStatusCode() + " - "
	                    + response.getStatusLine().getReasonPhrase());
        	}
            lastStatusCode = response.getStatusLine().getStatusCode();
            lastResponseEncoding = null;
            if (response.getStatusLine().getStatusCode()==HttpServletResponse.SC_OK) {
                final Header contentEncoding = response.getFirstHeader("Content-Encoding");
                if (contentEncoding!=null && contentEncoding.getValue()!=null &&
                        contentEncoding.getValue().contains("gzip")) {
                    lastResponseEncoding = "gzip";
                } else {
                    lastResponseEncoding = "plaintext";
                }
                final String responseBody = requestValidator.decodeMessage(putRequest.getURI().getPath(), response); // limiting to 16MB, should be way enough
            	if (logger.isDebugEnabled()) {
            		logger.debug("ping: response body=" + responseBody);
            	}
                if (responseBody!=null && responseBody.length()>0) {
                    Announcement inheritedAnnouncement = Announcement
                            .fromJSON(responseBody);
                    final long backoffInterval = inheritedAnnouncement.getBackoffInterval();
                    if (backoffInterval>0) {
                        // then reset the backoffPeriodEnd:
                        
                        /* minus 1 sec to avoid slipping the interval by a few millis */
                        this.backoffPeriodEnd = System.currentTimeMillis() + (1000 * backoffInterval) - 1000;
                        logger.debug("ping: servlet instructed to backoff: backoffInterval="+backoffInterval+", resulting in period end of "+new Date(backoffPeriodEnd));
                    } else {
                        logger.debug("ping: servlet did not instruct any backoff-ing at this stage");
                        this.backoffPeriodEnd = -1;
                    }
                    if (inheritedAnnouncement.isLoop()) {
                    	if (logger.isDebugEnabled()) {
	                        logger.debug("ping: connector response indicated a loop detected. not registering this announcement from "+
	                                    inheritedAnnouncement.getOwnerId());
                    	}
                    	if (inheritedAnnouncement.getOwnerId().equals(clusterViewService.getSlingId())) {
                    		// SLING-3316 : local-loop detected. Check config to see if we should stop this connector
                    		
                    	    if (config.isAutoStopLocalLoopEnabled()) {
                    			inheritedAnnouncement = null; // results in connected -> false and representsloop -> true
                    			autoStopped = true; // results in isAutoStopped -> true
                    		}
                    	}
                    } else {
                        inheritedAnnouncement.setInherited(true);
                        if (announcementRegistry
                                .registerAnnouncement(inheritedAnnouncement)==-1) {
                        	if (logger.isDebugEnabled()) {
	                            logger.debug("ping: connector response is from an instance which I already see in my topology"
	                                    + inheritedAnnouncement);
                        	}
                            statusDetails = "receiving side is seeing me via another path (connector or cluster) already (loop)";
                            return;
                        }
                    }
                    resultingAnnouncement = inheritedAnnouncement;
                    statusDetails = null;
                } else {
                    statusDetails = "no response body received";
                }
            } else {
                statusDetails = "got HTTP Status-Code: "+lastStatusCode;
            }
        	// SLING-2882 : reset suppressPingWarnings_ flag in success case
    		suppressPingWarnings_ = false;
        } catch (IOException e) {
        	// SLING-2882 : set/check the suppressPingWarnings_ flag
        	if (suppressPingWarnings_) {
        		if (logger.isDebugEnabled()) {
        			logger.debug("ping: got IOException: " + e + ", uri=" + uri);
        		}
        	} else {
        		suppressPingWarnings_ = true;
    			logger.warn("ping: got IOException [suppressing further warns]: " + e + ", uri=" + uri);
        	}
            statusDetails = e.toString();
        } catch (JSONException e) {
            logger.warn("ping: got JSONException: " + e);
            statusDetails = e.toString();
        } catch (RuntimeException re) {
            logger.warn("ping: got RuntimeException: " + re, re);
            statusDetails = re.toString();
        } finally {
            putRequest.releaseConnection();
            lastInheritedAnnouncement = resultingAnnouncement;
            lastPingedAt = System.currentTimeMillis();
            try {
				httpClient.close();
			} catch (IOException e) {
				logger.error("disconnect: could not close httpClient: "+e, e);
			}
        }
    }

	private CloseableHttpClient createHttpClient() {
		final HttpClientBuilder builder = HttpClientBuilder.create();
    	// setting the SoTimeout (which is configured in seconds)
    	builder.setDefaultSocketConfig(SocketConfig.
    			custom().
    			setSoTimeout(1000*config.getSoTimeout()).
    			build());
		builder.setRetryHandler(new DefaultHttpRequestRetryHandler(0, false));

    	return builder.build();
	}

    public int getStatusCode() {
        return lastStatusCode;
    }
    
    public URL getConnectorUrl() {
        return connectorUrl;
    }
    
    public boolean representsLoop() {
    	if (autoStopped) {
    		return true;
    	}
        if (lastInheritedAnnouncement == null) {
            return false;
        } else {
            return lastInheritedAnnouncement.isLoop();
        }
    }

    public boolean isConnected() {
    	if (autoStopped) {
    		return false;
    	}
        if (lastInheritedAnnouncement == null) {
            return false;
        } else {
            return announcementRegistry.hasActiveAnnouncement(lastInheritedAnnouncement.getOwnerId());
        }
    }
    
    public String getStatusDetails() {
        if (autoStopped) {
            return "auto-stopped";
        }
        if (lastInheritedAnnouncement == null) {
            return statusDetails;
        } else {
            if (announcementRegistry.hasActiveAnnouncement(lastInheritedAnnouncement.getOwnerId())) {
                // still active - so no status details
                return null;
            } else {
                return "received announcement has expired (it was last renewed "+new Date(lastPingedAt)+") - consider increasing heartbeat timeout";
            }
        }
    }
    
    public long getLastPingSent() {
        return lastPingedAt;
    }
    
    public int getNextPingDue() {
        final long absDue;
        if (backoffPeriodEnd>0) {
            absDue = backoffPeriodEnd;
        } else {
            absDue = lastPingedAt + 1000*config.getConnectorPingInterval();
        }
        final int relDue = (int) ((absDue - System.currentTimeMillis()) / 1000);
        if (relDue<0) {
            return -1;
        } else {
            return relDue;
        }
    }
    
    public boolean isAutoStopped() {
    	return autoStopped;
    }
    
    public String getLastRequestEncoding() {
        return lastRequestEncoding==null ? "" : lastRequestEncoding;
    }

    public String getLastResponseEncoding() {
        return lastResponseEncoding==null ? "" : lastResponseEncoding;
    }
    
    public String getRemoteSlingId() {
        if (lastInheritedAnnouncement == null) {
            return null;
        } else {
            return lastInheritedAnnouncement.getOwnerId();
        }
    }

    public String getId() {
        return id.toString();
    }

    /** Disconnect this connector **/
    public void disconnect() {
        final String uri = connectorUrl.toString()+"."+clusterViewService.getSlingId()+".json";
    	if (logger.isDebugEnabled()) {
    		logger.debug("disconnect: connectorUrl=" + connectorUrl + ", complete uri="+uri);
    	}

        if (lastInheritedAnnouncement != null) {
            announcementRegistry
                    .unregisterAnnouncement(lastInheritedAnnouncement
                            .getOwnerId());
        }

        final HttpClientContext clientContext = HttpClientContext.create();
        final CloseableHttpClient httpClient = createHttpClient();
        final HttpDelete deleteRequest = new HttpDelete(uri);
        // setting the connection timeout (idle connection, configured in seconds)
        deleteRequest.setConfig(RequestConfig.
        		custom().
        		setConnectTimeout(1000*config.getSocketConnectTimeout()).
        		build());

        try {
            String userInfo = connectorUrl.getUserInfo();
            if (userInfo != null) {
                Credentials c = new UsernamePasswordCredentials(userInfo);
                clientContext.getCredentialsProvider().setCredentials(
                        new AuthScope(deleteRequest.getURI().getHost(), deleteRequest
                                .getURI().getPort()), c);
            }

            requestValidator.trustMessage(deleteRequest, null);
            final CloseableHttpResponse response = httpClient.execute(deleteRequest, clientContext);
        	if (logger.isDebugEnabled()) {
	            logger.debug("disconnect: done. code=" + response.getStatusLine().getStatusCode()
	                    + " - " + response.getStatusLine().getReasonPhrase());
        	}
            // ignoring the actual statuscode though as there's little we can
            // do about it after this point
        } catch (IOException e) {
            logger.warn("disconnect: got IOException: " + e);
        } catch (RuntimeException re) {
            logger.error("disconnect: got RuntimeException: " + re, re);
        } finally {
            deleteRequest.releaseConnection();
            try {
				httpClient.close();
			} catch (IOException e) {
				logger.error("disconnect: could not close httpClient: "+e, e);
			}
        }
    }
}
