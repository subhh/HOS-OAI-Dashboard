package de.uni_hamburg.sub.oaidashboard.harvesting.datastructures;

import java.util.ArrayList;

public class MethaIdStructure {
	public ArrayList<Format> formats;
    public Identify identify;
	public ArrayList<MethaSet> sets;

    public MethaIdStructure()
    {
    	formats  = null;
    	identify = null;
    	sets     = null;
    }
}
