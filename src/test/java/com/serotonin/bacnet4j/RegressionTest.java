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

package com.serotonin.bacnet4j;

import static org.junit.Assert.*;

import org.junit.Test;

import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.npdu.VirtualNetworkProvider;
import com.serotonin.bacnet4j.npdu.ip.IpNetwork;
import com.serotonin.bacnet4j.npdu.ip.IpNetworkBuilder;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.transport.SharedQueueTransport;
import com.serotonin.bacnet4j.transport.Transport;

/**
 * @author acladera
 *
 */
public class RegressionTest {

	@Test
	public void issue4Test() throws Exception {
		IpNetwork network = new IpNetworkBuilder().localNetworkNumber(1).reuseAddress(true).build();
//		VirtualNetworkProvider networkProvider = new VirtualNetworkProvider(1, "0", MaxApduLength.UP_TO_1024);
//		networkProvider.initilize();
//		Network network = networkProvider.getVirtualNetwork("1");

		Transport transport = new DefaultTransport(network);
		LocalDevice router = new LocalDevice(100, transport);
		

		
		VirtualNetworkProvider virtualNetworkLink = new VirtualNetworkProvider(10, "0", MaxApduLength.UP_TO_1024);
		virtualNetworkLink.initilize();
		LocalDevice virtualRouter = new LocalDevice(1, new SharedQueueTransport(virtualNetworkLink.getVirtualNetwork("1")));
		LocalDevice virtualDevice2 = new LocalDevice(2, new SharedQueueTransport(virtualNetworkLink.getVirtualNetwork("2")));
		LocalDevice virtualDevice3 = new LocalDevice(3, new SharedQueueTransport(virtualNetworkLink.getVirtualNetwork("3")));
		
		router.getNetwork().addRoute(virtualRouter.getNetwork());
		virtualRouter.getNetwork().addRoute(router.getNetwork());
		
		try {
			router.initialize();
			virtualRouter.initialize();
			virtualDevice2.initialize();
			virtualDevice3.initialize();
			
			virtualDevice2.getNetwork().sendWhoIsRouterToNetwork(virtualDevice2.getNetwork().getLocalBroadcastAddress(), null, true);
			
			Thread.sleep(1000);
			
			IAmEventRecorder recorder1 = new IAmEventRecorder();
			virtualDevice3.getEventHandler().addListener(recorder1);
			
			IAmEventRecorder recorder2 = new IAmEventRecorder();
			router.getEventHandler().addListener(recorder2);
			
			virtualDevice2.sendGlobalBroadcast(virtualDevice2.getIAm());
			
			Thread.sleep(1000);
			
			// Only one message must be routed
			assertEquals(1, recorder1.getCount());
			assertEquals(1, recorder2.getCount());
			
			recorder2.setCount(0);
			router.sendGlobalBroadcast(router.getIAm());
			
			Thread.sleep(1000);
			
			assertEquals(0, recorder2.getCount());
			
		} finally {
			virtualDevice3.terminate();
			virtualDevice2.terminate();
			virtualRouter.terminate();
			router.terminate();
			virtualNetworkLink.terminate();
		}
	}

	class IAmEventRecorder extends DeviceEventAdapter {
		int count = 0;

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}

		@Override
		public void iAmReceived(RemoteDevice d) {
			count++;
		}
	}
}
