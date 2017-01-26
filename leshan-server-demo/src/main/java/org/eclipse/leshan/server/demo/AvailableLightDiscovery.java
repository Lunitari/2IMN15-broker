package org.eclipse.leshan.server.demo;

import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.Observation;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.client.Registration;
import org.eclipse.leshan.server.client.RegistrationListener;
import org.eclipse.leshan.server.client.RegistrationUpdate;
import org.eclipse.leshan.server.demo.servlet.BrokerServlet;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AvailableLightDiscovery {

	private static final String FORMAT_PARAM = "format";

    private static final Logger LOG = LoggerFactory.getLogger(AvailableLightDiscovery.class);

    public static final String LIGHT_STATE = "/10250/0/2";
    public static final String LIGHT_USER_TYPE = "/10250/0/3";
    public static final String LIGHT_USER_ID = "/10250/0/4";
    public static final String LIGHT_LOCATION_X = "/10250/0/8";
    public static final String LIGHT_LOCATION_Y = "/10250/0/9";
    public static final String SENSOR_STATE = "/10350/0/2";
    public static final String SENSOR_USER_ID = "/10350/0/3";
    public static final String SENSOR_LOCATION_X = "/10250/0/5";
    public static final String SENSOR_LOCATION_Y = "/10250/0/6";

    public final static String obervables = "/10250/0/2;/10250/0/3;/10250/0/4;/10350/0/2;/10350/0/3";
    public final static String locationInfo = "/10250/0/8;/10250/0/9;;/10350/0/5;/10350/0/6";


    private static final long TIMEOUT = 5000; // ms

    public static HashMap<String, HashMap<String, Object>> devices;

    private static BrokerServlet broker;

	private final LwM2mServer server;

	 RegistrationListener registrationListener = new RegistrationListener() {

			@Override
			public void updated(RegistrationUpdate update, Registration updatedRegistration) {}

			@Override
			public void unregistered(Registration registration) {
				// TODO remove device from lists

			}

			@Override
			public void registered(Registration registration) {
				// observe the state of the devices for the Available Light Discovery
				String endpoint = registration.getEndpoint();
				if (endpoint.contains("Light")) {
					try {
		            	ObserveRequest lightState = new ObserveRequest(ContentFormat.JSON, LIGHT_STATE);
		            	ObserveRequest userType = new ObserveRequest(ContentFormat.JSON, LIGHT_USER_TYPE);
		            	ObserveRequest userID = new ObserveRequest(ContentFormat.JSON, LIGHT_USER_ID);
						ObserveResponse cResponse = server.send(registration, lightState, TIMEOUT);
						HashMap<String, Object> resources = (HashMap<String, Object>) (devices.get(endpoint) != null ? devices.get(endpoint) : new HashMap<>());
						if(cResponse != null) {
							resources.put(LIGHT_STATE, (String) ((LwM2mSingleResource) cResponse.getContent()).getValue());
						}
						cResponse = server.send(registration, userType, TIMEOUT);
						if(cResponse != null) {
							resources.put(LIGHT_USER_TYPE, (String) ((LwM2mSingleResource) cResponse.getContent()).getValue());
						}
						cResponse = server.send(registration, userID, TIMEOUT);
						if (cResponse != null) {
							resources.put(LIGHT_USER_ID, (String) ((LwM2mSingleResource) cResponse.getContent()).getValue());
						}
						devices.put(registration.getEndpoint(), resources);
//		            	Set<Observation> tet = server.getObservationService().getObservations(registration);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				if (endpoint.contains("Sensor")) {
					try {
		            	ObserveRequest sensorState = new ObserveRequest(ContentFormat.JSON, SENSOR_STATE);
		            	ObserveRequest userID = new ObserveRequest(ContentFormat.JSON, SENSOR_USER_ID);
						ObserveResponse cResponse = server.send(registration, sensorState, TIMEOUT);
						HashMap<String, Object> resources = (HashMap<String, Object>) (devices.get(endpoint) != null ? devices.get(endpoint) : new HashMap<>());
						if(cResponse != null) {
							resources.put(SENSOR_STATE, (String) ((LwM2mSingleResource) cResponse.getContent()).getValue());
						}
						cResponse = server.send(registration, userID, TIMEOUT);
						if(cResponse != null) {
							resources.put(SENSOR_USER_ID, (String) ((LwM2mSingleResource) cResponse.getContent()).getValue());
						}
						devices.put(registration.getEndpoint(), resources);
		            	//Set<Observation> tet = server.getObservationService().getObservations(registration);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		};

		ObservationListener observationListener = new ObservationListener() {

			@Override
			public void newValue(Observation observation, ObserveResponse response) {
				Registration registration = server.getRegistrationService().getById(observation.getRegistrationId());

	            if (registration != null) {
	            	String path = observation.getPath().toString();
	            	String value = StringUtils.substringBetween(response.getContent().toString(), "value=", ",");
	            	HashMap<String, Object> resources =  devices.containsKey(registration.getEndpoint()) ? devices.get(registration.getEndpoint()) : new HashMap<String, Object>();
	            	resources.put(path, value);
	            	devices.put(registration.getEndpoint(), resources);
	            }
			}

			@Override
			public void newObservation(Observation observation) {}

			@Override
			public void cancelled(Observation observation) {
				//registration is always null so this option does not work !!
//				Registration registration = server.getRegistrationService().getByEndpoint(observation.getRegistrationId());
//				if (registration != null) {
//					String resource = observation.getPath().toString();
//					if(BrokerServlet.obervables.contains(observation.getPath().toString())) {
//						try {
//							ObserveRequest obsReq = new ObserveRequest(ContentFormat.JSON, resource);
//							ObserveResponse cResponse = server.send(registration, obsReq, TIMEOUT);
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
//
//					}
//				}
			}
		};

	public AvailableLightDiscovery(LwM2mServer server) {
		this.server = server;
        devices = new HashMap<>();

		this.server.getRegistrationService().addListener(this.registrationListener);
		this.server.getObservationService().addListener(this.observationListener);
	}

	public void setBroker(BrokerServlet broker) {
		this.broker = broker;
	}

	public static  Boolean userAtDesk(String userID) {
		if(userID.equals("Office-Admin-0")) return true;
		User user = broker.usersMap.get(userID);

		//check if required resources are known
		if(!devices.containsKey(user.getSensor())) return false;
		if(!devices.get(user.getSensor()).containsKey(SENSOR_STATE)) return false;

		return devices.get(user.getSensor()).get(SENSOR_STATE).equals("OCCUPIED");
//		return devices.get(user.getSensor()).get(SENSOR_STATE).equals("USED");
	}

	public Boolean isUserCloserToLight(String userID, String userID2, String endpoint) {
		User user = broker.usersMap.get(userID);
		User user2 = broker.usersMap.get(userID2);

		//check if required resources are known
		if(!devices.containsKey(endpoint)) return false;
		if(!devices.get(endpoint).containsKey(LIGHT_LOCATION_X)) return false;
		if(!devices.get(endpoint).containsKey(LIGHT_LOCATION_Y)) return false;

		if(user.getDistanceFromLocation((double) devices.get(endpoint).get(LIGHT_LOCATION_X), (double) devices.get(endpoint).get(LIGHT_LOCATION_Y)) <
			user2.getDistanceFromLocation((double) devices.get(endpoint).get(LIGHT_LOCATION_X), (double) devices.get(endpoint).get(LIGHT_LOCATION_Y)))
			return true;
		return false;
	}

	public Boolean isAvailabletoUser(String endpoint, String userID) {
		User user = broker.usersMap.get(userID);
		// User is USER1 of this light
		if (user.getLightUSER1().equals(endpoint)) return true;

		//check if required resources are known
		if(!devices.containsKey(endpoint)) return false;
		if(!devices.get(endpoint).containsKey(LIGHT_STATE)) return false;
		if(!devices.get(endpoint).containsKey(LIGHT_USER_TYPE)) return false;


		// User is USER2 && USER1 is not at his desk
		if (user.getLightUSER2().equals(endpoint) && (devices.get(endpoint).get(LIGHT_STATE).equals("FREE") ||
			(devices.get(endpoint).get(LIGHT_STATE).equals("USED") && !devices.get(endpoint).get(LIGHT_USER_TYPE).equals("USER1"))))
			return true;

		//check if required resources are known
		if(!devices.get(endpoint).containsKey(LIGHT_USER_ID)) return false;
		// User is USER3
		if(devices.get(endpoint).get(LIGHT_STATE).equals("FREE") ||
		  (devices.get(endpoint).get(LIGHT_STATE).equals("USED") && devices.get(endpoint).get(LIGHT_USER_TYPE).equals("USER3") &&
		    isUserCloserToLight(userID, (String) devices.get(endpoint).get(LIGHT_USER_ID), endpoint)))
			return true;


		return false;
	}
}
