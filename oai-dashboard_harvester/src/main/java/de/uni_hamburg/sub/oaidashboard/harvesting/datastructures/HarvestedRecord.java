package de.uni_hamburg.sub.oaidashboard.harvesting.datastructures;

import java.util.ArrayList;

public class HarvestedRecord {
	public ArrayList<String> specList;
    public ArrayList<String> rightsList;
    public ArrayList<String> typeList;
    public String dc_format;
    public String dateStamp;
    public String identifier;

    public HarvestedRecord() {
    	specList   = new ArrayList<String>();
    	rightsList = new ArrayList<String>();
    	typeList = new ArrayList<String>();
    	dc_format = "NO_FORMAT"; // default value
    	dateStamp  = "";
    }
}
