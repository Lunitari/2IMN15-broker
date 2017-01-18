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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.request.ContentFormat;
import org.eclipse.leshan.core.request.CreateRequest;
import org.eclipse.leshan.core.request.DeleteRequest;
import org.eclipse.leshan.core.request.ExecuteRequest;
import org.eclipse.leshan.core.request.ObserveRequest;
import org.eclipse.leshan.core.request.ReadRequest;
import org.eclipse.leshan.core.request.WriteRequest;
import org.eclipse.leshan.core.request.WriteRequest.Mode;
import org.eclipse.leshan.core.request.exception.RequestFailedException;
import org.eclipse.leshan.core.request.exception.ResourceAccessException;
import org.eclipse.leshan.core.response.CreateResponse;
import org.eclipse.leshan.core.response.DeleteResponse;
import org.eclipse.leshan.core.response.ExecuteResponse;
import org.eclipse.leshan.core.response.LwM2mResponse;
import org.eclipse.leshan.core.response.ObserveResponse;
import org.eclipse.leshan.core.response.ReadResponse;
import org.eclipse.leshan.core.response.WriteResponse;
import org.eclipse.leshan.server.LwM2mServer;
import org.eclipse.leshan.server.client.Registration;
import org.eclipse.leshan.server.demo.servlet.json.RegistrationSerializer;
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

    public final static String obervables = "/10250/0/2;/10250/0/3;/10250/0/4;/10350/0/2;/10350/0/3";
    public final static String locationInfo = "/10250/0/8;/10250/0/9;;/10350/0/5;/10350/0/6";
    public static HashMap<String, HashMap<String, Object>> devices;

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
        devices= new HashMap<>();

        
    //     users = new User[2];
    //     users[0] = new User("Peter", 25, "Hoek, Peter","p.hoek@tue.nl","12345",true);
    //     users[1] = new User("Mark", 22, "Hoek, Mark","m.hoek@tue.nl","54321",false);
    // 
        usersList=new ArrayList<>();
        User admin=new User("Office-Admin-0", 0, "Room-21","admin","admin@tue.nl","pswd");
        admin.updatePresenceUser(true);
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
        
        // test for modifying requests.
        if(typeRequest.equals("update")) {
        	updatePriorityOwnership(resp, path[1], "/api/broker/"+path[1]+"/test.json");
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
        
    	
    	if(typeRequest.equals("update")) {
        	updatePriorityOwnership(resp, path[1], "/api/broker/"+path[1]+"/OwnershipPriority.json");
            return;
        }
    	
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
     * TODO: remove the changing of the presence of the user -> acquire from the sensor/broker storage.
     * Used for logging into the client side and changing the presence of the user.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');
        String typeRequest = path[0];

        if (typeRequest.equals("users")) {
        	
        	if(path.length == 1) {
        		this.setUsers(req.getReader().lines().collect(Collectors.joining(System.lineSeparator())));
        		resp.setContentType("text/plain");
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
        		        		int correctLogin = usersList.get(i).checkLogin(input[1]);
        		        		if (correctLogin == 1){
        		                    resp.setContentType("text/plain");
        		                    String response = "correctLogin";
        		                    resp.getOutputStream().write(response.getBytes("UTF-8"));
        		                    resp.setStatus(HttpServletResponse.SC_OK);
        		                    return;
        		        		}
        		        		else if (correctLogin == 2){
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
        	        			usersList.get(i).updatePresenceUser(Boolean.parseBoolean(input[1]));
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
        	
        	// block observe delete requests
            if ("observe".equals(path[path.length - 1]) && obervables.contains(StringUtils.substringBetween(req.getPathInfo(), path[1], "/"+path[path.length - 1]))) {
            	resp.setStatus(HttpServletResponse.SC_OK);
            	return;
            }
        	((Request) req).setPathInfo(newPathInfo);
        	clients.doDelete(req, resp);
        	return;
        }
    	
	    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	    resp.getWriter().append("Operation not allowed").flush();
    }
}
