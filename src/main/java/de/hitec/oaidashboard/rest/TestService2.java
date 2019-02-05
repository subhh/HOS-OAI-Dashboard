package de.hitec.oaidashboard.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

@Path("/testservice2")
public class TestService2 {
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getTestService() throws IOException {
        Runtime rt = Runtime.getRuntime();
        String[] commands = {"metha-files", "http://ediss.sub.uni-hamburg.de/oai2/oai2.php"};
        Process proc = rt.exec(commands);

        BufferedReader stdInput = new BufferedReader(new
                InputStreamReader(proc.getInputStream()));

        // read the output from the command
        System.out.println("Here is the standard output of the command:\n");
        String s = null;
        String filepath;
        InputStream is = null;
        while ((s = stdInput.readLine()) != null) {
            filepath = s;
            is = new GZIPInputStream(new FileInputStream(filepath));
            System.out.println(is.toString());
        }


        // read any errors from the attempted command
        System.out.println("Here is the standard error of the command (if any):\n");
        //while ((s = stdError.readLine()) != null) {
        //    builder.append(s);
        //}
        String xml_string = "";
        if(is != null) {
            xml_string = new BufferedReader(new InputStreamReader(is))
                    .lines().collect(Collectors.joining("\n"));
        }

        return xml_string;
        //return "Hello from HITeC rest interface (get) test2";
    }
}

