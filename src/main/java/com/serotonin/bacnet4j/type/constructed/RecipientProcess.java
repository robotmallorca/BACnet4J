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
 * @author Matthew Lohbihler
 */
package com.serotonin.bacnet4j.type.constructed;

import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class RecipientProcess extends BaseType {
    private static final long serialVersionUID = -3615223495651827907L;
    private final Recipient recipient;
    private final UnsignedInteger processIdentifier;

    public RecipientProcess(Recipient recipient, UnsignedInteger processIdentifier) {
        this.recipient = recipient;
        this.processIdentifier = processIdentifier;
    }

    @Override
    public void write(ByteQueue queue) {
        write(queue, recipient, 0);
        write(queue, processIdentifier, 1);
    }

    public RecipientProcess(ByteQueue queue) throws BACnetException {
        recipient = read(queue, Recipient.class, 0);
        processIdentifier = read(queue, UnsignedInteger.class, 1);
    }

    public Recipient getRecipient() {
        return recipient;
    }

    public UnsignedInteger getProcessIdentifier() {
        return processIdentifier;
    }

    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((processIdentifier == null) ? 0 : processIdentifier.hashCode());
        result = PRIME * result + ((recipient == null) ? 0 : recipient.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final RecipientProcess other = (RecipientProcess) obj;
        if (processIdentifier == null) {
            if (other.processIdentifier != null)
                return false;
        }
        else if (!processIdentifier.equals(other.processIdentifier))
            return false;
        if (recipient == null) {
            if (other.recipient != null)
                return false;
        }
        else if (!recipient.equals(other.recipient))
            return false;
        return true;
    }
}
