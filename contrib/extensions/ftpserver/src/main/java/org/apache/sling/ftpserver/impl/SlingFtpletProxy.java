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
package org.apache.sling.ftpserver.impl;

import java.io.IOException;

import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpReply;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.FtpletContext;
import org.apache.ftpserver.ftplet.FtpletResult;

public class SlingFtpletProxy implements Ftplet {

    private Ftplet delegatee;

    private FtpletContext ftpletContext;

    public void setDelegatee(Ftplet delegatee) {
        if (this.delegatee != null) {
            this.delegatee.destroy();
            this.delegatee = null;
        }

        if (delegatee != null) {
            this.delegatee = delegatee;

            if (this.ftpletContext != null) {
                try {
                    this.delegatee.init(this.ftpletContext);
                } catch (FtpException e) {
                    // TODO log
                }
            }
        }

    }

    public void init(FtpletContext ftpletContext) throws FtpException {
        this.ftpletContext = ftpletContext;
        if (this.delegatee != null) {
            this.delegatee.init(ftpletContext);
        }
    }

    public void destroy() {
        if (this.delegatee != null) {
            this.delegatee.destroy();
        }
        this.ftpletContext = null;
    }

    public FtpletResult beforeCommand(FtpSession session, FtpRequest request) throws FtpException, IOException {
        if (this.delegatee != null) {
            return this.delegatee.beforeCommand(session, request);
        }
        return null;
    }

    public FtpletResult afterCommand(FtpSession session, FtpRequest request, FtpReply reply) throws FtpException,
            IOException {
        if (this.delegatee != null) {
            return this.delegatee.afterCommand(session, request, reply);
        }
        return null;
    }

    public FtpletResult onConnect(FtpSession session) throws FtpException, IOException {
        if (this.delegatee != null) {
            return this.delegatee.onConnect(session);
        }
        return null;
    }

    public FtpletResult onDisconnect(FtpSession session) throws FtpException, IOException {
        if (this.delegatee != null) {
            return this.delegatee.onDisconnect(session);
        }
        return null;
    }

}
