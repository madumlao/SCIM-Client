/*
 * SCIM-Client is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */
package gluu.scim.client;

import gluu.BaseScimTest;
import gluu.scim.client.model.*;
import gluu.scim2.client.util.Util;
import org.apache.commons.io.FileUtils;
import org.gluu.oxtrust.model.scim2.*;
import org.junit.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.testng.Assert.assertEquals;

/**
 * @author Val Pecaoco
 */
public class EmailSync2Tests extends BaseScimTest {

    String domainURL;
    ScimClient client;

    String id;

    @BeforeTest
    @Parameters({"domainURL", "umaMetaDataUrl", "umaAatClientId", "umaAatClientJwks", "umaAatClientKeyId"})
    public void init(final String domainURL, final String umaMetaDataUrl, final String umaAatClientId, final String umaAatClientJwks, @Optional final String umaAatClientKeyId) throws Exception {
        this.domainURL = domainURL;
        String umaAatClientJwksData = FileUtils.readFileToString(new File(umaAatClientJwks));
        client = ScimClient.umaInstance(domainURL, umaMetaDataUrl, umaAatClientId, umaAatClientJwksData, umaAatClientKeyId);
    }

    @Test(groups = "a")
    public void testCreateTwoAndUpdateWithOne() throws Exception {

        System.out.println("IN testCreateTwoAndUpdateWithOne...");

        ScimPerson person = createDummyPerson();

        ScimResponse response = client.createPerson(person, MediaType.APPLICATION_JSON);

        assertEquals(response.getStatusCode(), 201, "Could not add person, status != 201");

        ScimPerson personCreated = (ScimPerson) Util.jsonToObject(response, ScimPerson.class);
        this.id = personCreated.getId();

        List<ScimPersonEmails> emailsCreated = personCreated.getEmails();
        Assert.assertEquals(emailsCreated.size(), person.getEmails().size());

        for (ScimPersonEmails emailCreated : emailsCreated) {
            Assert.assertNotNull(emailCreated);
            System.out.println("emailCreated.getValue() = " + emailCreated.getValue());
        }

        personCreated.setDisplayName(personCreated.getDisplayName() + " UPDATED");
        personCreated.setPassword(null);

        List<ScimPersonEmails> emails = new ArrayList<ScimPersonEmails>();
        ScimPersonEmails email = new ScimPersonEmails();
        email.setPrimary("true");
        email.setValue("e@f.com");
        email.setType(Email.Type.WORK.getValue());
        emails.add(email);
        personCreated.setEmails(emails);

        ScimResponse responseUpdated = client.updatePerson(personCreated, this.id, MediaType.APPLICATION_JSON);

        Assert.assertEquals(200, responseUpdated.getStatusCode());

        ScimPerson personUpdated = (ScimPerson) Util.jsonToObject(responseUpdated, ScimPerson.class);

        assertEquals(personUpdated.getId(), this.id, "Person could not be retrieved");

        ScimPersonEmails emailUpdated = personUpdated.getEmails().get(0);
        Assert.assertNotNull(emailUpdated);
        Assert.assertEquals(emails.size(), personUpdated.getEmails().size());
        Assert.assertEquals(emails.get(0).getValue(), emailUpdated.getValue());
        System.out.println("emailUpdated.getValue() = " + emailUpdated.getValue());

        System.out.println("UPDATED response body = " + responseUpdated.getResponseBodyString());
        System.out.println("personUpdated.getId() = " + personUpdated.getId());
        System.out.println("personUpdated.getDisplayName() = " + personUpdated.getDisplayName());

        System.out.println("LEAVING testCreateTwoAndUpdateWithOne..." + "\n");
    }

    @Test(groups = "b", dependsOnGroups = "a", alwaysRun = true)
    public void testDeletePerson() throws Exception {

        System.out.println("IN testDeletePerson...");

        ScimResponse response = client.deletePerson(this.id);
        assertEquals(response.getStatusCode(), 200, "Person could not be deleted, status != 200");

        System.out.println("LEAVING testDeletePerson..." + "\n");
    }

    private ScimPerson createDummyPerson() {

        ScimPerson person = new ScimPerson();

        List<String> schema = new ArrayList<String>();
        schema.add("urn:scim:schemas:core:1.0");
        person.setSchemas(schema);

        ScimName name = new ScimName();
        name.setGivenName("Robert");
        name.setMiddleName("James");
        name.setFamilyName("Fischer");
        person.setName(name);

        person.setActive("active");

        person.setUserName("bobby_" + new Date().getTime());
        person.setPassword("test");
        person.setDisplayName("Bobby Fischer");
        person.setNickName("Bobby");
        person.setProfileUrl("");
        person.setLocale("en");
        person.setPreferredLanguage("US_en");
        person.setTitle("Super GM");

        List<ScimPersonEmails> scimPersonEmails  = new ArrayList<ScimPersonEmails>();
        for (int i = 1; i <= 2; i++) {
            ScimPersonEmails email = new ScimPersonEmails();
            email.setValue(i == 1 ? "a@b.com" : "c@d.com");
            email.setType(i == 1 ? "work" : "home");
            email.setPrimary(i == 1 ? "true" : "false");
            scimPersonEmails.add(email);
        }
        person.setEmails(scimPersonEmails);

        ScimPersonPhones phone = new ScimPersonPhones();
        phone.setType("work");
        phone.setValue("123-4567-890");
        person.getPhoneNumbers().add(phone);

        ScimPersonAddresses address = new ScimPersonAddresses();
        address.setCountry("USA");
        address.setStreetAddress("New York");
        address.setLocality("New York");
        address.setPostalCode("12345");
        address.setRegion("New York");
        address.setPrimary("true");
        address.setType("work");
        address.setFormatted(address.getStreetAddress() + " " + address.getLocality() + " " + address.getPostalCode() + " " + address.getRegion() + " " + address.getCountry());
        person.getAddresses().add(address);

        return person;
    }
}