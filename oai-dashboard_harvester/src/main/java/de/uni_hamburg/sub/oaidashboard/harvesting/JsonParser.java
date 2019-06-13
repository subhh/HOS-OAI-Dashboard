package de.uni_hamburg.sub.oaidashboard.harvesting;

import java.io.*;
import com.dslplatform.json.*;
import com.dslplatform.json.runtime.Settings;
import de.uni_hamburg.sub.oaidashboard.harvesting.datastructures.MethaIdStructure;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class JsonParser {
	
	private static DslJson.Settings<Object> settings;
	private static DslJson<Object> dslJson;
	private MethaIdStructure instance;

    private static Logger logger = LogManager.getLogger(Class.class.getName());

	public JsonParser(InputStream is, String filename) throws Exception
	{
	    // initialise json decoder
		settings = Settings.withRuntime() // Runtime configuration needs to be
				.includeServiceLoader();  // explicitly enabled for java8 types
		
		dslJson = new DslJson<Object>(settings);
		byte[] inputStreamBytes = IOUtils.toByteArray(is);
		byte[] toFile = (filename != "") ? inputStreamBytes.clone() : null;
		if (filename != "") {
			new FileOutputStream(filename).write(toFile);
		}
		instance = dslJson.deserialize(MethaIdStructure.class, inputStreamBytes, inputStreamBytes.length);

		logger.info("Metadata received: {}", instance.identify.repositoryName);
	}

	public MethaIdStructure getJsonStructure() {
		return instance;
	}
}
