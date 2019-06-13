package de.uni_hamburg.sub.oaidashboard.importexport.datastructures;

import java.sql.Timestamp;
import de.uni_hamburg.sub.oaidashboard.database.datastructures.LicenceType;
import de.uni_hamburg.sub.oaidashboard.database.datastructures.Licence;

public class JsonLicence {
	public String licence_name;
	public LicenceType licence_type;
	public Timestamp validFrom;
	public Timestamp validUntil;

	public JsonLicence()
	{
		licence_name = null;
		licence_type = null;
		validFrom    = null;
		validUntil   = null;
	}

	public JsonLicence(Licence lic)
	{
		licence_name = lic.getName();
		licence_type = lic.getType();
		validFrom    = lic.getValidFrom();
		validUntil   = lic.getValidUntil();
	}
}
