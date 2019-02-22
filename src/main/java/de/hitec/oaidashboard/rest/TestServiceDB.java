package de.hitec.oaidashboard.rest;

//import de.hitec.oaidashboard.database.ManageEmployee;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@Path("/testservice_db")
public class TestServiceDB {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getTestService() throws IOException {

        //ManageEmployee empManager = new ManageEmployee();

        return "db test (save into and retrieve)";
    }
}