package hitec.oaidashboard.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/testservice")
public class TestService {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getTestService() {
        return "Hello from HITeC rest interface (get) test2";
    }
}
