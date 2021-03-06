/*
 * SCIM-Client is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2017, Gluu
 */
package gluu.scim2.client.rest.provider;

import gluu.scim2.client.ClientMap;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.ext.Provider;

/**
 * A Client-side filter employed to "inject" the Authorization header to the outgoing request. This filter applies only for
 * requests issued by concrete instances of {@link gluu.scim2.client.AbstractScimClient AbstractScimClient} class.
 * <p>Developers do not need to manipulate this class for their SCIM applications.</p>
 */
/*
 * Created by jgomer on 2017-11-25.
 */
@Provider
public class AuthorizationInjectionFilter implements ClientRequestFilter {

    private Logger logger = LogManager.getLogger(getClass());

    public void filter(ClientRequestContext context){

        String authzHeader=ClientMap.getValue(context.getClient());
        if (StringUtils.isNotEmpty(authzHeader))    //resteasy client is tied to an authz header
            context.getHeaders().putSingle("Authorization", authzHeader);
    }

}
