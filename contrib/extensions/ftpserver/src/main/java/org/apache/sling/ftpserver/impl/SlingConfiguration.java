/*************************************************************************
 *
 * ADOBE CONFIDENTIAL
 * ___________________
 *
 *  Copyright 2013 Adobe Systems Incorporated
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 **************************************************************************/
package org.apache.sling.ftpserver.impl;

import java.util.Map;

import org.apache.ftpserver.ConnectionConfig;
import org.apache.sling.commons.osgi.PropertiesUtil;

public class SlingConfiguration implements ConnectionConfig {

    private final Map<String, Object> config;

    static final String NAME = "org.apache.sling.ftpserver.SlingFtpServer";

    static final String PROP_PORT = "port";

    static final int PROP_PORT_DEFAULT = 1221;

    static final String PROP_MAX_LOGINS = "maxLogins";

    static final int PROP_MAX_LOGINS_DEFAULT = 10;

    static final String PROP_ANONYMOUS_LOGINS = "anonymousLoginEnabled";

    static final boolean PROP_ANONYMOUS_LOGINS_DEFAULT = true;

    static final String PROP_MAX_ANONYMOUS_LOGINS = "maxAnonymousLogins";

    static final int PROP_MAX_ANONYMOUS_LOGINS_DEFAULT = 10;

    static final String PROP_MAX_LOGIN_FAILURES = "maxLoginFailures";

    static final int PROP_MAX_LOGIN_FAILURES_DEFAULT = 3;

    static final String PROP_LOGIN_FAILURE_DELAY = "loginFailureDelay";

    static final int PROP_LOGIN_FAILURE_DELAY_DEFAUT = 500;

    static final String PROP_MAX_THREADS = "maxThreads";

    static final int PROP_MAX_THREADS_DEFAULT = 0;

    static final String PROP_ENABLED = "enabled";

    static final boolean PROP_ENABLED_DEFAULT = true;

    static final String PROP_MAX_IDLE_TIME = "max.idle.time.sec";

    static final int PROP_MAX_IDLE_TIME_DEFAULT = 10 * 60;

    static final String PROP_MAX_CONCURRENT = "max.concurrent";

    static final int PROP_MAX_CONCURRENT_DEFAULT = 10;

    static final String PROP_MAX_CONCURRENT_IP = "max.concurrent.ip";

    static final int PROP_MAX_CONCURRENT_IP_DEFAULT = 10;

    static final String PROP_MAX_UPLOAD = "max.upload";

    static final int PROP_MAX_UPLOAD_DEFAULT = 0;

    static final String PROP_MAX_DOWNLOAD = "max.download";

    static final int PROP_MAX_DOWNLOAD_DEFAULT = 0;

    static final String PROP_FTP_HOME = "home";

    static final String PROP_FTP_HOME_DEFAULT = "/";

    public SlingConfiguration(final Map<String, Object> config) {
        this.config = config;
    }

    public int getMaxLoginFailures() {
        return get(PROP_MAX_LOGIN_FAILURES, PROP_MAX_LOGIN_FAILURES_DEFAULT);
    }

    public int getLoginFailureDelay() {
        return get(PROP_LOGIN_FAILURE_DELAY, PROP_LOGIN_FAILURE_DELAY_DEFAUT);
    }

    public int getMaxAnonymousLogins() {
        return get(PROP_MAX_ANONYMOUS_LOGINS, PROP_MAX_ANONYMOUS_LOGINS_DEFAULT);
    }

    public int getMaxLogins() {
        return get(PROP_MAX_LOGINS, PROP_MAX_LOGINS_DEFAULT);
    }

    public boolean isAnonymousLoginEnabled() {
        return get(PROP_ANONYMOUS_LOGINS, PROP_ANONYMOUS_LOGINS_DEFAULT);
    }

    public int getMaxThreads() {
        return get(PROP_MAX_THREADS, PROP_MAX_THREADS_DEFAULT);
    }

    int getPort() {
        return get(PROP_PORT, PROP_PORT_DEFAULT);
    }

    // whether users are enabled by default
    boolean isEnabled() {
        return get(PROP_ENABLED, PROP_ENABLED_DEFAULT);
    }

    // max idle time in seconds for users
    int getMaxIdelTimeSec() {
        return get(PROP_MAX_IDLE_TIME, PROP_MAX_IDLE_TIME_DEFAULT);
    }

    // max concurrent logins per user
    int getMaxConcurrent() {
        return get(PROP_MAX_CONCURRENT, PROP_MAX_CONCURRENT_DEFAULT);
    }

    // max concurrent logins per user from same IP
    int getMaxConcurrentPerIp() {
        return get(PROP_MAX_CONCURRENT_IP, PROP_MAX_CONCURRENT_IP_DEFAULT);
    }

    // max rate of data downloads
    int getMaxDownloadRate() {
        return get(PROP_MAX_DOWNLOAD, PROP_MAX_DOWNLOAD_DEFAULT);
    }

    // max rate of data uploads
    int getMaxUploadRate() {
        return get(PROP_MAX_UPLOAD, PROP_MAX_UPLOAD_DEFAULT);
    }

    String getFtpHome() {
        return get(PROP_FTP_HOME, PROP_FTP_HOME_DEFAULT);
    }

    boolean get(final String name, final boolean defaultValue) {
        return PropertiesUtil.toBoolean(this.config.get(name), defaultValue);
    }

    int get(final String name, final int defaultValue) {
        return PropertiesUtil.toInteger(this.config.get(name), defaultValue);
    }

    String get(final String name, final String defaultValue) {
        return PropertiesUtil.toString(this.config.get(name), defaultValue);
    }
}
