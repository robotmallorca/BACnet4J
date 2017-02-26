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

package com.serotonin.bacnet4j.util.scheduler;

import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Class for creating several Timer like objects implemented through a 
 * shared threadpool.
 *  
 * @author andreu
 *
 */
public class Scheduler {

	public class Timer {
		public void schedule(TimerTask timerTask, long delay) {
			ScheduledFuture<?> scheduled = scheduler.schedule(timerTask, delay, TimeUnit.MILLISECONDS);
			timerTask.setScheduled(scheduled);
		}
		
		public void schedule(TimerTask timerTask, Date date) {
			Date now = new Date();
			schedule(timerTask, date.getTime()-now.getTime());
		}
		
		public void schedule(TimerTask timerTask, long delay, long period) {
			ScheduledFuture<?> scheduled = scheduler.scheduleWithFixedDelay(timerTask, delay, period, TimeUnit.MILLISECONDS);
			timerTask.setScheduled(scheduled);
		}
		
		public void scheduleAtFixedRate(TimerTask timerTask, long delay, long period) {
			ScheduledFuture<?> scheduled = scheduler.scheduleAtFixedRate(timerTask, delay, period, TimeUnit.MILLISECONDS);
			timerTask.setScheduled(scheduled);
		}
	}

	private ScheduledExecutorService scheduler = null;
	private int counter = 0;
	private final int poolSize;
	
	public Scheduler(int poolSize) {
		this.poolSize = poolSize;
	}
	
	synchronized public Timer getTimer() {
		if(scheduler == null) {
			scheduler = Executors.newScheduledThreadPool(poolSize);
		}
		counter++;
		return new Timer();
	}
	
	synchronized public void releaseTimer(Timer timer) {
		counter--;
		if(counter == 0) {
			shutdown();
		}
	}

	private void shutdown() {
		if(scheduler != null) {
			scheduler.shutdown();
			try {
				if(!scheduler.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
					scheduler.shutdownNow();
				}
			} catch(InterruptedException e) {
				scheduler.shutdownNow();
			}
			scheduler = null;
		}
	}
}
