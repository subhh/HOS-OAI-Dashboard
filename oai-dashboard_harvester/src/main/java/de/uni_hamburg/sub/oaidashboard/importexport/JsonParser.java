package de.uni_hamburg.sub.oaidashboard.importexport;

import java.io.*;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import com.dslplatform.json.*;
import com.dslplatform.json.runtime.Settings;
import de.uni_hamburg.sub.oaidashboard.importexport.datastructures.Licences;
import de.uni_hamburg.sub.oaidashboard.importexport.datastructures.JsonLicence;
import de.uni_hamburg.sub.oaidashboard.database.datastructures.Licence;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class JsonParser {
	
	private static DslJson.Settings<Object> settings;
	private static DslJson<Object> dslJson;
	private Licences licences;
	private String filename;
	private JsonWriter writer;
	
    private static Logger logger = LogManager.getLogger(Class.class.getName());

	public JsonParser(String filename)
	{
	    // initialise json decoder
		settings = Settings.withRuntime() // Runtime configuration needs to be
				.includeServiceLoader();  // explicitly enabled for java8 types
		
		dslJson = new DslJson<Object>(settings);
		writer = dslJson.newWriter();
		this.filename = filename;
	}

	public Set<JsonLicence> getLicences() {
		byte[] inputStreamBytes;

		licences = null;
		File file = new File(filename);
		if(file.isFile()) {
			try {
				inputStreamBytes = IOUtils.toByteArray(new FileInputStream(filename));

				licences = dslJson.deserialize(Licences.class, inputStreamBytes, inputStreamBytes.length);
			} catch (IOException e) {
				e.printStackTrace();
			}
			logger.info("Licences received: {}", licences.toString());
		} else {
			licences = new Licences();
			licences.licenceSet = new HashSet<>();
			logger.info("No Licences received (no file found or empty): {}", filename);
		}
		return licences.licenceSet;
	}
	
	public void setLicences(Hashtable<String, Set<Licence>> dbLicences) {
		convertDbLicencesToJson(dbLicences);
		try {
			if (filename != "") {
				dslJson.serialize(writer, licences);
				PrettifyOutputStream out = new PrettifyOutputStream(new FileOutputStream(filename));
				out.write(writer.toByteArray());
				out.flush();
				out.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void convertDbLicencesToJson(Hashtable<String, Set<Licence>> dbLicences) {
		licences = new Licences();
		licences.licenceSet = new HashSet<JsonLicence>();
		dbLicences.forEach((k, v) -> {
			for (Licence l : v) {
				licences.licenceSet.add(new JsonLicence(l));
			}
		});
	}

}

