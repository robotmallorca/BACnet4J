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
package com.serotonin.bacnet4j.transport;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.npdu.NPDU;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.util.sero.ThreadUtils;

/**
 * @author Matthew
 */
public class DefaultTransport extends AbstractTransport implements Runnable {
    static final Logger LOG = LoggerFactory.getLogger(DefaultTransport.class);

    // Message queues
    private final Queue<Outgoing> outgoing = new ConcurrentLinkedQueue<Outgoing>();
    private final Queue<NPDU> incoming = new ConcurrentLinkedQueue<NPDU>();

    // Processing
    private Thread thread;
    private volatile boolean running = true;
    private final Object pauseLock = new Object();

    public DefaultTransport(Network network) {
    	super(network);
    }

    @Override
    protected void initializeImpl() throws Exception {
        thread = new Thread(this, "BACnet4J transport");
        thread.start();
    }

    @Override
    protected void terminateImpl() {
        running = false;
        ThreadUtils.notifySync(pauseLock);
        if (thread != null)
            ThreadUtils.join(thread);
    }

    //
    //
    // Adding new requests and responses.
    //
    @Override
    protected void sendImpl(Outgoing out) {
        outgoing.add(out);
        ThreadUtils.notifySync(pauseLock);
    }

    @Override
    protected void testCanCreateFuture() {
    	if(Thread.currentThread() == thread)
            throw new IllegalStateException("Cannot send future request in the transport thread. Use a callback " // 
                    + "call instead, or make this call in a new thread.");
    }
    
    @Override
    public void incoming(NPDU npdu) {
        incoming.add(npdu);
        ThreadUtils.notifySync(pauseLock);
    }


    //
    //
    // Processing
    //
    @Override
    public void run() {
        Outgoing out;
        NPDU in;
        boolean pause;

        while (running) {
            pause = true;

            // Send an outgoing message.
            out = outgoing.poll();
            if (out != null) {
                try {
                    out.send();
                }
                catch (Exception e) {
                    LOG.error("Error during send: {}", out, e);
                }
                pause = false;
            }

            // Receive an incoming message.
            in = incoming.poll();
            if (in != null) {
                try {
                    receiveImpl(in);
                }
                catch (Exception e) {
                    LOG.error("Error during receive: {}", in, e);
                }
                pause = false;
            }

            if (pause && running){
            	try{
            		pause = expire();
            	}catch(Exception e){
            		LOG.error("Error during expire messages: ", e);
			pause = false;
            	}
            }

            if (pause && running)
                ThreadUtils.waitSync(pauseLock, 50);
        }
    }
}
