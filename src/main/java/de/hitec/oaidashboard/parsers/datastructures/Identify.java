package de.hitec.oaidashboard.parsers.datastructures;

import java.util.ArrayList;

public class Identify {
	public String repositoryName;
    public String baseURL;
	public ArrayList<String> adminEmail;
	public String earliestDatestamp;

	public Identify()
    {
    	repositoryName    = null;
    	baseURL           = null;
    	adminEmail        = null;
    	earliestDatestamp = null;
    }
}
