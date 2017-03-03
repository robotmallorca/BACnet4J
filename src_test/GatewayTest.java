/**
 * 
 */

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.enums.MaxApduLength;
import com.serotonin.bacnet4j.npdu.VirtualNetworkProvider;
import com.serotonin.bacnet4j.npdu.ip.IpNetwork;
import com.serotonin.bacnet4j.npdu.ip.IpNetworkBuilder;
import com.serotonin.bacnet4j.obj.AnalogValueObject;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.transport.SharedQueueTransport;
import com.serotonin.bacnet4j.transport.Transport;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Real;

/**
 * @author acladera
 *
 */
public class GatewayTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		IpNetwork network = new IpNetworkBuilder().localNetworkNumber(1).reuseAddress(true).build();
		Transport transport = new DefaultTransport(network);
		LocalDevice routerDevice = new LocalDevice(100, transport);
		routerDevice.getConfiguration().writeProperty(PropertyIdentifier.objectName, new CharacterString("BACNet Gateway"));

		VirtualNetworkProvider virtualNetworkProvider = new VirtualNetworkProvider(10, "0", MaxApduLength.UP_TO_1024);
		virtualNetworkProvider.initilize();
		LocalDevice inRouterDevice = newDevice(virtualNetworkProvider, 1);
		inRouterDevice.getConfiguration().writeProperty(PropertyIdentifier.objectName, new CharacterString("Virtual Router"));
		LocalDevice virtualDevice = newDevice(virtualNetworkProvider, 2);
		virtualDevice.getConfiguration().writeProperty(PropertyIdentifier.objectName, new CharacterString("Virtual Device"));
		
		routerDevice.getNetwork().addRoute(inRouterDevice.getNetwork());
		inRouterDevice.getNetwork().addRoute(routerDevice.getNetwork());
		
		try {
			routerDevice.initialize();
			inRouterDevice.initialize();
			virtualDevice.initialize();
			
			virtualDevice.getNetwork().sendWhoIsRouterToNetwork(virtualDevice.getNetwork().getLocalBroadcastAddress(), null, true);
			
			float t = 20.0f;
			AnalogValueObject temperature = new AnalogValueObject(0, "analog_test0", t, EngineeringUnits.degreesCelsius, false);
			temperature.supportCovReporting(0.5f);
			temperature.supportCommandable(new Real(20.0f));
			virtualDevice.addObject(temperature);
			
			int input = 0;
			System.out.println("[n]ext, e[x]it...");
			System.out.println("Temp: " + temperature.getProperty(PropertyIdentifier.presentValue));
			while(input != 'x') {
				input = System.in.read();
				if(input == 'n') {
					t = ((Real)temperature.getProperty(PropertyIdentifier.presentValue)).floatValue();
					t += 0.1f;
					temperature.writePropertyImpl(PropertyIdentifier.presentValue, new Real(t));
					System.out.println("Temp: " + temperature.getProperty(PropertyIdentifier.presentValue));
				}
			}

		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			virtualDevice.terminate();
			inRouterDevice.terminate();
			routerDevice.terminate();
			virtualNetworkProvider.terminate();
		}
		System.out.println("Program terminated");
	}

	private static LocalDevice newDevice(VirtualNetworkProvider networkProvider, int id) {
		return new LocalDevice(id, new SharedQueueTransport(networkProvider.getVirtualNetwork(String.valueOf(id))));
	}
}
