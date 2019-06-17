package de.uni_hamburg.sub.oaidashboard.importexport.datastructures;

import java.util.Set;
import java.util.stream.Collectors;

public class Licences {
	public Set<JsonLicence> licenceSet;

    public Licences()
	    {
	    	licenceSet = null;
	    }

    @Override
	public String toString() {
		return licenceSet.stream().map(l -> l.licence_name + " -> " + l.licence_type.toString()).collect(Collectors.toList()).toString();
	}

}
