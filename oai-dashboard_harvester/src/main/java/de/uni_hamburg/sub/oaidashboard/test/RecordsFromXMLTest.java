package de.uni_hamburg.sub.oaidashboard.test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.RunWith;

import de.uni_hamburg.sub.oaidashboard.harvesting.JsonParser;
import de.uni_hamburg.sub.oaidashboard.harvesting.XmlParser;
import de.uni_hamburg.sub.oaidashboard.harvesting.datastructures.HarvestedRecord;
import de.uni_hamburg.sub.oaidashboard.harvesting.datastructures.MethaIdStructure;


@RunWith(JUnitPlatform.class)
public class RecordsFromXMLTest {
	static String pathToEnvironmentFiles = null;
//	String getProgramPath(TestInfo testinfo) {
	void init() {
		if (pathToEnvironmentFiles == null) {
			// this is only slightly better than a completely static string, but maybe the best possible way.
			pathToEnvironmentFiles = Paths.get(this.getClass().getClassLoader().getResource(".").getPath()).getParent().getParent().toFile() 
					  + File.separator + "src" + File.separator + "main" + File.separator + "java" + File.separator
					  + this.getClass().getName().substring(0, this.getClass().getName().lastIndexOf(".") + 1).replace(".", "/")
					  + "testEnvironment" + File.separator;
		}
	}
	
	@Test	
	void testXML() {
		init();
		List<HarvestedRecord> records = new ArrayList<>();
        XmlParser xmlparser = new XmlParser();
        try {
        	records.addAll(xmlparser.getRecords(Paths.get(pathToEnvironmentFiles + "testRecords.xml"), null));
        	assertEquals(records.size(), 6);
        	assertEquals(records.get(0).dc_format, "application/pdf"); 
        	assertEquals(records.get(1).dc_format, "NO_FORMAT"); 
        	assertEquals(records.get(0).rightsList.size(), 1); 
        	assertEquals(records.get(0).rightsList.get(0), "info:eu-repo/semantics/openAccess"); 
        	assertEquals(records.get(0).typeList.size(), 2);
        	assertEquals(records.get(0).typeList.get(0), "inProceedings");
        	assertEquals(records.get(0).specList.size(), 6);
        	assertEquals(records.get(0).specList.get(0), "com_11420_1"); 
        	assertEquals(records.get(0).identifier, "oai:tore.tuhh.de:11420/3");
        } catch (Exception e) {
        	System.out.println("Failed to run Record-Harvester. " + e);
        }
	}
	
	@Test
	void testJSON() {
		init();
		MethaIdStructure instance = null;
        try {
	        JsonParser jParser = new JsonParser(new FileInputStream(pathToEnvironmentFiles + "MethaID-Answer.json"), "");
	        instance = jParser.getJsonStructure();
	        assertNotNull(instance);
	        assertEquals(instance.formats.size(), 9);
	        assertEquals(instance.formats.get(3).metadataPrefix, "oai_dc");
	        assertEquals(instance.formats.get(3).schema, "http://www.openarchives.org/OAI/2.0/oai_dc.xsd");
	        assertEquals(instance.formats.get(3).metadataNamespace, "http://www.openarchives.org/OAI/2.0/oai_dc/");
	        assertEquals(instance.identify.repositoryName, "TORE TUHH Open Research");
	        assertEquals(instance.identify.baseURL, "http://tubdok.tub.tuhh.de/oai/request");
	        assertEquals(instance.sets.size(), 137);
	        assertEquals(instance.sets.get(0).setName, "Biography, genealogy, insignia");
	        assertEquals(instance.sets.get(0).setSpec, "ddc:920");
        } catch (Exception e) {
        	System.out.println("Failed to run Record-Harvester. " + e);
        }
		
	}
	
}
