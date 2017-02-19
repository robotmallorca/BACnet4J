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

package com.serotonin.bacnet4j.npdu;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.util.DiscoveryUtils;

/**
 * @author andreu
 *
 */
public class VirtualNetworkTest {

	protected static final Logger LOG = LoggerFactory.getLogger(GatewayTest.class);
	
	private LocalDevice d1;
	private LocalDevice d2;
	private LocalDevice d3;
	private VirtualNetworkProvider provider = new VirtualNetworkProvider(100, "0", MaxApduLength.UP_TO_1024);
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		d1 = new LocalDevice(1, new DefaultTransport((Network)provider.getVirtualNetwork("1")));
		d2 = new LocalDevice(2, new DefaultTransport((Network)provider.getVirtualNetwork("2")));
		d3 = new LocalDevice(3, new DefaultTransport((Network)provider.getVirtualNetwork("3")));
		
		provider.initilize();
		
		d1.initialize();
		d2.initialize();
		d3.initialize();
		
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		d1.terminate();
		d2.terminate();
		d3.terminate();
		
		provider.terminate();
	}

	/**
	 * Test method for {@link com.serotonin.bacnet4j.npdu.VirtualNetwork#getNetworkIdentifier()}.
	 */
	@Test
	public void testGetNetworkIdentifier() {
		VirtualNetwork network = (VirtualNetwork)d1.getNetwork();
		NetworkIdentifier expected = new VirtualNetworkIdentifier(new Address(100, NetworkUtils.toOctetString("1")));
		
		assertEquals(expected, network.getNetworkIdentifier());
	}

	/**
	 * Test method for {@link com.serotonin.bacnet4j.npdu.VirtualNetwork#getMaxApduLength()}.
	 */
	@Test
	public void testGetMaxApduLength() {
		VirtualNetwork network = (VirtualNetwork)d1.getNetwork();
		MaxApduLength expected = MaxApduLength.UP_TO_1024;
		
		assertEquals(expected, network.getMaxApduLength());
	}

	@Test
	public void sendReceiveTest() throws Exception {
		RemoteDevice rd1;
		
		d2.getEventHandler().addListener(new DeviceEventAdapter() {

			/* (non-Javadoc)
			 * @see com.serotonin.bacnet4j.event.DeviceEventAdapter#iAmReceived(com.serotonin.bacnet4j.RemoteDevice)
			 */
			@Override
			public void iAmReceived(RemoteDevice d) {
				assertEquals(NetworkUtils.toOctetString("1"), d.getAddress().getMacAddress());
			}
			
		});
		
		Thread.sleep(100);
		
		long refcountOut = d1.getBytesOut();
		long refcountIn = d2.getBytesIn();
		
		d1.sendGlobalBroadcast(d1.getIAm());
		
		Thread.sleep(100);
		
		assertEquals(refcountOut + 20, d1.getBytesOut());
		assertEquals(refcountIn + 20, d2.getBytesIn());
		
		rd1 = d2.getRemoteDevice(new Address(NetworkUtils.toOctetString("1")));
		
		assertNotNull(rd1);
		
		DiscoveryUtils.getExtendedDeviceInformation(d2, rd1);
		
		assertEquals(MaxApduLength.UP_TO_1024.getMaxLength(), rd1.getMaxAPDULengthAccepted());
		assertEquals(1, rd1.getInstanceNumber());
		assertEquals("BACnet device",rd1.getName());
		assertEquals(236, rd1.getVendorId());
		
	}
}
