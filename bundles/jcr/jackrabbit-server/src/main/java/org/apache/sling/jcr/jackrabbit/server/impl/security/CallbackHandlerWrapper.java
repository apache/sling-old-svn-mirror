/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.jcr.jackrabbit.server.impl.security;

import org.apache.jackrabbit.core.security.authentication.CredentialsCallback;
import org.apache.jackrabbit.core.security.authentication.ImpersonationCallback;

import java.io.IOException;

import javax.jcr.Credentials;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.UnsupportedCallbackException;

/**
 *
 */
public class CallbackHandlerWrapper implements CallbackHandler {

    private CallbackHandler callbackHandler;

    /**
     * @param callbackHandler
     */
    public CallbackHandlerWrapper(Subject subject,
            CallbackHandler callbackHandler) {
        this.callbackHandler = callbackHandler;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.security.auth.callback.CallbackHandler#handle(javax.security.auth.callback.Callback[])
     */
    public void handle(Callback[] callbacks) throws IOException,
            UnsupportedCallbackException {
        for (Callback cb : callbacks) {
            if (cb instanceof NameCallback) {
                Credentials creds = getCredentials();
                NameCallback nameCallback = (NameCallback) cb;
                if (creds instanceof TrustedCredentials) {
                    nameCallback.setName(((TrustedCredentials) creds)
                            .getPrincipal().getName());
                    return;
                }
            } else if (cb instanceof ImpersonationCallback) {
                Credentials creds = getCredentials();
                ImpersonationCallback impersonationCallback = (ImpersonationCallback) cb;
                if (creds instanceof TrustedCredentials) {
                    impersonationCallback
                            .setImpersonator(((TrustedCredentials) creds)
                                    .getImpersonator());
                    return;
                }
            }

        }
        if (callbackHandler != null) {
            callbackHandler.handle(callbacks);
        }
    }

    /**
     * @return
     * @throws UnsupportedCallbackException
     * @throws IOException
     */
    private Credentials getCredentials() throws IOException,
            UnsupportedCallbackException {
        CredentialsCallback credentialsCallback = new CredentialsCallback();
        callbackHandler.handle(new Callback[] { credentialsCallback });
        return credentialsCallback.getCredentials();
    }

}
