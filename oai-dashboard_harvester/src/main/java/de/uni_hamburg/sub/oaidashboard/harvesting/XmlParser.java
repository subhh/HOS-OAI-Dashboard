package de.uni_hamburg.sub.oaidashboard.harvesting;

import de.uni_hamburg.sub.oaidashboard.harvesting.datastructures.HarvestedRecord;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.input.TeeInputStream;

public class XmlParser {
	
    static final String LISTRECORDS = "ListRecords";
    static final String RECORD = "record";
    static final String HEADER = "header";
    static final String DATESTAMP = "datestamp";
    static final String SETSPEC = "setSpec";
    static final String METADATA = "metadata";
    static final String RIGHTS = "rights";
    static final String IDENTIFIER = "identifier";

	public ArrayList<HarvestedRecord> getRecords(Path input_filepath, Path output_filepath) throws Exception
	{
		ArrayList<HarvestedRecord> records = new ArrayList<HarvestedRecord>();
		InputStream is = null;
		TeeInputStream tis = null;
		if (output_filepath != null) {
			is = new GZIPInputStream(Files.newInputStream(input_filepath));
			// get rid of preceding date:
			int index1 = (input_filepath.getFileName().toString().lastIndexOf("-") + 1); 
			// get rid of '.gz':
			int index2 = input_filepath.getFileName().toString().lastIndexOf("."); 
			String outputFilename = input_filepath.getFileName().toString().substring(index1, index2);
			
			File file = new File(output_filepath.toString() + File.separator + outputFilename);
			OutputStream outputStream = new FileOutputStream(file, false);
			tis = new TeeInputStream(is, outputStream, true);
		}
		else {
			is = Files.newInputStream(input_filepath);		
		}
		
		try {
            // First, create a new XMLInputFactory
            XMLInputFactory inputFactory = XMLInputFactory.newInstance();
            // Setup a new eventReader
            XMLEventReader eventReader = inputFactory.createXMLEventReader((output_filepath != null) ? tis : is);
            // read the XML document
            HarvestedRecord record = null;
            StartElement startElement = null;
            EndElement endElement = null;
            
            while (eventReader.hasNext()) {
                XMLEvent event = eventReader.nextEvent();

                if (event.isStartElement()) {
                    startElement = event.asStartElement();
                    // A record startElement can occur inside GetRecord and ListRecords,
                    // we are only interested in the later ones.
                    if (startElement.getName().getLocalPart().equals(LISTRECORDS)) {
                        while (eventReader.hasNext()) {
                            event = eventReader.nextEvent();

                            if (event.isEndElement()) {
                            	endElement = event.asEndElement();
                                if (endElement.getName().getLocalPart().equals(LISTRECORDS)) {
                                	break;
                                }
                            }
                            if (event.isStartElement()) {
                                startElement = event.asStartElement();

                                if (startElement.getName().getLocalPart().equals(RECORD)) {

                                	record = new HarvestedRecord();
                                    while (eventReader.hasNext()) {
                                        event = eventReader.nextEvent();

                                        if (event.isEndElement()) {
                                        	endElement = event.asEndElement();
                                            if (endElement.getName().getLocalPart().equals(RECORD)) {
                                            	records.add(record);
                                            	break;
                                            }
                                        }
                                        if (event.isStartElement()) {
                                            startElement = event.asStartElement();

                                            if (startElement.getName().getLocalPart().equals(HEADER)) {
                                                
                                                while (eventReader.hasNext()) {
                                                    event = eventReader.nextEvent();

                                                    if (event.isEndElement()) {
                                                    	endElement = event.asEndElement();
                                                        if (endElement.getName().getLocalPart().equals(HEADER)) {
                                                        	break;
                                                        }
                                                    }
                                                    if (event.isStartElement()) {
                                                        startElement = event.asStartElement();
                                                        if (startElement.getName().getLocalPart().equals(SETSPEC)) {

                                                        	event = eventReader.nextEvent();
                                                            record.specList.add((String) event.asCharacters().getData());
                                                            continue;
                                                        }
                                                    }
                                                    if (event.isStartElement()) {
                                                        startElement = event.asStartElement();
                                                        if (startElement.getName().getLocalPart().equals(DATESTAMP)) {

                                                        	event = eventReader.nextEvent();
                                                            record.dateStamp = (String) event.asCharacters().getData();
                                                            continue;
                                                        }
                                                    }
                                                    if (event.isStartElement()) {
                                                        startElement = event.asStartElement();
                                                        if (startElement.getName().getLocalPart().equals(IDENTIFIER)) {
                                                            event = eventReader.nextEvent();
                                                            record.identifier = (String) event.asCharacters().getData();
                                                            continue;
                                                        }
                                                    }
                                                }
                                            }
                                            if (startElement.getName().getLocalPart().equals(METADATA)) {
                                                
                                                while (eventReader.hasNext()) {
                                                    event = eventReader.nextEvent();

                                                    if (event.isEndElement()) {
                                                    	endElement = event.asEndElement();
                                                        if (endElement.getName().getLocalPart().equals(METADATA)) {
                                                        	break;
                                                        }
                                                    }
                                                    if (event.isStartElement()) {
                                                        startElement = event.asStartElement();
                                                        if (startElement.getName().getLocalPart().equals(RIGHTS)) {

                                                        	event = eventReader.nextEvent();
                                                            record.rightsList.add((String) event.asCharacters().getData());
                                                            continue;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                            		if (record.rightsList.isEmpty())
                            		{
                            			record.rightsList.add("NO_RIGHTS");
                            		}
                                }
                            }
                        }
                    }
                }
            }
		}
		catch (Throwable ex) { 
			System.err.println("Failed to parse XMLFile." + ex);
			throw new Exception(ex); 
		}
		return records;
	}
}
