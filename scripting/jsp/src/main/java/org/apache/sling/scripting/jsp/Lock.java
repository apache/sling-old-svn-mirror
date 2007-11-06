/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.scripting.jsp;

/**
 * Lock implementation that allows nested acquiring and releasing.
 * Provides methods to {@link #acquire acquire} and {@link #release release} the
 * lock, {@link #tryAcquire acquire} the lock without blocking and
 * {@link #waitUntilReleased waiting} until the lock becomes free.
 */
final class Lock {

    /** Thread owning this lock */
    private Thread owner;

    /** Lock count */
    private int locks;

    /**
     * Creates a new <code>Lock</code> that is initially ownerless.
     */
    public Lock() {}

    /**
     * Acquires the lock. If the lock is ownerless or this thread already owns
     * this lock, the method returns immediately. Otherwise, this method blocks
     * until the lock becomes free.
     * @exception InterruptedException if the thread was interrupted
     */
    public synchronized final void acquire() throws InterruptedException {
        if (this.owner != Thread.currentThread()) {
            while (this.owner != null) {
                this.wait();
            }
            this.owner = Thread.currentThread();
        }
        this.locks++;
    }

    /**
     * Releases the lock. Marks the lock as free if this was the last lock
     * that was held on this object.
     * @return <code>true</code> if the lock is now ownerless, otherwise
     * <code>false</code>
     */
    public synchronized final boolean release() {
        // if not locked (owned) return immediately
        if (this.owner == null) {
            return true;
        }

        // decrement counter and actually release the lock if the counter
        // drops to zero (or below). Notify waiting users of the lock and
        // return that the lock is free now - may of course already have been
        // acquired in the meantime by another thread
        if (--this.locks <= 0) {
            this.owner = null;
            this.locks = 0;
            this.notifyAll();
            return true;
        }

        // lock count is still positive, so the lock is still held and
        // we return this information
        return false;
    }

    /**
     * Tries to obtain the lock. Returns independent of whether the lock could
     * be acquired or not.
     * @return <code>true</code> if the lock could be obtained,
     *         otherwise <code>false</code>
     */
    public synchronized final boolean tryAcquire() {
        return this.tryAcquire(-1);
    }

    /**
     * Tries to obtain the lock within the given timeout in milliseconds.
     * Returns independent of whether the lock could be acquired or not.
     *
     * @param timeout The maximum number of milliseconds for the lock to become
     *      available. If this number is less than or equal to zero, the method
     *      does not wait for the lock to be free and immediately returns
     *      <code>false</code>.
     *
     * @return <code>true</code> if the lock could be obtained,
     *         otherwise <code>false</code>
     */
    public synchronized final boolean tryAcquire(long timeout) {
        if (this.owner != Thread.currentThread()) {

            // the lock is owned, wait and check again once
            if (this.owner != null) {

                // simply wait for the designated amount of time
                if (timeout > 0) {
                    try {
                        this.wait(timeout);
                    } catch (InterruptedException ie) {
                        // interrupted waiting, don't care and continue
                    }
                }

                // if still (or again) owned after timeout or signal, fail
                if (this.owner != null) {
                    return false;
                }
            }

            // otherwise acquire the lock now
            this.owner = Thread.currentThread();
        }
        this.locks++;
        return true;
    }

    /**
     * Waits for the lock to become free, without acquiring it.
     * @exception InterruptedException if the thread was interrupted
     */
    public synchronized final void waitUntilReleased()
        throws InterruptedException {

        while (this.owner != null) {
            this.wait();
        }
    }

    /**
     * Returns the name of the owning thread of <code>null</code> if this lock
     * is not currently acquired.
     * <p>
     * This method is for informational purpose only and must not be used to
     * implement locking mechanisms better implemented using methods like
     * {@link #tryAcquire()} or {@link #waitUntilReleased()}.
     */
    public synchronized String getOwner() {
        return (this.owner == null) ? null : this.owner.getName();
    }

    /**
     * Returns the number of lock acquisitions done on this lock or zero
     * if this lock is not currently acquired.
     * <p>
     * This method is for informational purpose only and must not be used to
     * implement locking mechanisms better implemented using methods like
     * {@link #tryAcquire()} or {@link #waitUntilReleased()}.
     */
    public synchronized int getNumLocks() {
        return this.locks;
    }

    /**
     * Returns a string representation of this lock, indicating whether this
     * lock is currently locked or not and - if locked - who is the owner of
     * the lock.
     */
    public synchronized String toString() {
        if (this.owner == null) {
            return "Lock: unlocked";
        }

        return "Lock: locked " + this.locks + " times by " + this.owner;
    }
}
