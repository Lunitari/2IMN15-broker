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

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
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
public class UserServlet extends HttpServlet {

    private static final String FORMAT_PARAM = "format";

    private static final Logger LOG = LoggerFactory.getLogger(UserServlet.class);

    private static final long TIMEOUT = 5000; // ms

    private static final long serialVersionUID = 1L;

    private final LwM2mServer server;
    
    private User[] users;

    public UserServlet(LwM2mServer server, int securePort) {
        this.server = server;
      
        users = new User[2];
        users[0] = new User("Peter", 25, "Hoek, Peter","p.hoek@tue.nl","12345",true);
        users[1] = new User("Mark", 22, "Hoek, Mark","m.hoek@tue.nl","54321",false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	// delete operations not allowed.
	    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	    resp.getWriter().append("Operation not allowed").flush();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        

    }

    /**
     * TODO: remove the changing of the presence of the user -> acquire from the sensor/broker storage.
     * Used for logging into the client side and changing the presence of the user.
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');
        String userID = path[0];
        String data = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        String[] input = StringUtils.split(data, ':');
        if (input.length >= 2){
	        if (input[0].equals("login")){
		        for(int i = 0; i < users.length; i++){
		        	if(users[i].UserID.toLowerCase().equals(userID.toLowerCase())){
		        		int correctLogin = users[i].checkLogin(input[1]);
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
	        	for (int i = 0; i < users.length; i++) {
	        		if (users[i].UserID.equals(userID)){
	        			users[i].updatePresenceUser(Boolean.parseBoolean(input[1]));
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

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	// delete operations not allowed.
	    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
	    resp.getWriter().append("Operation not allowed").flush();
    }
}
