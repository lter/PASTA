/*
 *
 * $Date$
 * $Author$
 * $Revision$
 *
 * Copyright 2010 the University of New Mexico.
 *
 * This work was supported by National Science Foundation Cooperative
 * Agreements #DEB-0832652 and #DEB-0936498.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 *
 */

package edu.lternet.pasta.eventmanager;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManagerFactory;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;

import pasta.pasta_lternet_edu.log_entry_0.LogEntry;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClient.BoundRequestBuilder;
import com.ning.http.client.AsyncHttpClientConfig;
import com.sun.jersey.spi.resource.Singleton;

import edu.lternet.pasta.common.EmlPackageId;
import edu.lternet.pasta.common.EmlPackageIdFormat;
import edu.lternet.pasta.common.EmlPackageIdFormat.Delimiter;
import edu.lternet.pasta.common.LogEntryFactory;
import edu.lternet.pasta.common.MethodNameUtility;
import edu.lternet.pasta.common.ResourceDeletedException;
import edu.lternet.pasta.common.ResourceExistsException;
import edu.lternet.pasta.common.ResourceNotFoundException;
import edu.lternet.pasta.common.UserErrorException;
import edu.lternet.pasta.common.WebExceptionFactory;
import edu.lternet.pasta.common.WebResponseFactory;
import edu.lternet.pasta.common.proxy.AuditService;
import edu.lternet.pasta.common.security.access.AccessControllerFactory;
import edu.lternet.pasta.common.security.access.JaxRsHttpAccessController;
import edu.lternet.pasta.common.security.access.UnauthorizedException;
import edu.lternet.pasta.common.security.token.AuthToken;
import edu.lternet.pasta.common.security.token.AuthTokenFactory;
import edu.lternet.pasta.eventmanager.EmlSubscription.SubscriptionBuilder;

/**
 * A JAX-RS RESTful implementation of the <em>event notification</em> component
 * of the Event Manager.
 *
 * @webservicename Event Notifier
 * @baseurl https://event.lternet.edu/eventmanager/event
 */
@Produces(MediaType.TEXT_PLAIN)
@Path("event")
@Singleton  // needs to be singleton because of asynchronous POSTing
public final class EventNotifierResource extends EventManagerResource {
  
    /* 
     * Class variables 
     */

    /**
     * The default connection timeout in milliseconds.
     */
    public static final int CONNECTION_TIMEOUT = 30000;

    /**
     * The default request timeout in milliseconds.
     */
    public static final int REQUEST_TIMEOUT = 30000;

    private static final Logger logger =
                                Logger.getLogger(ConfigurationListener.class);

    private static final String SERVICE_OWNER = "pasta";

    
    /* 
     * Instance variables 
     */

    private final AsyncHttpClient asyncHttpClient;


    /* 
     * Constructors
     */

    /**
     * Constructs a new Event Manager resource with the entity manager
     * factory specified by the configuration listener and the default
     * connection and request timeouts.
     *
     * @see ConfigurationListener#getPersistenceUnit()
     */
    public EventNotifierResource() {
        super();
        asyncHttpClient = makeClient(CONNECTION_TIMEOUT, REQUEST_TIMEOUT);
    }

    
    /**
     * Constructs a new Event Manager resource with the provided entity manager
     * factory, connection timeout, and request timeout (in milliseconds).
     *
     * @param entityManagerFactory provides all entity managers used by this resource.
     * @param connectionTimeout timeout for POST connections.
     * @param requestTimeout timeout for POST requests.
     */
    public EventNotifierResource(EntityManagerFactory entityManagerFactory,
                                 int connectionTimeout,
                                 int requestTimeout) {
        super(entityManagerFactory);
        asyncHttpClient = makeClient(connectionTimeout, requestTimeout);
    }

    
    /* 
     * Class methods
     */

    
    /* 
     * Instance methods
     */

    private AsyncHttpClient makeClient(int connectionTimeout,
                                       int requestTimeout) {

        AsyncHttpClientConfig cf = new AsyncHttpClientConfig.Builder().
                                   setConnectionTimeoutInMs(connectionTimeout).
                                   setRequestTimeoutInMs(requestTimeout).
                                   build();
        return new AsyncHttpClient(cf);
    }

    
    /**
     * Performs appropriate cleanup actions before garbage collection.
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            asyncHttpClient.close();
        } finally {
            super.finalize();
        }
    }

    /**
     * Used to notify the event manager, via an HTTP POST request, that an EML
     * document in PASTA has been modified. Upon notification, the event manager
     * queries its database for all subscriptions matching the specified EML
     * packageId. POST requests are then made (asynchronously) to each of the
     * subscribed URLs with the packageId as the request body.
     * <p>
     * The request headers must contain an authorization token. If the request
     * is successful, an HTTP response with status code 200 'OK' is
     * returned. If the request is unauthorized, based on the
     * content of the authorization token and the current access control rule
     * for event notification, status code 401 'Unauthorized' is returned. If
     * the request contains an error, status code 400 'Bad Request' is
     * returned, with a description of the encountered error.
     * </p>
     *
     * @param httpHeaders
     *            the HTTP request headers containing the authorization token.
     * @param requestBody
     *            the POST request's body, containing XML.
     *
     * @return an HTTP response with an appropriate status code.
     */
    @POST
    @Path("eml/{scope}/{identifier}/{revision}")
    public Response notifyOfEvent(@Context                 HttpHeaders httpHeaders,
                                  @PathParam("scope")      String scope,
                                  @PathParam("identifier") String identifier,
                                  @PathParam("revision")   String revision) {

        EmlPackageIdFormat emlPackageIdFormat = new EmlPackageIdFormat(Delimiter.DOT);
        AuthToken authToken = null;
        String method = MethodNameUtility.methodName();
        LogEntry logEntry = null;
        Response response = null;
        String msg = null, resourceId = null;

        try {
            assertAuthorizedToWrite(httpHeaders, MethodNameUtility.methodName());
            authToken = AuthTokenFactory.makeAuthToken(httpHeaders.getCookies());

            EmlPackageId emlPackageId = parseEmlPackageId(scope, identifier, revision);
            List<EmlSubscription> emlSubscriptionList = getSubscriptions(emlPackageId);

            for (EmlSubscription emlSubscription : emlSubscriptionList) {
                asynchronousNotify(emlPackageIdFormat, emlSubscription, emlPackageId);
            }

            resourceId = emlPackageIdFormat.format(emlPackageId);
            response = Response.ok().build();
            return response;

        } catch (WebApplicationException e) {
            response = e.getResponse();
            msg = e.getMessage();
            return response;

        } finally {
            logEntry = LogEntryFactory.make(getVersionString(), method, authToken, response,
                                         resourceId, msg);
            AuditService.log(logEntry, authToken);
            AuditService.joinAll();
        }
    }

    
    /**
     * <strong>Execute Subscription</strong> operation, specifying the subscriptionId whose URL is to be executed.
     *
     * Used to execute a particular subscription in the event manager, 
     * via an HTTP POST request. Upon notification, the event manager
     * queries its database for the subscription matching the specified
     * subscriptionId. POST requests are then made (asynchronously) to the
     * matching subscription.
     * <p>
     * The request headers must contain an authorization token. If the request
     * is successful, an HTTP response with status code 200 'OK' is
     * returned. If the request is unauthorized, based on the
     * content of the authorization token and the current access control rule
     * for event notification, status code 401 'Unauthorized' is returned. If
     * the request contains an error, status code 400 'Bad Request' is
     * returned, with a description of the encountered error.
     * </p>
     *
     * <h4>Requests:</h4>
     * <table border="1" cellspacing="0" cellpadding="3">
     *   <tr>
     *     <th><b>Message Body</b></th>
     *     <th><b>MIME type</b></th>
     *     <th><b>Sample Request</b></th>
     *   </tr>
     *   <tr>
     *     <td></td>
     *     <td></td>
     *     <td>curl -i -b auth-token=$AUTH_TOKEN_UCARROLL -X POST https://event.lternet.edu/eventmanager/event/eml/120</td>
     *   </tr>
     * </table>
     * 
     * <h4>Responses:</h4>
     * <table border="1" cellspacing="0" cellpadding="3">
     *   <tr>
     *     <th><b>Status</b></th>
     *     <th><b>Reason</b></th>
     *     <th><b>Message Body</b></th>
     *     <th><b>MIME type</b></th>
     *     <th><b>Sample Message Body</b></th>
     *   </tr>
     *   <tr>
     *     <td>200 OK</td>
     *     <td>If the operation is successful</td>
     *     <td></td>
     *     <td><code></code></td>
     *     <td></td>
     *   </tr>
     *   <tr>
     *     <td>400 Bad Request</td>
     *     <td>If the request contains an error, such as a request containing a non-integer subscriptionId value</td>
     *     <td>An error message</td>
     *     <td><code>text/plain</code></td>
     *     <td>The provided subscription ID 'abc' cannot be parsed as an integer.</td>
     *   </tr>
     *   <tr>
     *     <td>401 Unauthorized</td>
     *     <td>If the requesting user is not authorized to execute the specified subscription</td>
     *     <td>An error message</td>
     *     <td><code>text/plain</code></td>
     *     <td></td>
     *   </tr>
     *   <tr>
     *     <td>405 Method Not Allowed</td>
     *     <td>The specified HTTP method is not allowed for the requested resource.
     *     For example, the HTTP method was specified as DELETE but the resource
     *     can only support POST.</td>
     *     <td>An error message</td>
     *     <td><code>text/plain</code></td>
     *     <td></td>
     *   </tr>
     *   <tr>
     *     <td>409 Conflict</td>
     *     <td>If a subscription with the specified subscriptionId had been deleted previously</td>
     *     <td>An error message</td>
     *     <td><code>text/plain</code></td>
     *     <td></td>
     *   </tr>
     *   <tr>
     *     <td>500 Internal Server Error</td>
     *     <td>The server encountered an unexpected condition which prevented 
     *     it from fulfilling the request. For example, a SQL error occurred, 
     *     or an unexpected condition was encountered while processing the request</td>
     *     <td>An error message</td>
     *     <td><code>text/plain</code></td>
     *     <td></td>
     *   </tr>
     * </table>
     * 
     * @param httpHeaders
     *            the HTTP request headers containing the authorization token.
     * @param subscriptionId
     *            the subscription identifier value, e.g. "84"
     *
     * @return an HTTP response with an appropriate status code.
     */
    @POST
    @Path("eml/{subscriptionId}")
  public Response executeSubscription(@Context HttpHeaders httpHeaders,
      @PathParam("subscriptionId") String subscriptionId) {

    AuthToken authToken = null;
    String method = MethodNameUtility.methodName();
    LogEntry logEntry = null;
    Response response = null;
    String entryText = null;

    try {
      assertAuthorizedToWrite(httpHeaders, method);
      authToken = AuthTokenFactory.makeAuthToken(httpHeaders.getCookies());
      EmlSubscription emlSubscription = getSubscription(subscriptionId,
          authToken);
      asynchronousNotify(null, emlSubscription, null);
      response = Response.ok().build();
    }
    catch (IllegalArgumentException e) {
      entryText = e.getMessage();
      response = WebExceptionFactory.makeBadRequest(e).getResponse();
    }
    catch (UnauthorizedException e) {
      entryText = e.getMessage();
      response = WebExceptionFactory.makeUnauthorized(e).getResponse();
    }
    catch (ResourceNotFoundException e) {
      entryText = e.getMessage();
      response = WebExceptionFactory.makeNotFound(e).getResponse();
    }
    catch (ResourceDeletedException e) {
      entryText = e.getMessage();
      response = WebExceptionFactory.makeConflict(e).getResponse();
    }
    catch (ResourceExistsException e) {
      entryText = e.getMessage();
      response = WebExceptionFactory.makeConflict(e).getResponse();
    }
    catch (UserErrorException e) {
      entryText = e.getMessage();
      response = WebResponseFactory.makeBadRequest(e);
    }
    catch (Exception e) {
      entryText = e.getMessage();
      WebApplicationException webApplicationException = 
        WebExceptionFactory.make(Response.Status.INTERNAL_SERVER_ERROR, 
          e, e.getMessage());
      response = webApplicationException.getResponse();
    }
    finally {
      if (response != null) {
        logEntry = LogEntryFactory.make(getVersionString(), method, authToken,
            response, null, entryText);
        AuditService.log(logEntry, authToken);
        AuditService.joinAll();
      }
    }
    
    return response;
  }


    private EmlPackageId parseEmlPackageId(String scope,
                                           String identifier,
                                           String revision) {

        EmlPackageIdFormat emlPackageIdFormat =
            new EmlPackageIdFormat(Delimiter.FORWARD_SLASH);

        try {
            return emlPackageIdFormat.parse(scope, identifier, revision);
        } catch (IllegalArgumentException e) {
            throw WebExceptionFactory.makeBadRequest(e);
        }

    }

    private List<EmlSubscription> getSubscriptions(EmlPackageId emlPackageId) {

        SubscriptionBuilder subscriptionBuilder = new SubscriptionBuilder();
        List<EmlSubscription> emlSubscriptionList = new LinkedList<EmlSubscription>();

        SubscriptionService subscriptionService = getSubscriptionService();

        subscriptionBuilder.setEmlPackageId(emlPackageId);
        emlSubscriptionList.addAll(subscriptionService.getWithPackageIdNulls(subscriptionBuilder));

        String scope = emlPackageId.getScope();
        Integer identifier = emlPackageId.getIdentifier();

        subscriptionBuilder.setEmlPackageId(new EmlPackageId(scope, identifier, null));
        emlSubscriptionList.addAll(subscriptionService.getWithPackageIdNulls(subscriptionBuilder));

        subscriptionBuilder.setEmlPackageId(new EmlPackageId(scope, null, null));
        emlSubscriptionList.addAll(subscriptionService.getWithPackageIdNulls(subscriptionBuilder));

        subscriptionBuilder.setEmlPackageId(new EmlPackageId(null, null, null));
        emlSubscriptionList.addAll(subscriptionService.getWithPackageIdNulls(subscriptionBuilder));

        subscriptionService.close();

        return emlSubscriptionList;
    }

    
    private EmlSubscription getSubscription(String subscriptionId, AuthToken authToken) {
      EmlSubscription emlSubscription = null;
      
      SubscriptionService subscriptionService = getSubscriptionService();
      Long id = parseSubscriptionId(subscriptionId);
      emlSubscription = subscriptionService.get(id, authToken);
      subscriptionService.close();

      return emlSubscription;
    }

    
    private void asynchronousNotify(EmlPackageIdFormat emlPackageIdFormat,
                                    EmlSubscription emlSubscription,
                                    EmlPackageId emlPackageId) {

        PostResponseHandler postResponseHandler = new PostResponseHandler(emlSubscription);

        BoundRequestBuilder boundRequestBuilder =
            asyncHttpClient.preparePost(emlSubscription.getUrl().toString());

        boundRequestBuilder.setHeader("Content-Type", MediaType.TEXT_PLAIN);
        
        if ((emlPackageIdFormat != null) && (emlPackageId != null)) {
          String packageId = emlPackageIdFormat.format(emlPackageId);
          boundRequestBuilder.setBody(packageId);
        }

        try {
            boundRequestBuilder.execute(postResponseHandler);
        } catch (IOException e) {
            logger.error("Subscriber Notification Error", e);
        }
    }

    
    private void assertAuthorizedToWrite(HttpHeaders headers, String serviceMethod) {

        String acr = AccessControlRuleFactory.getServiceAcr(serviceMethod);

        JaxRsHttpAccessController controller =
            AccessControllerFactory.getDefaultHttpAccessController();

        if (controller.canWrite(headers, acr, SERVICE_OWNER)) {
            return;
        }

        String s = "This request is not authorized to notify the event " +
                   "manager of events. Please check your authorization " + 
                   "credentials.";

        throw WebExceptionFactory.make(Response.Status.UNAUTHORIZED, null, s);
    }

}
