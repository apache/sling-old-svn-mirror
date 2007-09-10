/*
 * Copyright 2007 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at 
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
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
        if (owner != Thread.currentThread()) {
            while (owner != null) {
                wait();
            }
            owner = Thread.currentThread();
        }
        locks++;
    }
    
    /**
     * Releases the lock. Marks the lock as free if this was the last lock
     * that was held on this object.
     * @return <code>true</code> if the lock is now ownerless, otherwise
     * <code>false</code>
     */
    public synchronized final boolean release() {
        // if not locked (owned) return immediately
        if (owner == null) {
            return true;
        }

        // decrement counter and actually release the lock if the counter
        // drops to zero (or below). Notify waiting users of the lock and
        // return that the lock is free now - may of course already have been
        // acquired in the meantime by another thread
        if (--locks <= 0) {
            owner = null;
            locks = 0;
            notifyAll();
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
        return tryAcquire(-1);
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
        if (owner != Thread.currentThread()) {
            
            // the lock is owned, wait and check again once
            if (owner != null) {
                
                // simply wait for the designated amount of time
                if (timeout > 0) {
                    try {
                        wait(timeout);
                    } catch (InterruptedException ie) {
                        // interrupted waiting, don't care and continue
                    }
                }
                
                // if still (or again) owned after timeout or signal, fail
                if (owner != null) {
                    return false; 
                }
            }
            
            // otherwise acquire the lock now
            owner = Thread.currentThread();
        }
        locks++;
        return true;
    }

    /**
     * Waits for the lock to become free, without acquiring it.
     * @exception InterruptedException if the thread was interrupted
     */
    public synchronized final void waitUntilReleased()
        throws InterruptedException {

        while (owner != null) {
            wait();
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
        return (owner == null) ? null : owner.getName();
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
        return locks;
    }

    /**
     * Returns a string representation of this lock, indicating whether this
     * lock is currently locked or not and - if locked - who is the owner of
     * the lock.
     */
    public synchronized String toString() {
        if (owner == null) {
            return "Lock: unlocked";
        }

        return "Lock: locked " + locks + " times by " + owner;
    }
}
