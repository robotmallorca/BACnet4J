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
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

/**
 * Interface for virtual network link objects
 * 
 * @author acladera
 *
 */
public interface VirtualNetworkLink {

	/**
	 * 
	 * @return Returns maximum APDU length supported
	 */
	public MaxApduLength getMaxApduLength();

	/**
	 * @return Returns the BACnet network number for this link
	 */
	public UnsignedInteger getLocalNetwork();

	/**
	 * @return Returns broadcast MAC for this link.
	 */
	public OctetString getBroadcastMAC();

	/**
	 * Register a network object for sending/receiving messages
	 * 
	 * @param virtualNetwork
	 * @throws BACnetException 
	 */
	public void register(VirtualNetwork virtualNetwork) throws BACnetException;

	/**
	 * Unregister a network object for sending/receiving messages
	 * 
	 * @param virtualNetwork
	 */
	public void unregister(VirtualNetwork virtualNetwork);

	/**
	 * Sends NPDU to recipient virtual network objects.
	 * 
	 * @param recipient
	 * @param router
	 * @param npdu
	 * @param broadcast
	 * @param expectsReply
	 */
	public void sendNPDU(Address recipient, OctetString router, ByteQueue npdu, boolean broadcast, boolean expectsReply);
}
