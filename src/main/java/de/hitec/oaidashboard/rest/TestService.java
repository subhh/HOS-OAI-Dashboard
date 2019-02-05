package de.hitec.oaidashboard.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Path("/testservice")
public class TestService {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getTestService() throws IOException {
        Runtime rt = Runtime.getRuntime();
        String[] commands = {"metha-fortune"};
        Process proc = rt.exec(commands);

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc.getInputStream()));

        BufferedReader stdError = new BufferedReader(new
                InputStreamReader(proc.getErrorStream()));

        // read the output from the command
        System.out.println("Here is the standard output of the command:\n");
        String s = null;
        StringBuilder builder = new StringBuilder();
        while ((s = stdInput.readLine()) != null) {
            builder.append(s);
        }

        // read any errors from the attempted command
        System.out.println("Here is the standard error of the command (if any):\n");
        //while ((s = stdError.readLine()) != null) {
        //    builder.append(s);
        //}
        return builder.toString();
        //return "Hello from HITeC rest interface (get) test2";
    }
}
