/*******************************************************************************
 * Copyright (c) 2013-2015 Sierra Wireless and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *    http://www.eclipse.org/org/documents/edl-v10.html.
 *
 * Contributors:
 *     Sierra Wireless - initial API and implementation
 *******************************************************************************/
package org.eclipse.leshan.server.demo.servlet;
import org.json.*;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.client.Registration;
import org.eclipse.leshan.server.demo.servlet.json.RegistrationSerializer;
import org.eclipse.leshan.server.demo.AvailableLightDiscovery;
import org.eclipse.leshan.server.demo.User;
import org.eclipse.leshan.server.demo.servlet.json.LwM2mNodeDeserializer;
import org.eclipse.leshan.server.demo.servlet.json.LwM2mNodeSerializer;
import org.eclipse.leshan.server.demo.servlet.json.ResponseSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * Service HTTP REST API calls.
 */
public class BrokerServlet extends HttpServlet {

    private static final String FORMAT_PARAM = "format";

    private static final Logger LOG = LoggerFactory.getLogger(BrokerServlet.class);

    private static final long TIMEOUT = 5000; // ms

    private static final long serialVersionUID = 1L;
    private final String FILEPATH = "/home/pi/lightdevices/";


    private final LwM2mServer server;
    private final ClientServlet clients;
    private final Gson gson;

    //private static User[] users;
    private static ArrayList<User> usersList;

    public BrokerServlet(ClientServlet clients, LwM2mServer server, int securePort) {
        this.server = server;
        this.clients = clients;
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeHierarchyAdapter(Registration.class, new RegistrationSerializer(securePort));
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mResponse.class, new ResponseSerializer());
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeSerializer());
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeDeserializer());
        gsonBuilder.setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        this.gson = gsonBuilder.create();

        usersList=new ArrayList<>();
        User admin=new User("Office-Admin-0", 0, "Room-21","admin","admin@tue.nl","pswd");

//        execute the code below when receiving the OWNERSHIPPRIORITY.JSON
//        admin.setLocation(2, 2);
//        double dist = admin.getDistanceFromLocation(1, 5);
        usersList.add(admin);
    }

    public JSONArray findClientType(String type) {
    	Collection<Registration> registrations = server.getRegistrationService().getAllRegistrations();

        String json = this.gson.toJson(registrations.toArray(new Registration[] {}));

        JSONArray j = new JSONArray(json);
        JSONArray response = new JSONArray();
        for(int i = 0; i < j.length(); i++) {
        	JSONObject c = j.getJSONObject(i);
    			String endpoint = c.getString("endpoint");
          if (endpoint.toLowerCase().contains(type)) {
            JSONObject b = new JSONObject();
            b.put("endpoint", endpoint);
            response.put(b);
          }
        }
        return response;
    }

    public void setUsers(String type) {
        JSONArray j = new JSONArray(type);

        for(int i = 0; i < j.length(); i++) {
            JSONObject c = j.getJSONObject(i);
                usersList.add(new User(c.getString("UserID"),c.getInt("GroupNo"),c.getString("RoomID"),
                                  c.getString("Name"),c.getString("Email"),c.getString("Password")));
                //test
                usersList.get(i).setSensor("Sensor-Device-19-1");
        }
        return;
    }


    public void updatePriorityOwnership(HttpServletResponse resp, String endpoint, String url) throws IOException {
    	String content = "{\"id\":12,\"value\":\""+url+"\"}";
    	LwM2mNode node;
        try {
            node = gson.fromJson(content, LwM2mNode.class);
	        WriteRequest request = new WriteRequest(Mode.REPLACE, ContentFormat.fromName("JSON"), "/10250/0/12", node);
			WriteResponse cResponse = server.send(server.getRegistrationService().getByEndpoint(endpoint), request, TIMEOUT);
			String response = null;
	        if (cResponse == null) {
	            LOG.warn(String.format("Request %s timed out.", "/update/"+endpoint));
	            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
	            resp.getWriter().append("Request timeout").flush();
	        } else {
	            response = this.gson.toJson(cResponse);
	            resp.setContentType("application/json");
	            resp.getOutputStream().write(response.getBytes());
	            resp.setStatus(HttpServletResponse.SC_OK);
	        }
        } catch (JsonSyntaxException e) {
            throw new IllegalArgumentException("unable to parse json to tlv:" + e.getMessage(), e);
        }catch (InterruptedException e) {
        	LOG.warn("Invalid request or response", e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().append(e.getMessage()).flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

    	String[] path = StringUtils.split(req.getPathInfo(), '/');
        String typeRequest = path[0];

        if ((typeRequest.equals("lights") || typeRequest.equals("sensors")) && path.length == 1) {
        	JSONArray response = findClientType(typeRequest.substring(0, typeRequest.length()-1));
        	resp.setContentType("application/json");
            resp.getOutputStream().write(response.toString().getBytes("UTF-8"));
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }




        // forward request to the device
        if ((typeRequest.equals("lights") || typeRequest.equals("sensors")) && path.length > 2) {
        	String newPathInfo = StringUtils.substringAfter(req.getPathInfo(), "lights");
        	((Request) req).setPathInfo(newPathInfo);
        	clients.doGet(req, resp);
        	return;
        }

	    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	    resp.getWriter().append("Operation not allowed").flush();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
	@Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	String[] path = StringUtils.split(req.getPathInfo(), '/');
        String typeRequest = path[0];

    	// forward request to the device
    	if ((typeRequest.equals("lights") || typeRequest.equals("sensors")) && path.length > 2) {
        	String newPathInfo = StringUtils.substringAfter(req.getPathInfo(), "lights");
        	((Request) req).setPathInfo(newPathInfo);
        	clients.doPut(req, resp);
        	return;
        }

	    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	    resp.getWriter().append("Operation not allowed").flush();
    }

    /**
     * TODO: BEFORE PUSHING REVERT COMMENT updatePresence
     * 
     * remove the changing of the presence of the user -> acquire from the sensor/broker storage.
     * Used for logging into the client side and changing the presence of the user.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');
        String typeRequest = path[0];

        if (typeRequest.equals("users")) {

        	if(path.length == 1) {
        		this.setUsers(req.getReader().lines().collect(Collectors.joining(System.lineSeparator())));
        		resp.setContentType("application/json");
                String response = "{\"status\":\"SUCCESSFUL\"}";
                resp.getOutputStream().write(response.getBytes("UTF-8"));
                resp.setStatus(HttpServletResponse.SC_OK);
                return;
        	}
        	// Specific Users (Login/update sensor status)
        	if (path.length > 1) {
            	String userID = path[1];
                String data = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
                String[] input = StringUtils.split(data, ':');
                if (input.length >= 2){
        	        if (input[0].equals("login")){
        		        for(int i = 0; i < usersList.size(); i++){
        		        	if(usersList.get(i).UserID.toLowerCase().equals(userID.toLowerCase())){
        		        		Boolean atDesk = AvailableLightDiscovery.userAtDesk(userID);
        		        		Boolean correctLogin = usersList.get(i).checkPassword(input[1]);
        		        		if (correctLogin && atDesk){
        		                    resp.setContentType("text/plain");
        		                    String response = "correctLogin";
        		                    resp.getOutputStream().write(response.getBytes("UTF-8"));
        		                    resp.setStatus(HttpServletResponse.SC_OK);
        		                    return;
        		        		}
        		        		else if (correctLogin && !atDesk){
        		        			resp.setContentType("text/plain");
        		                    String response = "notAtDesk";
        		                    resp.getOutputStream().write(response.getBytes("UTF-8"));
        		                    resp.setStatus(HttpServletResponse.SC_OK);
        		                    return;
        		        		}
        		        		else {
        		        			resp.setStatus(HttpServletResponse.SC_ACCEPTED);
        		        	        resp.getWriter().format("Invalid username or password").flush();
        		        	        return;
        		        		}
        		        	}
        		        }
        	        }
        	        else if (input[0].equals("status")){
        	        	for (int i = 0; i < usersList.size(); i++) {
        	        		if (usersList.get(i).UserID.equals(userID)){
//        	        			usersList.get(i).updatePresenceUser(Boolean.parseBoolean(input[1]));
        	        			resp.setStatus(HttpServletResponse.SC_ACCEPTED);
        	        	        resp.getWriter().format("Status changed").flush();
        	        	        return;
        	        		}
        	        	}
        	        }
                }
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().format("No registered user with id '%s'", userID).flush();

        	}
        }
        if (typeRequest.equals("lights") && path.length > 2 && path[2].equals("update") ) {

        	String dataUpdate = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        	String pathJSON = "/home/pi/lightdevices/"+path[1]+"OwnershipPriority.json";
        	try {

        		FileWriter file = new FileWriter(pathJSON);
        		file.write(dataUpdate);
        		file.close();
        		

            	String newPathInfo = StringUtils.substringAfter(req.getPathInfo(), "lights");
            	newPathInfo = StringUtils.substringBefore(newPathInfo, "update")+"10250/0/12";
            	//((Request) req).setPathInfo(newPathInfo);
            	updatePriorityOwnership(resp,newPathInfo,pathJSON);
        	} catch (IOException e) {
        	   // do something
        	}
        	resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    	    resp.getWriter().append("JSON not valid").flush();
        	return;

        }

		// forward request to the device
		if ((typeRequest.equals("lights") || typeRequest.equals("sensors")) && path.length > 2) {
			String newPathInfo = StringUtils.substringAfter(req.getPathInfo(), "lights");
			((Request) req).setPathInfo(newPathInfo);
			clients.doPost(req, resp);
			return;
		}

	    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	    resp.getWriter().append("Operation not allowed").flush();
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	String[] path = StringUtils.split(req.getPathInfo(), '/');
        String typeRequest = path[0];

        // forward request to the device
    	if ((typeRequest.equals("lights") || typeRequest.equals("sensors")) && path.length > 2) {
        	String newPathInfo = StringUtils.substringAfter(req.getPathInfo(), "lights");
        	((Request) req).setPathInfo(newPathInfo);
        	clients.doDelete(req, resp);
        	return;
        }

	    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	    resp.getWriter().append("Operation not allowed").flush();
    }
}
