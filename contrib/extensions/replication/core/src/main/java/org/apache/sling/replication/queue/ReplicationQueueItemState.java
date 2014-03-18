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
package org.apache.sling.replication.queue;

import java.util.Calendar;

/**
 * the current status of a certain item in a {@link ReplicationQueue}
 */

public class ReplicationQueueItemState {

    private int attempts;

    private ItemState state;

    private Calendar entered;

    public boolean isSuccessful() {
        return ItemState.SUCCEEDED.equals(state);
    }

    public void setSuccessful(boolean successful) {
        state = successful ? ItemState.SUCCEEDED : ItemState.ERROR;
    }

    public int getAttempts() {
        return attempts;
    }

    public void setAttempts(int attempts) {
        this.attempts = attempts;
    }

    public ItemState getItemState() {
        return state;
    }

    public void setItemState(ItemState status) {
        this.state = status;
    }

    @Override
    public String toString() {
        return "{\"attempts\":\"" + attempts + "\",\"" + "successful\":\"" + isSuccessful() + "\",\"" + "state\":\"" + state + "\"}";
    }

    public Calendar getEntered() {
        return entered;
    }

    public void setEntered(Calendar entered) {
        this.entered = entered;
    }

    public enum ItemState {
        QUEUED, // waiting in queue after adding or for restart after failing
        ACTIVE, // job is currently in processing
        SUCCEEDED, // processing finished successfully
        STOPPED, // processing was stopped by a user
        GIVEN_UP, // number of retries reached
        ERROR, // processing signaled CANCELLED or throw an exception
        DROPPED // dropped jobs
    }

}
