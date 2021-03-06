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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
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

//    private static ArrayList<User> usersList;
    public static HashMap<String, User> usersMap;

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

        usersMap=new HashMap<>();
        User admin=new User("Office-Admin-0", 0, "Room-21","admin","admin@tue.nl","pswd");

//        execute the code below when receiving the OWNERSHIPPRIORITY.JSON
//        admin.setLocation(2, 2);
//        double dist = admin.getDistanceFromLocation(1, 5);
        usersMap.put(admin.UserID,admin);
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

    public void setUsers(String users) {
        JSONArray j = new JSONArray(users);

        for(int i = 0; i < j.length(); i++) {
            JSONObject c = j.getJSONObject(i);
                usersMap.put(c.getString("UserID"),new User(c.getString("UserID"),c.getInt("GroupNo"),c.getString("RoomID"),
                                  c.getString("Name"),c.getString("Email"),c.getString("Password")));
                //test
//                usersList.get(i).setSensor("Sensor-Device-19-1");
        }
        return;
    }

    public void setUserOwnership(String lightID,String usersOwnership) {
    	JSONArray j = new JSONArray(usersOwnership);
    	for(int i = 0; i < j.length(); i++) {
            JSONObject c = j.getJSONObject(i);
            if (c.get("user_id") != null && c.get("user_type") != null && c.get("user_location_x") != null) {
	        	User user = usersMap.get(c.get("user_id"));
	            if (c.get("user_type").equals("USER1")) {
	            	user.setLightUSER1(lightID);
	            	user.setLocation(c.getDouble("user_location_x"), c.getDouble("user_location_y"));
	            	user.setSensor(c.getString("sensor_id"));
	            }
	            else if (c.get("user_type").equals("USER2")) {
	            	user.setLightUSER2(lightID);
	            }
	            user.setLocation(c.getDouble("user_location_x"), c.getDouble("user_location_y"));
            }
    	}

    	return;
    }


    public void updatePriorityOwnership(HttpServletResponse resp, String endpoint, String url) throws IOException {
    	String content = "{\"id\":12,\"value\":\""+url+"\"}";
    	LwM2mNode node;
        try {
            node = gson.fromJson(content, LwM2mNode.class);
	        WriteRequest request = new WriteRequest(Mode.REPLACE, ContentFormat.JSON, "/10250/0/12", node);
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

        if (typeRequest.equals("lights") && path[2].equals("user_type")){

             resp.setContentType("text/plain");
             String response = usersMap.get(path[3]).checkUserType(path[1]);
             resp.getOutputStream().write(response.getBytes("UTF-8"));
             resp.setStatus(HttpServletResponse.SC_OK);
        	return;
        }

        if (typeRequest.equals("lights") && path[2].equals("updateOwnershipPriority")) {

        	try {
        		String content = FileUtils.readFileToString(new File(FILEPATH+path[1]+"/OwnershipPriority.json"));
        		resp.setContentType("application/json");
                resp.getOutputStream().write(content.getBytes("UTF-8"));
                resp.setStatus(HttpServletResponse.SC_OK);
        	} catch (IOException e) {
         	   // do something
         	}
            return;
        }

        // forward request to the device
        if ((typeRequest.equals("lights") || typeRequest.equals("sensors")) && path.length > 2) {
        	String newPathInfo = StringUtils.substringAfter(req.getPathInfo(), typeRequest);
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
        	String newPathInfo = StringUtils.substringAfter(req.getPathInfo(), typeRequest);
        	((Request) req).setPathInfo(newPathInfo);
        	clients.doPut(req, resp);
        	return;
        }

	    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	    resp.getWriter().append("Operation not allowed").flush();
    }

    
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
        	// Specific Users
        	if (path.length > 1) {
            	String userID = path[1];
                String input = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
	        	if(usersMap.containsKey(userID)) {
	        		Boolean atDesk = AvailableLightDiscovery.userAtDesk(userID);
	        		Boolean correctLogin = usersMap.get(userID).checkPassword(input);
	        		if (correctLogin && atDesk){
	                    resp.setContentType("text/plain");
	                    String response = "correctLogin";
	                    resp.getOutputStream().write(response.getBytes("UTF-8"));
	                    resp.setStatus(HttpServletResponse.SC_OK);
	                    return;
	        		}
	        		else if (correctLogin && !atDesk) {
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

                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().format("No registered user with id '%s'", userID).flush();

        	}
        }
        if (typeRequest.equals("lights") && path.length > 2 && path[2].equals("update") ) {

        	String dataUpdate = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        	setUserOwnership(path[1], dataUpdate);
        	String pathJSON = FILEPATH+path[1]+"/OwnershipPriority.json";
        	try {

        		File file = new File(pathJSON);
        		file.getParentFile().mkdirs();
        		FileWriter fileWr = new FileWriter(file);
        		fileWr.write(dataUpdate);
        		fileWr.close();
        		updatePriorityOwnership(resp,path[1],"http://" + InetAddress.getLocalHost().getHostAddress()+":8080/api/broker/lights/"+path[1]+"/updateOwnershipPriority");
            	return;

        	} catch (IOException e) {
        	   // do something
        	}

        	resp.setStatus(HttpServletResponse.SC_OK);
        	return;
        }

		// forward request to the device
		if ((typeRequest.equals("lights") || typeRequest.equals("sensors")) && path.length > 2) {
			String newPathInfo = StringUtils.substringAfter(req.getPathInfo(), typeRequest);
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
        	String newPathInfo = StringUtils.substringAfter(req.getPathInfo(), typeRequest);
        	((Request) req).setPathInfo(newPathInfo);
        	clients.doDelete(req, resp);
        	return;
        }

	    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	    resp.getWriter().append("Operation not allowed").flush();
    }
}
