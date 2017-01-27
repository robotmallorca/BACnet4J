package com.serotonin.bacnet4j.npdu;

import static org.junit.Assert.*;

import org.junit.Test;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.apdu.APDU;
import com.serotonin.bacnet4j.apdu.UnconfirmedRequest;
import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.service.unconfirmed.WhoIsRequest;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.sero.ByteQueue;
import com.serotonin.bacnet4j.util.sero.ThreadUtils;

public class GatewayTest {

	
	@Test
	public void routerTest() {
		Address address = new Address(1, NetworkUtils.toOctetString("10"));
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
		
		ThreadUtils.sleep(50);
		
		APDU apdu = new UnconfirmedRequest(new WhoIsRequest());

		routeGlobal(network, apdu, address);
		
		ByteQueue apduData = new ByteQueue();
		apdu.write(apduData);
		
		assertNotNull(remoteNetwork100.getLastRouted());
		System.out.println(remoteNetwork100.getLastRouted());
		assertEquals(apduData.toString(), remoteNetwork100.getLastRouted().getNetworkMessageData().toString());
		
		assertNotNull(remoteNetwork101.getLastRouted());
		System.out.println(remoteNetwork101.getLastRouted());
		assertEquals(apduData.toString(), remoteNetwork100.getLastRouted().getNetworkMessageData().toString());
		
		apdu = new UnconfirmedRequest(new WhoIsRequest(new UnsignedInteger(1), new UnsignedInteger(10)));
		
		routeLocal(network, remoteNetwork100, apdu, address);
		System.out.println(remoteNetwork100.getLastRouted());
		
		ByteQueue apduData1 = new ByteQueue();
		apdu.write(apduData1);
		assertEquals(apduData1.toString(), remoteNetwork100.getLastRouted().getNetworkMessageData().toString());
		assertEquals(1, remoteNetwork100.getLastRouted().getFrom().getNetworkNumber().intValue());
		assertEquals(apduData.toString(), remoteNetwork101.getLastRouted().getNetworkMessageData().toString());
	}

	@Test
	public void Dev2DevTest() throws Exception {
		LocalDevice d1 = new LocalDevice(1, new DefaultTransport(new NetworkTest(10,"1")));
		LocalDevice d2 = new LocalDevice(2, new DefaultTransport(new NetworkTest(11,"1")));
		RemoteDevice rd1;
		RemoteDevice rd2;
		
		d1.getNetwork().addRoute(d2.getNetwork());
	
		d1.initialize();
		d2.initialize();
		
		d1.sendGlobalBroadcast(d1.getIAm());
		d2.sendGlobalBroadcast(d2.getIAm());
		
		Thread.sleep(100);
		
		assertNotNull(rd1 = d2.getRemoteDevice(1, 10));
		assertNotNull(rd2 = d1.getRemoteDevice(1, 11));
		
	}
	
	private void routeLocal(Network network, Network remoteNetwork, APDU apdu, Address from) {
		ByteQueue npdu = new ByteQueue();
		
		NPCI npci = new NPCI(remoteNetwork.getLocalBroadcastAddress(), network.getAllLocalAddresses()[0], apdu.expectsReply());
		
		npci.write(npdu);
		apdu.write(npdu);
		
		network.handleIncomingData(npdu, from.getMacAddress());
	}

	private void routeGlobal(Network network, APDU apdu, Address from) {
		ByteQueue npdu = new ByteQueue();
		
		NPCI npci = new NPCI((Address) null);
		
		npci.write(npdu);
		apdu.write(npdu);
		
		network.handleIncomingData(npdu, from.getMacAddress());
	}
}


class NetworkTest extends TestNetwork {
	
	private NPDU lastRouted;
	

	public NetworkTest(Address address) {
		super(address, 10);
	}

	public NetworkTest(int networkNumber, Address address) {
		super(new Address(networkNumber, address.getMacAddress()),10);
	}
	
	public NetworkTest(int networkNumber, String macAddress) {
		super(new Address(networkNumber, NetworkUtils.toOctetString(macAddress)), 10);
	}
	
	public NPDU getLastRouted() {
		return lastRouted;
	}

	@Override
	protected void routeImpl(NPDU npdu) throws Exception {
		System.out.println("routing NPDU: " + npdu);
		lastRouted = npdu;
	}
}