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
package org.apache.sling.jobs.impl;

import org.apache.sling.jobs.impl.spi.MapValueAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by ieb on 29/03/2016.
 */
public class Utils {

    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);
    private static final Charset UTF8 = Charset.forName("UTF-8") ;
    private static final String PROCESS_NAME = generateUniqueNamespace();
    // Set the counter to the class load time.
    private static final AtomicLong idCounter = new AtomicLong(System.currentTimeMillis());

    /**
     * Gets a string
     * @return
     */
    @Nonnull
    private static String generateUniqueNamespace() {
        String macAddress = null;
        // get the MAC address of the primary interface, failing that use a fake.
        try {
            for ( Enumeration<NetworkInterface> netInterfaceE = NetworkInterface.getNetworkInterfaces(); netInterfaceE.hasMoreElements();) {
                NetworkInterface netInterface = netInterfaceE.nextElement();
                byte[] hw = netInterface.getHardwareAddress();
                if ( !netInterface.isLoopback() && !netInterface.isVirtual() && hw != null) {
                    macAddress = tohex(hw);
                    LOGGER.info("Job IDs seeded with MAC Address from interface {} ", netInterface);
                    break;
                }
            }
            if ( macAddress == null) {
                LOGGER.info("No MAC address available, seeding JobID from startup time.");
                macAddress = "fake-" + System.currentTimeMillis();
            }
        } catch (SocketException e) {
            LOGGER.warn("Unable to get MAC address, defaulting to fake ", e);
        }
        long processID;
        try {
            // most JVMs.
            processID = Long.parseLong(java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);
        } catch (Exception e) {
            try {
                // most Linux kernels.
                processID = Long.parseLong(new File("/proc/self").getCanonicalFile().getName());
            } catch (Exception e1) {
                LOGGER.warn("Unable to get ProcessID by  address, defaulting to fake ", e);
                processID = System.currentTimeMillis();  // this will be way beyond any process ID.
            }
        }
        String baseId = macAddress + "/" + processID+ "/";
        LOGGER.info("Job IDS base is {} ", baseId);
        return  baseId;
    }

    @Nonnull
    private static String tohex(@Nonnull byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for( byte b : bytes) {
            sb.append(String.format("%02x",b));
        }
        return sb.toString();
    }

    /**
     * Generate an ID based on the unique name of the jvm process and a counter.
     * @return
     */
    @Nonnull
    public static String generateId() {
        try {
            return Utils.tohex(MessageDigest.getInstance("SHA1").digest((Utils.PROCESS_NAME+idCounter.incrementAndGet()).getBytes(UTF8)));
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException("SHA1 not supported", nsae);
        }
    }

    @Nonnull
    public static Map<String, Object> toMapValue(@Nonnull Object msg) {
        if (msg instanceof Map) {
            //noinspection unchecked
            return (Map<String, Object>) msg;
        } else if (msg instanceof MapValueAdapter) {
            return toMapValue(((MapValueAdapter) msg).toMapValue());
        }
        throw new IllegalArgumentException("Unable to convert "+msg.getClass()+" to a Map.");
    }


    @Nonnull
    public static <T> T getRequired(@Nonnull Map<String, Object> m, @Nonnull String name) {
        if (m.containsKey(name)) {
            //noinspection unchecked
            if ( m.get(name) != null) {
                return (T) m.get(name);
            }
        }
        throw new IllegalArgumentException("Required key "+name+" is missing from "+m);
    }

    @Nullable
    public static <T> T getOptional(@Nonnull Map<String, Object> m, @Nonnull String name, @Nullable T defaultValue) {
        if (m.containsKey(name)) {
            //noinspection unchecked
            Object o = m.get(name);
            if ( defaultValue instanceof Integer && o instanceof Long) {
                return (T)(Integer) ((Long) o).intValue();
            } else if ( defaultValue instanceof Float && o instanceof Double) {
                return (T)(Float) ((Double) o).floatValue();
            }
            return (T) o;
        }
        return defaultValue;
    }


}
