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
import com.serotonin.bacnet4j.npdu.NPCI;
import com.serotonin.bacnet4j.npdu.NPDU;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.npdu.VirtualNetwork;
import com.serotonin.bacnet4j.npdu.VirtualNetworkLink;
import com.serotonin.bacnet4j.npdu.VirtualNetworkProvider;
import com.serotonin.bacnet4j.npdu.VirtualNetworkUtils;
import com.serotonin.bacnet4j.npdu.ip.IpNetwork;
import com.serotonin.bacnet4j.npdu.ip.IpNetworkBuilder;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.transport.SharedQueueTransport;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.RequestUtils;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

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

	@Test
	public void issue5Test() throws Exception {
		VirtualNetworkProvider networkProvider = new VirtualNetworkProvider(1, "0", MaxApduLength.UP_TO_1024);
		networkProvider.initilize();
		Network network = networkProvider.getVirtualNetwork("1");
		Network testNetwork = new TestNetwork(networkProvider, new Address(1, VirtualNetworkUtils.toOctetString("2")));

		Transport transport = new DefaultTransport(network);
		LocalDevice router = new LocalDevice(100, transport); 
		LocalDevice testDev = new LocalDevice(101, new DefaultTransport(testNetwork));
		

		
		VirtualNetworkProvider virtualNetworkLink = new VirtualNetworkProvider(2, "0", MaxApduLength.UP_TO_1024);
		virtualNetworkLink.initilize();
		LocalDevice virtualRouter = new LocalDevice(1, new SharedQueueTransport(virtualNetworkLink.getVirtualNetwork("1")));
		LocalDevice virtualDevice2 = new LocalDevice(2, new SharedQueueTransport(virtualNetworkLink.getVirtualNetwork("2")));
		LocalDevice virtualDevice3 = new LocalDevice(3, new SharedQueueTransport(new TestNetwork(virtualNetworkLink, new Address(2, VirtualNetworkUtils.toOctetString("3")))));
		
		
		router.getNetwork().addRoute(virtualRouter.getNetwork());
		virtualRouter.getNetwork().addRoute(router.getNetwork());
		
		try {
			router.initialize();
			testDev.initialize();
			virtualRouter.initialize();
			virtualDevice2.initialize();
			virtualDevice3.initialize();
			
			virtualDevice2.getNetwork().sendWhoIsRouterToNetwork(virtualDevice2.getNetwork().getLocalBroadcastAddress(), null, true);
			testDev.getNetwork().sendWhoIsRouterToNetwork(network.getLocalBroadcastAddress(), null, true);
			
			Thread.sleep(1000);
			
			assertNotNull(((DefaultTransport)testDev.getNetwork().getTransport()).getNetworkRouters().get(2));
			
			virtualDevice3.getConfiguration().writeProperty(PropertyIdentifier.objectName, new CharacterString("Test_3"));
			RemoteDevice rd3 = testDev.findRemoteDevice(new Address(2, VirtualNetworkUtils.toOctetString("3")), 3);
			assertNotNull(rd3);

			rd3.setName(RequestUtils.getProperty(testDev, rd3, PropertyIdentifier.objectName).toString());
			assertEquals("Test_3", rd3.getName());

			virtualRouter.getConfiguration().writeProperty(PropertyIdentifier.objectName, new CharacterString("Virtual Router"));
			RemoteDevice rdrouter = testDev.findRemoteDevice(new Address(2, VirtualNetworkUtils.toOctetString("1")), 1);
			assertNotNull(rdrouter);

			rdrouter.setName(RequestUtils.getProperty(testDev, rdrouter, PropertyIdentifier.objectName).toString());
			assertEquals("Virtual Router", rdrouter.getName());

		} finally {
			virtualDevice3.terminate();
			virtualDevice2.terminate();
			virtualRouter.terminate();
			testDev.terminate();
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
	
	class TestNetwork extends VirtualNetwork {

		/**
		 * @param link
		 * @param address
		 */
		public TestNetwork(VirtualNetworkLink link, Address address) {
			super(link, address);
		}

		/* (non-Javadoc)
		 * @see com.serotonin.bacnet4j.npdu.VirtualNetwork#handleIncomingDataImpl(com.serotonin.bacnet4j.util.sero.ByteQueue, com.serotonin.bacnet4j.type.primitive.OctetString)
		 */
		@Override
		protected NPDU handleIncomingDataImpl(ByteQueue queue, OctetString linkService) throws Exception {
			NPCI npci = new NPCI((ByteQueue)queue.clone());
			
			if(npci.hasDestinationInfo()) {
				// We must not receive a msg for us with DNET info
				if(npci.getDestinationNetwork() != Address.ALL_NETWORKS) {
					fail("Received a message with DNET info: " + npci.getDestinationNetwork());
				}
			}
			return super.handleIncomingDataImpl(queue, linkService);
		}
		
		
	}
}
