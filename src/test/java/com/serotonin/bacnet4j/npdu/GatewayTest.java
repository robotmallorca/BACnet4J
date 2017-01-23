package com.serotonin.bacnet4j.npdu;

import static org.junit.Assert.*;

import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.apdu.APDU;
import com.serotonin.bacnet4j.apdu.UnconfirmedRequest;
import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.service.unconfirmed.WhoIsRequest;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;

public class GatewayTest {
	Address address = new Address(1, NetworkUtils.toOctetString("10"));

	@Test
	public void routerTest() {
		Address remoteAddress = new Address(NetworkUtils.toOctetString("1"));
		NetworkTest network = new NetworkTest(address);
		NetworkTest remoteNetwork100 = new NetworkTest(100, remoteAddress);
		NetworkTest remoteNetwork101 = new NetworkTest(101, remoteAddress);
		NetworkTest invalidRemoteNetwork = new NetworkTest(remoteAddress);
		Transport transport = new DefaultTransport(network);
		LocalDevice localDevice = new LocalDevice(1234, transport);

		try {
			localDevice.initialize();
		} catch(Exception e) {
			e.printStackTrace();
			fail("Unexpected Exception");
		}
		
		try {
			network.addRoute(invalidRemoteNetwork);
			fail("Exception expected");
		} catch(RuntimeException e) {
			assertEquals("Invalid arguments: invalid remote network number 0", e.getMessage());
		} catch(Exception e) {
			e.printStackTrace();
			fail("Incorrect exception type");
		}
		
		assertEquals(1, network.addRoute(remoteNetwork100));
		assertEquals(remoteNetwork100, network.getRouteNetworkFromPort(1));
		assertEquals(remoteNetwork100, network.getRouteNetwork(100));
		network.addRoute(3, remoteNetwork101);
		assertEquals(remoteNetwork101, network.getRouteNetworkFromPort(3));
		assertEquals(remoteNetwork101, network.getRouteNetwork(101));
		assertNull(network.getRouteNetworkFromPort(2));
		assertNull(network.getRouteNetwork(2));
		
		APDU apdu = new UnconfirmedRequest(new WhoIsRequest());

		routeGlobal(network, apdu);
		
		ByteQueue apduData = new ByteQueue();
		apdu.write(apduData);
		
		assertNotNull(remoteNetwork100.getLastRouted());
		System.out.println(remoteNetwork100.getLastRouted());
		assertEquals(apduData.toString(), remoteNetwork100.getLastRouted().getNetworkMessageData().toString());
		
		assertNotNull(remoteNetwork101.getLastRouted());
		System.out.println(remoteNetwork101.getLastRouted());
		assertEquals(apduData.toString(), remoteNetwork100.getLastRouted().getNetworkMessageData().toString());
		
		apdu = new UnconfirmedRequest(new WhoIsRequest(new UnsignedInteger(1), new UnsignedInteger(10)));
		
		routeLocal(network, remoteNetwork100, apdu);
		System.out.println(remoteNetwork100.getLastRouted());
		
		ByteQueue apduData1 = new ByteQueue();
		apdu.write(apduData1);
		assertEquals(apduData1.toString(), remoteNetwork100.getLastRouted().getNetworkMessageData().toString());
		assertEquals(1, remoteNetwork100.getLastRouted().getFrom().getNetworkNumber().intValue());
		assertEquals(apduData.toString(), remoteNetwork101.getLastRouted().getNetworkMessageData().toString());
	}

	private void routeLocal(Network network, Network remoteNetwork, APDU apdu) {
		ByteQueue npdu = new ByteQueue();
		
		NPCI npci = new NPCI(remoteNetwork.getLocalBroadcastAddress(), network.getAllLocalAddresses()[0], apdu.expectsReply());
		
		npci.write(npdu);
		apdu.write(npdu);
		
		network.handleIncomingData(npdu, address.getMacAddress());
	}

	private void routeGlobal(Network network, APDU apdu) {
		ByteQueue npdu = new ByteQueue();
		
		NPCI npci = new NPCI((Address) null);
		
		npci.write(npdu);
		apdu.write(npdu);
		
		network.handleIncomingData(npdu, address.getMacAddress());
	}

}

class NetworkTest extends Network {
	
	private NPDU lastRouted;
	
	class MyNetworkIdentifier extends NetworkIdentifier {
		
		private final int networkNumber;
		
		public MyNetworkIdentifier(int networkNumber) {
			super();
			this.networkNumber = networkNumber;
		}

		@Override
		public String getIdString() {
			return "TestNetwork:" + networkNumber;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + networkNumber;
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
			MyNetworkIdentifier other = (MyNetworkIdentifier) obj;
			if (networkNumber != other.networkNumber)
				return false;
			return true;
		}

	}

    public static final OctetString BROADCAST = new OctetString(new byte[0]);
    private final Address address;
    
	public NetworkTest(Address address) {
		super();
		this.address = address;
	}

	public NetworkTest(int networkNumber, Address address) {
		super(networkNumber);
		this.address = new Address(networkNumber, address.getMacAddress());
	}
	
	
	public NPDU getLastRouted() {
		return lastRouted;
	}

	@Override
	public long getBytesOut() {
		return getBytesOut();
	}

	@Override
	public long getBytesIn() {
		return getBytesIn();
	}

	@Override
	public NetworkIdentifier getNetworkIdentifier() {
		return new MyNetworkIdentifier(getLocalNetworkNumber());
	}

	@Override
	public MaxApduLength getMaxApduLength() {
		return MaxApduLength.UP_TO_1476;
	}

	@Override
	public void terminate() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected OctetString getBroadcastMAC() {
		return BROADCAST;
	}

	@Override
	public Address[] getAllLocalAddresses() {
		return new Address[] {address};
	}

	@Override
	protected void sendNPDU(Address recipient, OctetString router, ByteQueue npdu, boolean broadcast,
			boolean expectsReply) throws BACnetException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected NPDU handleIncomingDataImpl(ByteQueue queue, OctetString linkService) throws Exception {
		return parseNpduData(queue, linkService);
	}

	@Override
	protected void routeImpl(NPDU npdu) throws Exception {
		lastRouted = npdu;
	}
	
	
}