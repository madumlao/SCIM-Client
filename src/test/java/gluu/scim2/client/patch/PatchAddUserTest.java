package gluu.scim2.client.patch;

import gluu.scim2.client.UserBaseTest;
import org.gluu.oxtrust.model.scim2.user.UserResource;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static javax.ws.rs.core.Response.Status.*;

import static org.testng.Assert.*;

/**
 * Created by jgomer on 2017-11-02.
 */
public class PatchAddUserTest extends UserBaseTest{

    private UserResource user;

    @Parameters({"user_average_create"})
    @Test
    public void create(String json){
        logger.debug("Creating user from json...");
        user=createUserFromJson(json);
    }

    @Parameters({"user_patchadd"})
    @Test(dependsOnMethods = "create",  groups = "A")
    public void jsonPatch(String patchRequest){
        Response response = client.patchUser(patchRequest, user.getId(), null, null, null);
        assertEquals(response.getStatus(), OK.getStatusCode());

        UserResource other=response.readEntity(usrClass);

        assertNotNull(other.getNickName());
        assertNotNull(other.getUserType());
        assertTrue(user.getEmails().size() < other.getEmails().size());
        assertTrue(user.getPhoneNumbers().size() < other.getPhoneNumbers().size());

        assertTrue(other.getIms().size()>0);
        assertTrue(other.getRoles().size()>0);
    }

    @Test(dependsOnMethods = "jsonPatch")
    public void delete(){
        deleteUser(user);
    }

}
