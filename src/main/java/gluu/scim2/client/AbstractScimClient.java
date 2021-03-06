/*
 * SCIM-Client is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package gluu.scim2.client;

import gluu.scim2.client.rest.FreelyAccessible;
import gluu.scim2.client.rest.provider.AuthorizationInjectionFilter;
import gluu.scim2.client.rest.provider.ListResponseProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;

import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * The base class for specific SCIM clients.
 * <p>Upon initialization, this class internally creates a RestEasy proxy client based on parameters passed. This proxy
 * is used to invoke the operations the service offers. The exact methods that can be called are driven by the interface
 * class passed in the constructor.</p>
 * <p>When a service method is invoked through an instance obtained by any of the factory methods of
 * {@link gluu.scim2.client.factory.ScimClientFactory ScimClientFactory}, the call is dispatched by the {@link #invoke(Object, Method, Object[]) invoke}
 * method of this class, which properly handles the authorization details in conjuction with the filter
 * {@link gluu.scim2.client.rest.provider.AuthorizationInjectionFilter AuthorizationInjectionFilter}.</p>
 * <p>Concrete subclasses of this class must provide {@link #getAuthenticationHeader() getAuthenticationHeader} and
 * {@link #authorize(Response) authorize} methods that must implement specific ways to obtain access tokens depending
 * on how the SCIM service is being protected.</p>
 * @param <T> The type of the internal RestEasy proxy used by this class. This is the same type that
 * {@link gluu.scim2.client.factory.ScimClientFactory ScimClientFactory} methods return.
 */
/*
 * @author Yuriy Movchan Date: 08/23/2013
 * Re-engineered by jgomer on 2017-09-14.
 */
public abstract class AbstractScimClient<T> implements InvocationHandler, Serializable {

    private static final long serialVersionUID = 9098930517944520482L;

    private Logger logger = LogManager.getLogger(getClass());

    //All method calls using scimService (with exception of close) will return a javax.ws.rs.core.Response object.
    //The underlying data can be read using the readEntity method
    private T scimService;

    private ResteasyClient client;

    AbstractScimClient(String domain, Class<T> serviceClass){
        /*
         Configures a proxy to interact with the service using the new JAX-RS 2.0 Client API, see section
         "Resteasy Proxy Framework" of RESTEasy JAX-RS user guide
         */
        client = new ResteasyClientBuilder().build();
        ResteasyWebTarget target = client.target(domain);

        scimService = target.proxy(serviceClass);
        target.register(ListResponseProvider.class);
        target.register(AuthorizationInjectionFilter.class);

        ClientMap.update(client, null);
    }

    /*
     * The actual call to service methods is done here. Note that the response is buffered so the underlying input
     * stream is fully consumed, so there is no need to create finally blocks with close(). Also, the readEntity method
     * can be called any number of times. For instance, the "raw" response can be inspected by using readEntity(String.class)
     */
    private Response invokeServiceMethod(Method method, Object[] args) throws ReflectiveOperationException{

        logger.trace("Sending service request for method {}", method.getName());
        Response response = (Response) method.invoke(scimService, args);
        logger.trace("Received response entity was{} buffered", response.bufferEntity() ? "" : " not");
        logger.trace("Response status code was {}", response.getStatus());
        return response;

    }

    /**
     * This method is the single point of dispatch for any and all the requests made to the service. It takes care of
     * requesting access tokens when necessary and make them available when requests are bound to be issued.
     * <p>As with all methods of this class and its subclasses, invoke is not called directly by developers: the calls are
     * triggered when the objects returned by factory methods of {@link gluu.scim2.client.factory.ScimClientFactory ScimClientFactory}
     * are manipulated.</p>
     * @return The response associated to the invocation (normally a javax.ws.rs.core.Response instance)
     */
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable{

        String methodName=method.getName();

        if (methodName.equals("close")) {
            logger.info("Closing RestEasy client");
            ClientMap.remove(client);
            return null;
        }
        else{
            Response response;
            FreelyAccessible unprotected=method.getAnnotation(FreelyAccessible.class);

            //Set authorization header if needed
            if (unprotected!=null){
                response = invokeServiceMethod(method, args);
            }
            else{
                ClientMap.update(client, getAuthenticationHeader());
                response = invokeServiceMethod(method, args);

                if (response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()) {
                    if (authorize(response)) {
                        logger.trace("Trying second attempt of request (former received unauthorized response code)");
                        ClientMap.update(client, getAuthenticationHeader());
                        response = invokeServiceMethod(method, args);
                    }
                    else
                        logger.error("Could not get access token for current request: {}", methodName);
                }
            }
            return response;
        }

    }

    abstract String getAuthenticationHeader();

    abstract boolean authorize(Response response);

}
