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

import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * Class that implements a virtual BACnet network for use as a proxy network for virtual BACnet devices
 * 
 * @author acladera
 *
 */
public class VirtualNetwork extends Network {

	protected final VirtualNetworkLink link;
	protected final Address address;
	
    private long bytesOut = 0;
    private long bytesIn = 0;

	
	public VirtualNetwork(VirtualNetworkLink link, Address address) {
		super(link.getLocalNetwork().intValue());
		this.link = link;
		this.address = address;
	}

	@Override
	public long getBytesOut() {
		return bytesOut;
	}

	@Override
	public long getBytesIn() {
		return bytesIn;
	}

	@Override
	public NetworkIdentifier getNetworkIdentifier() {
		return new VirtualNetworkIdentifier(address);
	}

	@Override
	public MaxApduLength getMaxApduLength() {
		return link.getMaxApduLength();
	}

	
	@Override
	public void initialize(Transport transport) throws Exception {
		super.initialize(transport);
		
		transport.setRetries(0);
		
		link.register(this);
	}

	@Override
	public void terminate() {
		link.unregister(this);
	}

	@Override
	protected OctetString getBroadcastMAC() {
		return link.getBroadcastMAC();
	}

	@Override
	public Address[] getAllLocalAddresses() {
		return new Address[] {address};
	}

	@Override
	public Address getLocalAddress() {
		return address;
	}

	@Override
	protected void sendNPDU(Address recipient, OctetString router, ByteQueue npdu, boolean broadcast,
			boolean expectsReply) throws BACnetException {
		link.sendNPDU(recipient, router, npdu, broadcast, expectsReply);
	}

	@Override
	protected NPDU handleIncomingDataImpl(ByteQueue queue, OctetString linkService) throws Exception {
		return parseNpduData(queue, linkService);
	}
}
