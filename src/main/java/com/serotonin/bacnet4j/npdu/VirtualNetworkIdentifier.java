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

import com.serotonin.bacnet4j.type.constructed.Address;

/**
 * @author acladera
 *
 */
public class VirtualNetworkIdentifier extends NetworkIdentifier {
	private final Address address;

	
	public VirtualNetworkIdentifier(Address address) {
		super();
		this.address = address;
	}


	/* (non-Javadoc)
	 * @see com.serotonin.bacnet4j.npdu.NetworkIdentifier#getIdString()
	 */
	@Override
	public String getIdString() {
		return "VirtualNetwork " + address.getNetworkNumber() + ":" + address.getMacAddress();
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + address.getNetworkNumber().intValue();
		result = prime * result + address.getMacAddress().hashCode();
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;
		if(obj == null)
			return false;
		if(getClass() != obj.getClass())
			return false;
		VirtualNetworkIdentifier other = (VirtualNetworkIdentifier) obj;
		return other.address.equals(address);
	}
}
