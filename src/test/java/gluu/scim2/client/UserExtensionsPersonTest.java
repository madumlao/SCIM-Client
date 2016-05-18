/*
 * SCIM-Client is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package gluu.scim2.client;

import gluu.BaseScimTest;
import gluu.scim.client.ScimResponse;

import gluu.scim2.client.util.Util;
import org.apache.commons.io.FileUtils;
import org.gluu.oxtrust.model.scim2.*;
import org.gluu.oxtrust.model.scim2.schema.AttributeHolder;
import org.gluu.oxtrust.model.scim2.schema.extension.UserExtensionSchema;
import org.joda.time.DateTime;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;

import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;

import static org.testng.Assert.assertEquals;

/**
 * README:
 *
 * Check first if /install/community-edition-setup/templates/test/scim-client/data/scim-test-data.ldif
 * has been loaded to LDAP.
 *
 * @author Val Pecaoco
 */
public class UserExtensionsPersonTest extends BaseScimTest {

    String domainURL;
    String uid;
    Scim2Client client;
    User personToAdd;
    User personToUpdate;

    String username = "userjson.add2.username";
    String updateDisplayName = "update2.Scim2DisplayName";

    @BeforeTest
    @Parameters({"domainURL", "umaMetaDataUrl", "umaAatClientId", "umaAatClientJwks", "umaAatClientKeyId"})
    public void init(final String domainURL, final String umaMetaDataUrl, final String umaAatClientId, final String umaAatClientJwks, @Optional final String umaAatClientKeyId) throws Exception {

        this.domainURL = domainURL;
        String umaAatClientJwksData = FileUtils.readFileToString(new File(umaAatClientJwks));
        client = Scim2Client.umaInstance(domainURL, umaMetaDataUrl, umaAatClientId, umaAatClientJwksData, umaAatClientKeyId);

        personToAdd = new User();
        personToUpdate = new User();

        personToAdd.setUserName(username);
        personToAdd.setPassword("test");
        personToAdd.setDisplayName("Scim2DisplayName2");
        personToAdd.setActive(true);

        Email email = new Email();
        email.setValue("scim@gluu.org");
        email.setType(org.gluu.oxtrust.model.scim2.Email.Type.WORK);
        email.setPrimary(true);
        personToAdd.getEmails().add(email);

        PhoneNumber phone = new PhoneNumber();
        phone.setType(org.gluu.oxtrust.model.scim2.PhoneNumber.Type.WORK);
        phone.setValue("654-6509-263");
        personToAdd.getPhoneNumbers().add(phone);

        org.gluu.oxtrust.model.scim2.Address address = new org.gluu.oxtrust.model.scim2.Address();
        address.setCountry("US");
        address.setStreetAddress("random street");
        address.setLocality("Austin");
        address.setPostalCode("65672");
        address.setRegion("TX");
        address.setPrimary(true);
        address.setType(org.gluu.oxtrust.model.scim2.Address.Type.WORK);
        address.setFormatted(address.getStreetAddress() + " " + address.getLocality() + " " + address.getPostalCode() + " " + address.getRegion() + " "
                + address.getCountry());
        personToAdd.getAddresses().add(address);

        personToAdd.setPreferredLanguage("US_en");

        org.gluu.oxtrust.model.scim2.Name name = new  org.gluu.oxtrust.model.scim2.Name();
        name.setGivenName("SCIM");
        name.setMiddleName("Test");
        name.setFamilyName("SCIM");
        name.setFormatted("SCIM Test SCIM");
        personToAdd.setName(name);

        // User Extensions
        Extension.Builder extensionBuilder = new Extension.Builder(Constants.USER_EXT_SCHEMA_ID);
        extensionBuilder.setField("scimCustomFirst", "valueOne");
        extensionBuilder.setFieldAsList("scimCustomSecond", Arrays.asList(new String[]{"2016-02-23T03:35:22Z", "2016-02-24T01:52:05Z"}));
        extensionBuilder.setField("scimCustomThird", new BigDecimal(3000));
        personToAdd.addExtension(extensionBuilder.build());
    }

    @Test(groups = "a")
    public void checkIfExtensionsExist() throws Exception {

        UserExtensionSchema userExtensionSchema = client.getUserExtensionSchema();

        assertEquals(userExtensionSchema.getId(), Constants.USER_EXT_SCHEMA_ID);

        boolean customFirstExists = false;
        boolean customSecondExists = false;
        boolean customThirdExists = false;
        for (AttributeHolder attributeHolder : userExtensionSchema.getAttributes()) {

            if (attributeHolder.getName().equals("scimCustomFirst")) {

                customFirstExists = true;
                assert(attributeHolder.getType().equals("string"));
                assert(attributeHolder.getMultiValued().equals(Boolean.FALSE));

            } else if (attributeHolder.getName().equals("scimCustomSecond")) {

                customSecondExists = true;
                assert(attributeHolder.getType().equals("dateTime"));
                assert(attributeHolder.getMultiValued().equals(Boolean.TRUE));

            } else if (attributeHolder.getName().equals("scimCustomThird")) {

                customThirdExists = true;
                assert(attributeHolder.getType().equals("decimal"));
                assert(attributeHolder.getMultiValued().equals(Boolean.FALSE));
            }
        }
        assertEquals(customFirstExists, true, "Custom attribute \"scimCustomFirst\" not found.");
        assertEquals(customSecondExists, true, "Custom attribute \"scimCustomSecond\" not found.");
        assertEquals(customThirdExists, true, "Custom attribute \"scimCustomThird\" not found.");
    }

    @Test(groups = "b", dependsOnGroups = "a")
    public void createPersonTest() throws Exception {

        ScimResponse response = client.createPerson(personToAdd, MediaType.APPLICATION_JSON);

        System.out.println(" createPersonTest() RESPONSE = " + response.getResponseBodyString());

        assertEquals(response.getStatusCode(), 201, "Could not Add the person, status != 201");

        User person = (User) Util.toUser(response, client.getUserExtensionSchema());
        
        this.uid = person.getId();
    }

    @Test(groups = "c", dependsOnGroups = "b")
    public void updatePersonTest() throws Exception {

        personToUpdate = personToAdd;
        personToUpdate.setDisplayName(updateDisplayName);

        // User Extensions
        Extension.Builder extensionBuilder = new Extension.Builder(Constants.USER_EXT_SCHEMA_ID);
        extensionBuilder.setField("scimCustomFirst", "valueUpdated");
        // extensionBuilder.setFieldAsList("scimCustomSecond", Arrays.asList(new String[]{"1969-02-23T03:35:22Z"}));
        extensionBuilder.setFieldAsList("scimCustomSecond", Arrays.asList(new Date[]{(new DateTime("1969-01-02")).toDate(), (new DateTime("1970-02-27")).toDate()}));
        extensionBuilder.setField("scimCustomThird", new BigDecimal(6000));
        personToUpdate.addExtension(extensionBuilder.build());

        ScimResponse response = client.updatePerson(personToUpdate, this.uid, MediaType.APPLICATION_JSON);

        System.out.println(" updatePersonTest() RESPONSE = " + response.getResponseBodyString());

        assertEquals(response.getStatusCode(), 200, "Could not update the person, status != 200");

        User person = (User) Util.toUser(response, client.getUserExtensionSchema());
        
        assertEquals(person.getDisplayName(), updateDisplayName, "could not update the user");
    }

    @Test(groups = "d", dependsOnGroups = "c")
    public void retrievePersonTest() throws Exception {
        ScimResponse response = client.retrievePerson(this.uid, MediaType.APPLICATION_JSON);
        System.out.println(" retrievePersonTest() RESPONSE = "  + response.getResponseBodyString());
        assertEquals(response.getStatusCode(), 200, "Could not get the person, status != 200");
    }

    @Test(dependsOnGroups = "d")
    public void deletePersonTest() throws Exception {
        ScimResponse response = client.deletePerson(this.uid);
        System.out.println(" deletePersonTest() RESPONSE = " + response.getResponseBodyString());
        assertEquals(response.getStatusCode(), 200, "Could not delete the person, status != 200");
    }

    public UserExtensionsPersonTest() {
        super();
    }
}
