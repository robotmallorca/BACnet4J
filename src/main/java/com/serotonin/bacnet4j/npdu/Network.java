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
package com.serotonin.bacnet4j.npdu;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.serotonin.bacnet4j.apdu.APDU;
import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.Unsigned16;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

abstract public class Network {
    static final Logger LOG = LoggerFactory.getLogger(Network.class);

    private final int localNetworkNumber;
    private Transport transport;
    // Routing map indexed by networkNumber
    private Map<Integer, Network> routes = new HashMap<Integer, Network>();
    private Vector<Integer> routerPorts = new Vector<Integer>();

    // Network layer message types
    public static final int WHO_IS_ROUTER_TO_NETWORK = 0x00;
    public static final int I_AM_ROUTER_TO_NETWORK = 0x01;
    public static final int I_COULD_BE_ROUTER_TO_NETWORK = 0x02;
    public static final int REJECT_MESSAGE_TO_NETWORK = 0x03;
    public static final int ROUTER_BUSY_TO_NETWORK = 0x04;
    public static final int ROUTER_AVAILABLE_TO_NETWORK = 0x05;
    public static final int INITIALIZE_ROUTING_TABLE = 0x06;
    public static final int INITIALIZE_ROUTING_TABLE_ACK = 0x07;
    public static final int ESTABLISH_CONNECTION_TO_NETWORK = 0x08;
    public static final int DISCONNECT_CONNECTION_TO_NETWORK = 0x09;
    public static final int WHAT_IS_NETWORK_NUMBER = 0x12;
    public static final int NETWORK_NUMBER_IS = 0x13;
    
    public Network() {
        this(0);
    }

    public Network(int localNetworkNumber) {
        this.localNetworkNumber = localNetworkNumber;
    }

    public int getLocalNetworkNumber() {
        return localNetworkNumber;
    }

    public void setTransport(Transport transport) {
        this.transport = transport;
    }

    public Transport getTransport() {
        return transport;
    }

    /**
     * Adds a new route to remote network assigning automatically a port to the new route.
     * 
     * @param remoteNetwork The remote network, remoteNetwork.getLocalNetworkNumber() must return
     *                      the remote network number.
     *                      
     * @return returns the port assigned to the new route. 
     */
    public int addRoute(Network remoteNetwork) {
    	int remoteNetworkNumber = remoteNetwork.getLocalNetworkNumber();
    	int port = -1;
    	if(remoteNetworkNumber > 0 && remoteNetworkNumber != Address.ALL_NETWORKS && remoteNetworkNumber != getLocalNetworkNumber() ) {
    		synchronized (routes) {
    			routes.put(remoteNetworkNumber, remoteNetwork);
    			port = routerPorts.indexOf(remoteNetworkNumber);
    			if(port == -1) {
    				if(routerPorts.size() > 254)
    					throw new RuntimeException("No room for more routes");
    				routerPorts.add(remoteNetworkNumber);
    				port = routerPorts.size();
    			} else {
    				++port;
    			}
			}
    	} else {
    		throw new RuntimeException("Invalid arguments: invalid remote network number " + remoteNetworkNumber);
    	}
    	return port;
    }
    
    /**
     * Adds a new route to remote network.
     * 
     * @param port			Assigned router port.
     * @param remoteNetwork	The remote network, remoteNetwork.getLocalNetworkNumber() must return
     *                      the remote network number.
     */
    public void addRoute(int port, Network remoteNetwork) {
    	if(port < 1 || port > 255)
    		throw new RuntimeException("Invalid arguments: invalid port " + port);

    	int remoteNetworkNumber = remoteNetwork.getLocalNetworkNumber();
    	if(remoteNetworkNumber > 0 && remoteNetworkNumber != Address.ALL_NETWORKS && remoteNetworkNumber != getLocalNetworkNumber() ) {
    		synchronized (routes) {
    			routes.put(remoteNetworkNumber, remoteNetwork);
    			if(port > routerPorts.size()) {
    				routerPorts.setSize(port);
    			}
    			routerPorts.set(port-1, remoteNetworkNumber);
			}
    	} else {
    		throw new RuntimeException("Invalid arguments: invalid remote network number " + remoteNetworkNumber);
    	}
    }
    
    /**
     * 
     * @param remoteNetworkNumber
     * @return Returns the Network object used for routing remoteNetworkNumber or null
     *         if the route don't exists.
     */
    public Network getRouteNetwork(Integer remoteNetworkNumber){
    	if(remoteNetworkNumber == null)
    		return null;
    	synchronized (routes) {
    		return routes.get(remoteNetworkNumber);
		}
    }
    
    /**
     * 
     * @param port
     * @return Returns the Network object assigned to the router port or null 
     *         if the router port don't exists.
     */
    public Network getRouteNetworkFromPort(int port) {
    	synchronized (routes) {
    		if(port > 0 && port <= routerPorts.size() && routerPorts.get(port-1) != null) {
    			return routes.get(routerPorts.get(port-1));
    		} else {
    			return null;
    		}
		}
    }
    
    abstract public long getBytesOut();

    abstract public long getBytesIn();

    abstract public NetworkIdentifier getNetworkIdentifier();

    abstract public MaxApduLength getMaxApduLength();

    public void initialize(Transport transport) throws Exception {
        this.transport = transport;
    }

    abstract public void terminate();

    public final Address getLocalBroadcastAddress() {
        return new Address(localNetworkNumber, getBroadcastMAC());
    }

    abstract protected OctetString getBroadcastMAC();

    abstract public Address[] getAllLocalAddresses();
    
    abstract public Address getLocalAddress();

    public final void sendAPDU(Address recipient, OctetString router, APDU apdu, boolean broadcast)
            throws BACnetException {
        ByteQueue npdu = new ByteQueue();

        NPCI npci = null;
        
        if (recipient.isGlobal()) {
        	// Send to local routes
        	for (Integer networkNum : routerPorts) {
        		Network route = getRouteNetwork(networkNum);
				if(route != null) {
			        LOG.debug("{}{} Route APDU to {}: {}", getLocalNetworkNumber(), getLocalAddress().getMacAddress(), recipient, apdu);
					route.route(getLocalAddress(), recipient, apdu, broadcast);
				}
			}
        	// Prepare for local delivery
            npci = new NPCI((Address) null);
        } else if (isThisNetwork(recipient)) {
            if (router != null)
                throw new RuntimeException("Invalid arguments: router address provided for local recipient " + recipient);
            npci = new NPCI(null, null, apdu.expectsReply());
        }
        else {
            if (router == null || isThisAddress(new Address(router))) {
            	Network localRoute = getRouteNetwork(recipient.getNetworkNumber().intValue());
            	if(localRoute == null) {
            		throw new RuntimeException("Invalid arguments: router address not provided for remote recipient " + recipient);
            	}
		        LOG.debug("{}{} Route APDU to {}: {}", getLocalNetworkNumber(), getLocalAddress().getMacAddress(), recipient, apdu);
            	localRoute.route(getLocalAddress(), recipient, apdu, broadcast);
            } else {
            	npci = new NPCI(recipient, null, apdu.expectsReply());
            }
        }

        if(npci != null) {
	        if (apdu.getNetworkPriority() != null)
	            npci.priority(apdu.getNetworkPriority());
	
	        npci.write(npdu);
	
	        apdu.write(npdu);
	
	        LOG.debug("{}{} Send APDU to {}: {}", getLocalNetworkNumber(), getLocalAddress().getMacAddress(), recipient, npdu);
       		sendNPDU(recipient, router, npdu, broadcast, apdu.expectsReply());
        }
    }

    public final void sendNetworkMessage(Address recipient, OctetString router, int messageType, byte[] msg,
            boolean broadcast, boolean expectsReply) throws BACnetException {
        ByteQueue npdu = new ByteQueue();

        NPCI npci = null;

        if (recipient.isGlobal()) {
        	// Send to local routes
        	if(messageType != 0x13) { // Network-Number-Is is never routed
        		for(Integer networkNum : routerPorts) {
        			Network route = getRouteNetwork(networkNum);
        			if(route != null) {
        		        LOG.debug("{}{} Route NPDU to {}: {}", getLocalNetworkNumber(), getLocalAddress().getMacAddress(), recipient, msg);
        				route.route(getLocalAddress(), recipient, messageType, msg, broadcast, expectsReply);
        			}
        		}
        	}
        	// Prepare for local delivery
            npci = new NPCI(null, null, expectsReply, messageType, 0);
        } else if (isThisNetwork(recipient)) {
            if (router != null)
                throw new RuntimeException("Invalid arguments: router address provided for a local recipient");
            npci = new NPCI(null, null, expectsReply, messageType, 0);
        }
        else {
            if (router == null) {
            	Network localRoute = getRouteNetwork(recipient.getNetworkNumber().intValue());
            	if(localRoute == null) {
            		throw new RuntimeException("Invalid arguments: router address not provided for a remote recipient");
            	}
            	LOG.debug("{}{} Route NPDU to {}: {}", getLocalNetworkNumber(), getLocalAddress().getMacAddress(), recipient, msg);
				localRoute.route(getLocalAddress(), recipient, messageType, msg, broadcast, expectsReply);
            } else {
                npci = new NPCI(recipient, null, expectsReply, messageType, 0);
            }
        }
        
        if(npci != null) {
	        npci.write(npdu);
	
	        // Network message
	        if (msg != null)
	            npdu.push(msg);
	
	        LOG.debug("{}{} Send NPDU to {}: {}", getLocalNetworkNumber(), getLocalAddress().getMacAddress(), recipient, npdu);
        	sendNPDU(recipient, router, npdu, broadcast, expectsReply);
        }
    }

	abstract protected void sendNPDU(Address recipient, OctetString router, ByteQueue npdu, boolean broadcast,
            boolean expectsReply) throws BACnetException;

    protected OctetString getDestination(Address recipient, OctetString link) {
        if (recipient.isGlobal())
            return getLocalBroadcastAddress().getMacAddress();
        if (link != null)
            return link;
        return recipient.getMacAddress();
    }

    public boolean isThisNetwork(Address address) {
        int nn = address.getNetworkNumber().intValue();
        return nn == Address.LOCAL_NETWORK || nn == localNetworkNumber;
    }

    protected void handleIncomingData(ByteQueue queue, OctetString linkService) {
        try {
            NPDU npdu = handleIncomingDataImpl(queue, linkService);
            if (npdu != null) {
                if(isThisAddress(npdu.getFrom())) {
                	// Discard messages received from ourself
                	return;
                }
                LOG.debug("{}{} Received NPDU from {}: {}", getLocalNetworkNumber(), getLocalAddress().getMacAddress(), linkService, npdu);
                Address to = npdu.getTo();
                if(to == null || isThisAddress(to)) {
                	// Message (unicast) for this node
                	getTransport().incoming(npdu);
                } else {
                	// The message must be routed to another network
                	int destNet = to.getNetworkNumber().intValue();
                	if(destNet == Address.ALL_NETWORKS) {
                		int hopcount = npdu.getHopCount();
                		if(hopcount > 0) {
	                		// Broadcast to all routes
                			for(Integer networkNumber:routerPorts) {
                				if((networkNumber != null) && (networkNumber > 0) && 
                					((npdu.getFrom() == null) || (networkNumber != npdu.getFrom().getNetworkNumber().intValue()))) 
                				{
                					// Rewrite from info for messages generated in local network
                					Address from = npdu.getFrom();
                					if((from == null)) {
                						from = getLocalAddress();
                					} else if(from.getNetworkNumber().intValue() == 0) {
                						from = new Address(getLocalNetworkNumber(), from.getMacAddress());
                					}
		                			NPDU forwardNpdu = new NPDU(from, npdu.getTo(), npdu.getLinkService(), (ByteQueue)npdu.getNetworkMessageData().clone(), npdu.getExpectsReply());
		                			forwardNpdu.setHopCount(hopcount-1);
		                	        LOG.debug("{}{} Route NPDU: {}", getLocalNetworkNumber(), getLocalAddress().getMacAddress(), forwardNpdu);
	                				getRouteNetwork(networkNumber).route(to, forwardNpdu, true);
                				}
	                		}
                			// Deliver locally
	                		getTransport().incoming(npdu);
                		}
                	} else {
                		Network route = getRouteNetwork(destNet);
                		if(route != null) {
                			Address route_to = npdu.getTo();
                			if(route.isThisNetwork(route_to)) {
                				route_to = null;
                			}
                			Address route_from = npdu.getFrom();
	            			if(isThisNetwork(route_from)) {
	            				route_from = new Address(getLocalNetworkNumber(),npdu.getFrom().getMacAddress());
	            			}
	            			npdu = new NPDU(route_from, route_to, npdu.getLinkService(), npdu.getNetworkMessageData(), npdu.getExpectsReply());
	            			LOG.debug("{}{} Route NPDU: {}", getLocalNetworkNumber(), getLocalAddress().getMacAddress(), npdu);
	        				route.route(to, npdu, false);
                		} else {
                			LOG.error("Invalid local route for network " + destNet);
                		}
                	}
                }
            }
        }
        catch (Exception e) {
            transport.getLocalDevice().getExceptionDispatcher().fireReceivedException(e);
        }
        catch (Throwable t) {
            transport.getLocalDevice().getExceptionDispatcher().fireReceivedThrowable(t);
        }
    }

    /**
     * 
     * @param address
     * @return Returns true if from is one of our local addresses
     */
	protected boolean isThisAddress(Address address) {
		UnsignedInteger networkNumber = address.getNetworkNumber();
		OctetString mac = address.getMacAddress();
		if(networkNumber == null || networkNumber.intValue() == 0 || networkNumber.intValue() == getLocalNetworkNumber()) {
			if(mac == null)
				return true;
			for(Address localAddress:  getAllLocalAddresses()) {
				if(mac.equals(localAddress.getMacAddress()))
					return true;
			}
		}
		return false;
	}

	abstract protected NPDU handleIncomingDataImpl(ByteQueue queue, OctetString linkService) throws Exception;

	/**
	 * Deliver a NPDU originated from another network
	 * 
	 * @param recipient
	 * @param npdu
	 * @param broadcast
	 */
    protected void route(Address recipient, NPDU npdu, boolean broadcast) {
    	try {
    		routeImpl(recipient, npdu, broadcast);
    	} catch(Exception e) {
    		transport.getLocalDevice().getExceptionDispatcher().fireReceivedException(e);
    	} catch (Throwable t) {
            transport.getLocalDevice().getExceptionDispatcher().fireReceivedThrowable(t);
        }
	}

    protected void route(Address from, Address recipient, APDU apdu, boolean broadcast) {
    	ByteQueue data = new ByteQueue();
    	apdu.write(data);
    	
    	Address to = recipient;
    	if(isThisNetwork(to)) {
    		to = null;
    	}
    	route(recipient, new NPDU(from, to, null, data, apdu.expectsReply()), broadcast);
    }
    
    protected void route(Address from, Address recipient, int messageType, byte[] msg, boolean broadcast,
			boolean expectsReply) {
		ByteQueue data = new ByteQueue();
		
		if(msg != null) {
			data.push(msg);
		}
		Address to = recipient;
		if(isThisNetwork(to)) {
			to = null;
		}
		route(recipient, new NPDU(from, to, null, messageType, data, expectsReply), broadcast);
	}

    /**
     * Remote network proxies can override this method to deliver remote NPDUs to
     * the right destination.
     * 
     * @param recipient
     * @param npdu
     * @param broadcast 
     * @throws Exception
     */
    protected void routeImpl(Address recipient, NPDU npdu, boolean broadcast) throws Exception {
		ByteQueue data = new ByteQueue();
		NPCI npci;
		if(npdu.isNetworkMessage()) {
			npci = new NPCI(npdu.getTo(), npdu.getFrom(), npdu.getExpectsReply(), npdu.getNetworkMessageType(), 0);
		} else {
			npci = new NPCI(npdu.getTo(), npdu.getFrom(), npdu.getExpectsReply());
		}
		npci.write(data);
		npdu.write(data);
		
		sendNPDU(recipient, null, data, broadcast, npdu.getExpectsReply());
	}

	public NPDU parseNpduData(ByteQueue queue, OctetString linkService) throws MessageValidationException {
        // Network layer protocol control information. See 6.2.2
        NPCI npci = new NPCI(queue);
        if (npci.getVersion() != 1)
            throw new MessageValidationException("Invalid protocol version: " + npci.getVersion());

        // Check the destination network number and ignore foreign networks for which we don't have a route  
        Address to = null;
        if (npci.hasDestinationInfo()) {
        	if(npci.isNetworkMessage() && npci.getMessageType() == 0x13) {
        		// Network-Number-Is with DNET/DADR info must be ignored
        		return null;
        	}
            int destNet = npci.getDestinationNetwork();
            if (destNet > 0 && destNet != Address.ALL_NETWORKS && getLocalNetworkNumber() != destNet && getRouteNetwork(destNet) == null) {
            	return null;
            }
            if(getLocalNetworkNumber() != destNet) {
            	OctetString temp = null;
            	if(npci.getDestinationAddress() != null) {
            		temp = new OctetString(npci.getDestinationAddress());
            	}
            	to = new Address(destNet, temp);
            }
        }

        Address from;
        if (npci.hasSourceInfo())
            from = new Address(npci.getSourceNetwork(), npci.getSourceAddress());
        else
            from = new Address(linkService);

        if (isThisNetwork(from))
            linkService = null;

        NPDU npdu;
        if (npci.isNetworkMessage()) {
            // Network message
            npdu = new NPDU(from, to, linkService, npci.getMessageType(), queue, npci.isExpectingReply());
        } else {
	        // APDU message
	        npdu = new NPDU(from, to, linkService, queue, npci.isExpectingReply());
			if(npci.hasDestinationInfo()) {
				npdu.setHopCount(npci.getHopCount());
			}
        }
        return npdu;
    }

	/**
	 * Sends a Who-Is-Router-To-Network network message
	 * 
	 * @param recipient		Recipient address
	 * @param networkNumber	Network number to query for. Can be null for querying all routers
	 * @param broadcast		
	 */
	public void sendWhoIsRouterToNetwork(Address recipient, UnsignedInteger networkNumber, boolean broadcast) {
		ByteQueue data = new ByteQueue();
		
		if (networkNumber != null) {
			data.pushU2B(networkNumber.intValue());
		}
		try {
			sendNetworkMessage(recipient, null, WHO_IS_ROUTER_TO_NETWORK, (data.size() != 0) ? data.popAll():null, broadcast, false);
		} catch (BACnetException e) {
			transport.getLocalDevice().getExceptionDispatcher().fireReceivedException(e);
		}
		
	}
	
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + localNetworkNumber;
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
        Network other = (Network) obj;
        if (localNetworkNumber != other.localNetworkNumber)
            return false;
        return true;
    }

    /**
     * Handles a Who-Is-Router-To-Network request.
     * 
     * @param from
     * @param networkMessageData
     * @throws BACnetException 
     */
	public void handleWhoIsRouter(Address from, ByteQueue data) throws BACnetException {
		UnsignedInteger networkNumber = null;
		if(data != null && data.size() >= 2) {
			networkNumber = new Unsigned16(data.popU2B());
		}
		ByteQueue msgData = new ByteQueue();
		if(networkNumber != null) {
			// Search a route for networkNumber
			for (Integer routedNetwork : routerPorts) {
				if(routedNetwork != null) {
					if(routedNetwork.intValue() == networkNumber.intValue()) {
						msgData.pushU2B(networkNumber.intValue());
						break;
					}
					Network route = getRouteNetwork(routedNetwork);
					OctetString router = route.getTransport().getNetworkRouters().get(networkNumber);
					if(router != null) {
						msgData.pushU2B(networkNumber.intValue());
						break;
					}
				}
			}
			// TODO No router found. Forward query to all routes 
		} else {
			// Collect all routes
			for(Integer routedNetwork : routerPorts) {
				if(routedNetwork != null) {
					msgData.pushU2B(routedNetwork.intValue());
					Network route = getRouteNetwork(routedNetwork);
					if(route.getTransport() == null) {
						LOG.warn("Transport for ({}) is null", route.getLocalAddress());
						continue;
					}
					Iterator<Entry<Integer, OctetString>> it = route.getTransport().getNetworkRouters().entrySet().iterator();
					while(it.hasNext()) {
						Entry<Integer, OctetString> entry = it.next();
						if(entry.getKey().intValue() != getLocalNetworkNumber()) {
							msgData.pushU2B(entry.getKey().intValue());
						}
					}
				}
			}
		}
		
		if(msgData.size() > 0) {
			sendIAmRouterToNetwork(msgData);
		}
	}

	private void sendIAmRouterToNetwork(ByteQueue data) throws BACnetException {
		sendNetworkMessage(getLocalBroadcastAddress(), null, I_AM_ROUTER_TO_NETWORK, data.popAll(), true, false);
	}
}
