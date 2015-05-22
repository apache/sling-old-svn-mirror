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
package org.apache.sling.bgservlets.impl;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

import org.apache.sling.bgservlets.JobProgressInfo;
import org.apache.sling.bgservlets.JobStatus;

/**
 * Wraps an OutputStream with controls for suspending it or throwing an
 * IOException next time it is written to. Used to suspend background servlets
 * (by blocking the stream) or stop them (by throwing an exception).
 */
public class SuspendableOutputStream extends FilterOutputStream implements
        JobStatus {
    private State state = State.NEW;
    private boolean closed = false;

    @SuppressWarnings("serial")
    public static class StreamStoppedException extends RuntimeException {
        StreamStoppedException() {
            super("Stopped by " + SuspendableOutputStream.class.getSimpleName());
        }
    }

    public SuspendableOutputStream(OutputStream os) {
        super(os);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkWritePermission();
        super.write(b, off, len);
    }

    @Override
    public void write(byte[] b) throws IOException {
        checkWritePermission();
        super.write(b);
    }

    @Override
    public void write(int b) throws IOException {
        checkWritePermission();
        super.write(b);
    }

    @Override
    public void close() throws IOException {
        super.close();
        state = State.DONE;
        closed = true;
    }

    private void checkWritePermission() throws IOException {
        if (closed) {
            throw new IOException("Attempt to write to closed stream");
        }

        if (state == State.STOP_REQUESTED || state == State.STOPPED) {
            state = State.STOPPED;
            // stopped: throw exception to stop stream user
            flush();
            throw new StreamStoppedException();

        } else if (state == State.SUSPEND_REQUESTED || state == State.SUSPENDED) {
            synchronized (this) {
                if (state == State.SUSPEND_REQUESTED
                        || state == State.SUSPENDED)
                    state = State.SUSPENDED;
                try {
                    // suspended: block until resumed
                    while (state == State.SUSPENDED) {
                        wait();
                    }
                } catch (InterruptedException e) {
                    throw (IOException)new IOException(
                            "InterruptedException in checkWritePermission()").initCause(e);
                }
            }
        }
    }

    public State getState() {
        return state;
    }

    /** Only SUSPENDED, STOP, and RUNNING make sense here */
    public synchronized void requestStateChange(State s) {
        boolean illegal = false;

        if (state == State.DONE) {
            // ignore changes
        } else if (s == State.SUSPENDED) {
            if (state == State.NEW || state == State.QUEUED
                    || state == State.RUNNING) {
                state = State.SUSPEND_REQUESTED;
                notify();
            } else if (state == State.SUSPEND_REQUESTED
                    || state == State.SUSPENDED) {
                // ignore change
            } else {
                illegal = true;
            }

        } else if (s == State.STOPPED) {
            if (state == State.NEW || state == State.QUEUED
                    || state == State.RUNNING
                    || state == State.SUSPEND_REQUESTED
                    || state == State.SUSPENDED) {
                state = State.STOP_REQUESTED;
                notify();
            } else if (state == State.STOP_REQUESTED || state == State.STOPPED) {
                // ignore change
            } else {
                illegal = true;
            }

        } else if (s == State.RUNNING) {
            if (state == State.QUEUED 
                    || state == State.SUSPEND_REQUESTED 
                    || state == State.SUSPENDED) {
                state = State.RUNNING;
                notify();
            }

        } else {
            state = s;
            notify();
        }

        if (illegal) {
            throw new IllegalStateException("Illegal state change:" + state
                    + " -> " + s);
        }
    }
    
    /** @inheritDoc */
    public State [] getAllowedHumanStateChanges() {
        return getAllowedStates(state);
    }
    
    static State [] getAllowedStates(State s) {
        State [] result = new State[] {};
        if(s == State.RUNNING) {
            result = new State[] { State.SUSPENDED, State.STOPPED }; 
        } else if(s == State.SUSPEND_REQUESTED || s == State.SUSPENDED) {
            result = new State[] { State.RUNNING, State.STOPPED }; 
        }
        return result;
    }

    /**
     * Not implemented
     * @throws UnsupportedOperationException
     */
    public String getPath() {
        throw new UnsupportedOperationException(
                "getPath() is not applicable to this class");
    }
    
    /**
     * Not implemented
     * @throws UnsupportedOperationException
     */
    public String getStreamPath() {
        throw new UnsupportedOperationException(
                "getStreamPath() is not applicable to this class");
    }

    /**
     * Not implemented
     * @throws UnsupportedOperationException
     */
    public Date getCreationTime() {
        throw new UnsupportedOperationException(
        "getCreationTime() is not applicable to this class");
    }

    /**
     * Not implemented
     * @throws UnsupportedOperationException
     */
    public JobProgressInfo getProgressInfo() {
        throw new UnsupportedOperationException(
        "getProgressInfo() is not applicable to this class");
    }
    
    
}
