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

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.npdu.NetworkUtils;
import com.serotonin.bacnet4j.npdu.VirtualNetworkProvider;
import com.serotonin.bacnet4j.type.constructed.Address;

/**
 * @author acladera
 *
 */
public class SharedQueueTransportTest {
	protected static final Logger LOG = LoggerFactory.getLogger(SharedQueueTransport.class);

	private static final int NUM_DEVICES = 200;

	private LocalDevice devices[] = new LocalDevice[NUM_DEVICES];
	private VirtualNetworkProvider networkProvider = new VirtualNetworkProvider(100, "0", MaxApduLength.UP_TO_1024);

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		networkProvider.initilize();
		for(int i = 0; i < NUM_DEVICES; ++i) {
			devices[i] = new LocalDevice(i+1, new SharedQueueTransport((Network)networkProvider.getVirtualNetwork(String.valueOf(i+1))));
			devices[i].initialize();
		}
		
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
		for(int i = 0; i < NUM_DEVICES; ++i) {
			devices[i].terminate();
		}
		networkProvider.terminate();
	}

	@Test
	public void test() throws InterruptedException {
		RemoteDevice rd1 = null;
		RemoteDevice rd5 = null;
		
		devices[0].sendGlobalBroadcast(devices[0].getIAm());
		Thread.sleep(1000);
		
		rd1 = devices[2].getRemoteDevice(new Address(NetworkUtils.toOctetString("1")));
		try {
			rd5 = devices[2].findRemoteDevice(new Address(NetworkUtils.toOctetString("5")), 5);
		} catch (BACnetException e){
			e.printStackTrace();
			fail("Unexpected Exception");
		}
		
		assertNotNull(rd1);
		assertNotNull(rd5);
	}

}
