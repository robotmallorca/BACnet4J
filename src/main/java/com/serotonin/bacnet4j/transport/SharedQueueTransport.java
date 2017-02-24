/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * When signing a commercial license with Infinite Automation Software,
 * the following extension to GPL is made. A special exception to the GPL is
 * included to allow you to distribute a combined work that includes BAcnet4J
 * without being obliged to provide the source code for any proprietary components.
 *
 * See www.infiniteautomation.com for commercial license options.
 * 
 */

package com.serotonin.bacnet4j.transport;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.npdu.NPDU;
import com.serotonin.bacnet4j.npdu.Network;

/**
 * Class that implements a variant of {@link DefaultTransport} where the message queues of all instances
 * are merged into a single shared queue and processed by a threadpool.
 * 
 * This implementation is suitable for use on applications where a big number of local devices could
 * be instantiated. In this situation, the use of {@link DefaultTransport} is too much resource consuming 
 * as it creates two threads for each local device instance.
 *  
 * @author acladera
 *
 */
public class SharedQueueTransport extends AbstractTransport {
    static final Logger LOG = LoggerFactory.getLogger(SharedQueueTransport.class);
    
    /**
     * Size of the threadpool used for maintenance tasks.
     */
    static final int MAINTENANCE_POOL_SIZE = 5;

	/**
	 * Threadpool used for processing in/out frames
	 */
	private static ExecutorService executor = null;
	/**
	 * Threadpool used for maintenance periodic tasks
	 */
	private static ScheduledExecutorService scheduler = null;
	/**
	 * Counter of active instances of this class. Used for correctly shutdown executor.
	 */
	private static Integer instanceCounter = 0;
	
	private boolean initialized = false;
	private ScheduledFuture<?> scheduledTask;
	
	/**
	 * @param network
	 */
	public SharedQueueTransport(Network network) {
		super(network);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.bacnet4j.transport.AbstractTransport#initializeImpl()
	 */
	@Override
	protected void initializeImpl() throws Exception {
        synchronized (instanceCounter) {
        	if(executor == null) {
        		if(instanceCounter != 0) {
        			LOG.error("Bad value for 'instanceCounter'. Must be 0, actual value = {}", instanceCounter);
        		}
        		executor = Executors.newCachedThreadPool();
        	}
        	if(scheduler == null) {
        		scheduler = Executors.newScheduledThreadPool(MAINTENANCE_POOL_SIZE);
        	}
        	if(!initialized) {
        		instanceCounter++;
        		initialized = true;
        		scheduledTask = scheduler.scheduleWithFixedDelay(new Runnable() {
					
					@Override
					public void run() {
						expire();
					}
				}, timeout, timeout, TimeUnit.MILLISECONDS);
        	}
		}
	}

	/* (non-Javadoc)
	 * @see com.serotonin.bacnet4j.transport.AbstractTransport#terminateImpl()
	 */
	@Override
	protected void terminateImpl() {
		synchronized (instanceCounter) {
			if(initialized) {
				scheduledTask.cancel(false);

				instanceCounter--;
				if(instanceCounter < 0) {
					LOG.error("Bad value for 'instanceCounter'. Must be >= 0, actual value = {}", instanceCounter);
				}
				if((instanceCounter <= 0) && (executor != null)) {
					terminateExecutor(executor);
					LOG.info("Executed {} tasks using a maximum of {} threads", ((ThreadPoolExecutor)executor).getTaskCount(), ((ThreadPoolExecutor)executor).getLargestPoolSize());
					executor = null;
					terminateExecutor(scheduler);
					scheduler = null;
				}
				initialized = false;
			}
		}
	}

	private void terminateExecutor(ExecutorService srv) {
		srv.shutdown();
		try {
			if(!srv.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
				srv.shutdownNow();
			}
		} catch (InterruptedException e) {
			srv.shutdownNow();
		}
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.bacnet4j.transport.AbstractTransport#sendImpl(com.serotonin.bacnet4j.transport.AbstractTransport.Outgoing)
	 */
	@Override
	protected void sendImpl(Outgoing out) {
		if(initialized) {
			final Outgoing outMsg = out;
			executor.execute(new Runnable() {
				
				@Override
				public void run() {
					try {
						outMsg.send();
					} catch(Exception e) {
						LOG.error("Error during send: {}", outMsg, e);
					}
				}
			});
		}
	}

	/* (non-Javadoc)
	 * @see com.serotonin.bacnet4j.transport.Transport#incoming(com.serotonin.bacnet4j.npdu.NPDU)
	 */
	@Override
	public void incoming(NPDU npdu) {
		if(initialized) {
			final NPDU msg = npdu;
			executor.execute(new Runnable() {
				
				@Override
				public void run() {
					try {
						receiveImpl(msg);
					} catch(Exception e) {
						LOG.error("Error during receive: {}", msg, e);
					}
				}
			});
		}
	}

	/* (non-Javadoc)
	 * @see com.serotonin.bacnet4j.transport.AbstractTransport#testCanCreateFuture()
	 */
	@Override
	protected void testCanCreateFuture() {
		// Nothing to be done here. Always can create future
	}
}
