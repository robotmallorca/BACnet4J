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

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.ThreadUtils;

/**
 * Class used as factory for VirtualNetwork objects and link implementation between this objects.
 * 
 * @author acladera
 *
 */
public class VirtualNetworkProvider implements VirtualNetworkLink, Runnable {

	private final MaxApduLength maxApduLength;
	private final Unsigned16 localNetworkNumber;
	private final OctetString broadcastMAC;
	private final Map<OctetString, VirtualNetwork> registry = new ConcurrentHashMap<OctetString, VirtualNetwork>();
	private final Queue<MessageData> queue = new ConcurrentLinkedQueue<MessageData>();
    private volatile boolean running = true;
	private Thread thread;

	private final Logger LOG = LoggerFactory.getLogger(this.getClass());
	
	
	/**
	 * VirtualNetwork factory method.
	 * 
	 * @param macAddress
	 * @return Returns a new VirtualNetwork object attached to this link
	 */
	public VirtualNetwork getVirtualNetwork(OctetString macAddress) {
		return new VirtualNetwork(this, new Address(localNetworkNumber, macAddress));
	}

	/**
	 * VirtualNetwork factory method.
	 * 
	 * @param macAddress
	 * @return Returns a new VirtualNetwork object attached to this link
	 */
	public VirtualNetwork getVirtualNetwork(String macAddress) {
		return getVirtualNetwork(VirtualNetworkUtils.toOctetString(macAddress));
	}

	public VirtualNetworkProvider(Unsigned16 localNetworkNumber, OctetString broadcastMAC, 
			MaxApduLength maxApduLength) {
		super();
		this.maxApduLength = maxApduLength;
		this.localNetworkNumber = localNetworkNumber;
		this.broadcastMAC = broadcastMAC;
	}


	public VirtualNetworkProvider(int localNetworkNumber, String broadcastMAC, MaxApduLength maxApduLength) {
		super();
		this.maxApduLength = maxApduLength;
		this.localNetworkNumber = new Unsigned16(localNetworkNumber);
		this.broadcastMAC = VirtualNetworkUtils.toOctetString(broadcastMAC);
	}


	public void initilize() {
		thread = new Thread(this, "BACnet4J virtual network [" + localNetworkNumber + "]");
		thread.start();
	}
	
	public void terminate() {
		running = false;
		ThreadUtils.notifySync(queue);
		if(thread != null) {
			ThreadUtils.join(thread);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.serotonin.bacnet4j.npdu.VirtualNetworkLink#getMaxApduLength()
	 */
	@Override
	public MaxApduLength getMaxApduLength() {
		return maxApduLength;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.bacnet4j.npdu.VirtualNetworkLink#getLocalNetwork()
	 */
	@Override
	public UnsignedInteger getLocalNetwork() {
		return localNetworkNumber;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.bacnet4j.npdu.VirtualNetworkLink#getBroadcastMAC()
	 */
	@Override
	public OctetString getBroadcastMAC() {
		return broadcastMAC;
	}

	/* (non-Javadoc)
	 * @see com.serotonin.bacnet4j.npdu.VirtualNetworkLink#register(com.serotonin.bacnet4j.npdu.VirtualNetwork)
	 */
	@Override
	public void register(VirtualNetwork virtualNetwork) throws BACnetException {
		if(!localNetworkNumber.equals(virtualNetwork.getLocalAddress().getNetworkNumber())) {
			throw new BACnetException("Trying to register a VirtualNetwork with an invalid network number [" + virtualNetwork.getLocalAddress().getNetworkNumber() +"]");
		}
		registry.put(virtualNetwork.getLocalAddress().getMacAddress(), virtualNetwork);
	}

	/* (non-Javadoc)
	 * @see com.serotonin.bacnet4j.npdu.VirtualNetworkLink#unregister(com.serotonin.bacnet4j.npdu.VirtualNetwork)
	 */
	@Override
	public void unregister(VirtualNetwork virtualNetwork) {
		VirtualNetwork removed = registry.remove(virtualNetwork.getLocalAddress().getMacAddress());
		if(removed != null && !removed.equals(virtualNetwork)) {
			LOG.warn("Removed wrong virtual network object: {}", removed);
		}
	}

	/* (non-Javadoc)
	 * @see com.serotonin.bacnet4j.npdu.VirtualNetworkLink#sendNPDU(com.serotonin.bacnet4j.type.constructed.Address, com.serotonin.bacnet4j.type.primitive.OctetString, com.serotonin.bacnet4j.util.sero.ByteQueue, boolean, boolean)
	 */
	@Override
	public void sendNPDU(Address recipient, Address origin, OctetString router, ByteQueue npdu, boolean broadcast,
			boolean expectsReply) {
		queue.add(new MessageData(recipient, origin, router, npdu, broadcast, expectsReply));
		ThreadUtils.notifySync(queue);
	}


	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	@Override
	public void run() {
		while(running) {
			MessageData data = queue.poll();
			
			if(data == null) {
				ThreadUtils.waitSync(queue, 100);
			} else {
				if(data.isBroadcast()) {
					for(VirtualNetwork network : registry.values()) {
						receive(network, data);
					}
				} else {
					VirtualNetwork network = registry.get(data.getDestination());
					if(network != null) {
						receive(network,data);
					}
				}
			}
		}
	}
	
	
	/**
	 * @param network
	 * @param data
	 */
	private void receive(VirtualNetwork network, MessageData data) {
		network.handleIncomingData((ByteQueue)data.npdu.clone(), data.getOrigin());
	}

	class MessageData {
		final Address recipient;
		final Address origin;
		final OctetString router;
		final ByteQueue npdu;
		final boolean broadcast;
		final boolean expectsReply;
		
		public MessageData(Address recipient, Address origin, OctetString router, ByteQueue npdu, boolean broadcast,
				boolean expectsReply) {
			super();
			this.recipient = recipient;
			this.origin = origin;
			this.router = router;
			this.npdu = npdu;
			this.broadcast = broadcast;
			this.expectsReply = expectsReply;
		}

		/**
		 * @return Returns 'true' if destination is broadcast
		 */
		public boolean isBroadcast() {
			if(broadcast || recipient == null)
				return true;
			if(recipient.getMacAddress().equals(getBroadcastMAC()) || recipient.equals(Address.GLOBAL))
				return true;
			return false;
		}

		/**
		 * @return Returns destination MAC
		 */
		public OctetString getDestination() {
			if(isBroadcast())
				return getBroadcastMAC();
			if(router != null)
				return router;
			
			return recipient.getMacAddress();
		}

		/**
		 * @return Returns the origin MAC
		 */
		public OctetString getOrigin() {
			return origin.getMacAddress();
		}
		
		
	}

}
