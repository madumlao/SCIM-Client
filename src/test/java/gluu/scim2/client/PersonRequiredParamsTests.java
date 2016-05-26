/*
 * SCIM-Client is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package gluu.scim2.client;

import gluu.BaseScimTest;
import gluu.scim.client.ScimResponse;
import org.apache.commons.io.FileUtils;
import org.gluu.oxtrust.model.scim2.User;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.io.File;

import static org.testng.Assert.assertEquals;

/**
 * @author Val Pecaoco
 */
public class PersonRequiredParamsTests extends BaseScimTest {

    String domainURL;
    Scim2Client client;

    @BeforeTest
    @Parameters({"domainURL", "umaMetaDataUrl", "umaAatClientId", "umaAatClientJwks", "umaAatClientKeyId"})
    public void init(final String domainURL, final String umaMetaDataUrl, final String umaAatClientId, final String umaAatClientJwks, @Optional final String umaAatClientKeyId) throws Exception {
        this.domainURL = domainURL;
        String umaAatClientJwksData = FileUtils.readFileToString(new File(umaAatClientJwks));
        client = Scim2Client.umaInstance(domainURL, umaMetaDataUrl, umaAatClientId, umaAatClientJwksData, umaAatClientKeyId);
    }

    @Test
    public void testPersonRequiredParams() throws Exception {

        User user = new User();

        ScimResponse response = client.createPerson(user, MediaType.APPLICATION_JSON);

        assertEquals(response.getStatusCode(), 500, "Status code is not equal to 500");
        assert response.getResponseBodyString().contains("There are missing required parameters");

        System.out.println("response.getStatusCode() = " + response.getStatusCode());
        System.out.println("response.getResponseBodyString() = " + response.getResponseBodyString());
    }
}