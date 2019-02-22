package de.hitec.oaidashboard.parsers;

import java.io.*;
import com.dslplatform.json.*;
import com.dslplatform.json.runtime.Settings;
import org.apache.commons.io.IOUtils;
import de.hitec.oaidashboard.parsers.datastructures.MethaIdStructure;


public class JsonParser {
	
	private static DslJson.Settings<Object> settings;
	private static DslJson<Object> dslJson;
	private MethaIdStructure instance;

	public JsonParser() {};
	
	public JsonParser(InputStream is, String filename) throws Exception
	{
	    // initialise json decoder
		settings = Settings.withRuntime() // Runtime configuration needs to be
				.includeServiceLoader();  // explicitly enabled for java8 types
		
		dslJson = new DslJson<Object>(settings);
		byte[] inputStreamBytes = IOUtils.toByteArray(is);
		byte[] toFile = inputStreamBytes.clone();
		instance = dslJson.deserialize(MethaIdStructure.class, inputStreamBytes, inputStreamBytes.length);
		try (FileOutputStream stream = new FileOutputStream(filename)) {
		    stream.write(toFile);
		}
		System.out.println("Metadata received: " + instance.identify.repositoryName);
	}

	public MethaIdStructure getJsonStructure() {
		return instance;
	}
}
