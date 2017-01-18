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
import java.util.HashMap;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.leshan.core.node.LwM2mNode;
import org.eclipse.leshan.core.node.LwM2mObjectInstance;
import org.eclipse.leshan.core.node.LwM2mSingleResource;
import org.eclipse.leshan.core.observation.Observation;
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
import org.eclipse.leshan.server.client.RegistrationListener;
import org.eclipse.leshan.server.client.RegistrationUpdate;
import org.eclipse.leshan.server.demo.servlet.json.RegistrationSerializer;
import org.eclipse.leshan.server.demo.AvailableLightDiscovery;
import org.eclipse.leshan.server.demo.servlet.json.LwM2mNodeDeserializer;
import org.eclipse.leshan.server.demo.servlet.json.LwM2mNodeSerializer;
import org.eclipse.leshan.server.demo.servlet.json.ResponseSerializer;
import org.eclipse.leshan.server.observation.ObservationListener;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

/**
 * Service HTTP REST API calls.
 */
public class ClientServlet extends HttpServlet {

    private static final String FORMAT_PARAM = "format";

    private static final Logger LOG = LoggerFactory.getLogger(ClientServlet.class);

    private static final long TIMEOUT = 5000; // ms

    private static final long serialVersionUID = 1L;
    
    private final AvailableLightDiscovery discovery;

    private final LwM2mServer server;

    private final Gson gson;

    public ClientServlet(AvailableLightDiscovery discovery, LwM2mServer server, int securePort) {
        this.server = server;
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeHierarchyAdapter(Registration.class, new RegistrationSerializer(securePort));
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mResponse.class, new ResponseSerializer());
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeSerializer());
        gsonBuilder.registerTypeHierarchyAdapter(LwM2mNode.class, new LwM2mNodeDeserializer());
        gsonBuilder.setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        this.gson = gsonBuilder.create();
        this.discovery = discovery;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    	
    	String pathInfo = req.getPathInfo() == null ? "" : req.getPathInfo();
        // all registered clients
        if (pathInfo.equals("") || pathInfo.contains("/lights")) {
            Collection<Registration> registrations = server.getRegistrationService().getAllRegistrations();
            String json = this.gson.toJson(registrations.toArray(new Registration[] {}));
            if (pathInfo.contains("/lights")) {
            	String user = StringUtils.substringAfter(pathInfo, "/lights/");
	            JSONArray clients = new JSONArray(json);
	            for(int i = clients.length()-1; i >= 0; i--) {
	            	JSONObject c = clients.getJSONObject(i);
	            	if (!c.getString("endpoint").toLowerCase().contains("light")) {
	            		clients.remove(i);
	            	}
	            	else {
	            		Registration registration = server.getRegistrationService().getByEndpoint(c.getString("endpoint"));
	            		String contentFormatParam = "JSON";
	                    ContentFormat contentFormat = contentFormatParam != null
	                            ? ContentFormat.fromName(contentFormatParam.toUpperCase()) : null;

	                    if(!user.equals("Office-Admin-0") && !discovery.isAvailabletoUser(c.getString("endpoint"),user)) {
	                    	clients.remove(i);
	                    }
	            		
	            	}
	            }
	            resp.getOutputStream().write(clients.toString().getBytes("UTF-8"));
            } else {
                resp.getOutputStream().write(json.getBytes("UTF-8"));
            }
            resp.setContentType("application/json");
            resp.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        String[] path = StringUtils.split(req.getPathInfo(), '/');
        if (path.length < 1) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path");
            return;
        }
        String clientEndpoint = path[0];

        // /endPoint : get client
        if (path.length == 1) {
            Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
            if (registration != null) {
                resp.setContentType("application/json");
                resp.getOutputStream().write(this.gson.toJson(registration).getBytes("UTF-8"));
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
            }
            return;
        }

        // /clients/endPoint/LWRequest : do LightWeight M2M read request on a given client.
        try {
            String target = StringUtils.removeStart(req.getPathInfo(), "/" + clientEndpoint);
            Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
            if (registration != null) {
                // get content format
                String contentFormatParam = req.getParameter(FORMAT_PARAM);
                ContentFormat contentFormat = contentFormatParam != null
                        ? ContentFormat.fromName(contentFormatParam.toUpperCase()) : null;

                // create & process request
                ReadRequest request = new ReadRequest(contentFormat, target);
                ReadResponse cResponse = server.send(registration, request, TIMEOUT);
                processDeviceResponse(req, resp, cResponse);
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().format("No registered client with id '%s'", clientEndpoint).flush();
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid request or response", e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().append(e.getMessage()).flush();
        } catch (ResourceAccessException | RequestFailedException e) {
            LOG.warn(String.format("Error accessing resource %s%s.", req.getServletPath(), req.getPathInfo()), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append(e.getMessage()).flush();
        } catch (InterruptedException e) {
            LOG.warn("Thread Interrupted", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append(e.getMessage()).flush();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');
        String clientEndpoint = path[0];

        // at least /endpoint/objectId/instanceId
        if (path.length < 3) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid path");
            return;
        }

        try {
            String target = StringUtils.removeStart(req.getPathInfo(), "/" + clientEndpoint);
            Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
            if (registration != null) {
                // get content format
                String contentFormatParam = req.getParameter(FORMAT_PARAM);
                ContentFormat contentFormat = contentFormatParam != null
                        ? ContentFormat.fromName(contentFormatParam.toUpperCase()) : null;

                // create & process request
                LwM2mNode node = extractLwM2mNode(target, req);
                
                //get location changes
                if(discovery.locationInfo.contains(target)) {
                	discovery.devices.get(clientEndpoint).put(target, ((LwM2mSingleResource) node).getValue());
                }
                
                WriteRequest request = new WriteRequest(Mode.REPLACE, contentFormat, target, node);
                WriteResponse cResponse = server.send(registration, request, TIMEOUT);
                processDeviceResponse(req, resp, cResponse);
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().format("No registered client with id '%s'", clientEndpoint).flush();
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid request or response", e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().append(e.getMessage()).flush();
        } catch (ResourceAccessException | RequestFailedException e) {
            LOG.warn(String.format("Error accessing resource %s%s.", req.getServletPath(), req.getPathInfo()), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append(e.getMessage()).flush();
        } catch (InterruptedException e) {
            LOG.warn("Thread Interrupted", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append(e.getMessage()).flush();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');
        String clientEndpoint = path[0];

        // /clients/endPoint/LWRequest/observe : do LightWeight M2M observe request on a given client.
        if (path.length >= 4 && "observe".equals(path[path.length - 1])) {
            try {
                String target = StringUtils.substringBetween(req.getPathInfo(), clientEndpoint, "/observe");
                Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
                if (registration != null) {
                    // get content format
                    String contentFormatParam = req.getParameter(FORMAT_PARAM);
                    ContentFormat contentFormat = contentFormatParam != null
                            ? ContentFormat.fromName(contentFormatParam.toUpperCase()) : null;

                    // create & process request
                    ObserveRequest request = new ObserveRequest(contentFormat, target);
                    ObserveResponse cResponse = server.send(registration, request, TIMEOUT);
                    processDeviceResponse(req, resp, cResponse);
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
                }
            } catch (IllegalArgumentException e) {
                LOG.warn("Invalid request or response", e);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().append(e.getMessage()).flush();
            } catch (ResourceAccessException | RequestFailedException e) {
                LOG.warn(String.format("Error accessing resource %s%s.", req.getServletPath(), req.getPathInfo()), e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().append(e.getMessage()).flush();
            } catch (InterruptedException e) {
                LOG.warn("Thread Interrupted", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().append(e.getMessage()).flush();
            }
            return;
        }

        String target = StringUtils.removeStart(req.getPathInfo(), "/" + clientEndpoint);

        // /clients/endPoint/LWRequest : do LightWeight M2M execute request on a given client.
        if (path.length == 4) {
            try {
                Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
                if (registration != null) {
                    ExecuteRequest request = new ExecuteRequest(target, IOUtils.toString(req.getInputStream()));
                    ExecuteResponse cResponse = server.send(registration, request, TIMEOUT);
                    processDeviceResponse(req, resp, cResponse);
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
                }
            } catch (IllegalArgumentException e) {
                LOG.warn("Invalid request or response", e);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().append(e.getMessage()).flush();
            } catch (ResourceAccessException | RequestFailedException e) {
                LOG.warn(String.format("Error accessing resource %s%s.", req.getServletPath(), req.getPathInfo()), e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().append(e.getMessage()).flush();
            } catch (InterruptedException e) {
                LOG.warn("Thread Interrupted", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().append(e.getMessage()).flush();
            }
            return;
        }

        // /clients/endPoint/LWRequest : do LightWeight M2M create request on a given client.
        if (2 <= path.length && path.length <= 3) {
            try {
                Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
                if (registration != null) {
                    // get content format
                    String contentFormatParam = req.getParameter(FORMAT_PARAM);
                    ContentFormat contentFormat = contentFormatParam != null
                            ? ContentFormat.fromName(contentFormatParam.toUpperCase()) : null;

                    // create & process request
                    LwM2mNode node = extractLwM2mNode(target, req);
                    if (node instanceof LwM2mObjectInstance) {
                        CreateRequest request = new CreateRequest(contentFormat, target, (LwM2mObjectInstance) node);
                        CreateResponse cResponse = server.send(registration, request, TIMEOUT);
                        processDeviceResponse(req, resp, cResponse);
                    } else {
                        throw new IllegalArgumentException("payload must contain an object instance");
                    }
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
                }
            } catch (IllegalArgumentException e) {
                LOG.warn("Invalid request or response", e);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().append(e.getMessage()).flush();
            } catch (ResourceAccessException | RequestFailedException e) {
                LOG.warn(String.format("Error accessing resource %s%s.", req.getServletPath(), req.getPathInfo()), e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().append(e.getMessage()).flush();
            } catch (InterruptedException e) {
                LOG.warn("Thread Interrupted", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().append(e.getMessage()).flush();
            }
            return;
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String[] path = StringUtils.split(req.getPathInfo(), '/');
        String clientEndpoint = path[0];

        // /clients/endPoint/LWRequest/observe : cancel observation for the given resource.
        if (path.length >= 4 && "observe".equals(path[path.length - 1])) {
            try {
                String target = StringUtils.substringsBetween(req.getPathInfo(), clientEndpoint, "/observe")[0];
                // block observe delete requests (does still update the client)
                if (discovery.obervables.contains(target)) {
                	resp.setStatus(HttpServletResponse.SC_OK);
                	return;
                }
                Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
                if (registration != null) {
                    server.getObservationService().cancelObservations(registration, target);
                    resp.setStatus(HttpServletResponse.SC_OK);
                } else {
                    resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
                }
            } catch (IllegalArgumentException e) {
                LOG.warn("Invalid request or response", e);
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().append(e.getMessage()).flush();
            } catch (ResourceAccessException | RequestFailedException e) {
                LOG.warn(String.format("Error accessing resource %s%s.", req.getServletPath(), req.getPathInfo()), e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().append(e.getMessage()).flush();
            }
            return;
        }

        // /clients/endPoint/LWRequest/ : delete instance
        try {
            String target = StringUtils.removeStart(req.getPathInfo(), "/" + clientEndpoint);
            Registration registration = server.getRegistrationService().getByEndpoint(clientEndpoint);
            if (registration != null) {
                DeleteRequest request = new DeleteRequest(target);
                DeleteResponse cResponse = server.send(registration, request, TIMEOUT);
                processDeviceResponse(req, resp, cResponse);
            } else {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().format("no registered client with id '%s'", clientEndpoint).flush();
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid request or response", e);
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().append(e.getMessage()).flush();
        } catch (ResourceAccessException | RequestFailedException e) {
            LOG.warn(String.format("Error accessing resource %s%s.", req.getServletPath(), req.getPathInfo()), e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append(e.getMessage()).flush();
        } catch (InterruptedException e) {
            LOG.warn("Interrupted request", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append(e.getMessage()).flush();
        }
    }

    private void processDeviceResponse(HttpServletRequest req, HttpServletResponse resp, LwM2mResponse cResponse)
            throws IOException {
        String response = null;
        if (cResponse == null) {
            LOG.warn(String.format("Request %s%s timed out.", req.getServletPath(), req.getPathInfo()));
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().append("Request timeout").flush();
        } else {
            response = this.gson.toJson(cResponse);
            resp.setContentType("application/json");
            resp.getOutputStream().write(response.getBytes());
            resp.setStatus(HttpServletResponse.SC_OK);
        }
    }

    private LwM2mNode extractLwM2mNode(String target, HttpServletRequest req) throws IOException {
        String contentType = StringUtils.substringBefore(req.getContentType(), ";");
        if ("application/json".equals(contentType)) {
            String content = IOUtils.toString(req.getInputStream(), req.getCharacterEncoding());
            LwM2mNode node;
            try {
                node = gson.fromJson(content, LwM2mNode.class);
            } catch (JsonSyntaxException e) {
                throw new IllegalArgumentException("unable to parse json to tlv:" + e.getMessage(), e);
            }
            return node;
        } else if ("text/plain".equals(contentType)) {
            String content = IOUtils.toString(req.getInputStream(), req.getCharacterEncoding());
            int rscId = Integer.valueOf(target.substring(target.lastIndexOf("/") + 1));
            return LwM2mSingleResource.newStringResource(rscId, content);
        }
        throw new IllegalArgumentException("content type " + req.getContentType() + " not supported");
    }
}
