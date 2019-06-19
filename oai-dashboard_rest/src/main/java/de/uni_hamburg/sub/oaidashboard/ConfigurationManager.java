package de.uni_hamburg.sub.oaidashboard;


import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;


public class ConfigurationManager {

    private static Logger logger = LogManager.getLogger(ConfigurationManager.class.getName());

    private Map<String, String> reverseDCTypeMappings = new HashMap<>();

    public ConfigurationManager() {
        String conf_dir = getConfigurationDirectory();
        if(conf_dir != null) {
            logger.info("loading config/mapping files from directory: {}", conf_dir);
            loadMappingsDCType(conf_dir);
        } else {
            logger.info("loading config/mapping files from resources folder (src/main/resources), provided at compile time");
            loadMappingsDCType();
        }
    }

    private void loadMappingsDCType(String conf_dir) {
        String mappingFilePath = conf_dir + "/" + "mappings_dc_type.json";
        logger.info("loading mappings for dc type from file: {}", mappingFilePath);
        File mappingFile = new File(mappingFilePath);
        loadMappingsDCType(mappingFile);
    }

    private void loadMappingsDCType() {
        String mappingFileName = "mappings_dc_type.json";
        logger.info("loading mappings for dc type from file: {}", mappingFileName);
        File mappingFile = new File(getClass().getClassLoader().getResource(mappingFileName).getFile());
        loadMappingsDCType(mappingFile);
    }

    private void loadMappingsDCType(File mappingFile) {
        Map<String, List<String>> mappings = new HashMap<>();
        Map<String, String> reverseMappings = new HashMap<>();

        try {
            String jsonContent = FileUtils.readFileToString(mappingFile, "utf-8");
            JSONObject jsonMappings = new JSONObject(jsonContent).getJSONObject("mappings_dc_type");
            for(String jsonKey : jsonMappings.keySet()) {
                JSONArray jsonValues = jsonMappings.getJSONArray(jsonKey);

                List<String> values = new ArrayList<>();
                for(int i=0; i < jsonValues.length(); i++) {
                    values.add(jsonValues.get(i).toString());
                    reverseMappings.put(jsonValues.get(i).toString(), jsonKey);
                }
                mappings.put(jsonKey, values);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e);
        }
        logger.info("loaded mappings: {}", mappings);
        logger.info("loaded reverseMappings: {}", reverseMappings);
        this.reverseDCTypeMappings = reverseMappings;
    }

    public Map<String, String> getReverseDCTypeMappings() {
        return this.reverseDCTypeMappings;
    }

    private String getConfigurationDirectory() {
        String conf_dir = null;

        // read config from "resources"-folder
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Properties props = new Properties();
        try(InputStream resourceStream = loader.getResourceAsStream("rest.properties")) {
            props.load(resourceStream);
        } catch (IOException e) {
            logger.error(e);
        }
        conf_dir = props.getProperty("rest.config.directory", null);
        if(conf_dir != null) {
            logger.info("Using configuration directory: {}", conf_dir);
        } else {
            logger.info("No configuration directory provided in rest.properties");
        }
        return conf_dir;
    }
}
