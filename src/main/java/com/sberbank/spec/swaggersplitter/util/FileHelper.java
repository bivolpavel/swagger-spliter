package com.sberbank.spec.swaggersplitter.util;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

@Component
public class FileHelper {

    private final ObjectMapper objectMapper;

    public FileHelper(ObjectMapper objectMapper) {
        System.out.println("created");
        this.objectMapper = objectMapper;
    }

    public File readJsonFromFile(String filePath) throws Exception {
        try {
            return new File(filePath);
        } catch (Exception e) {
            throw new IOException("Could not read json file: ", e);
        }
    }

    public void writeJsonToFile(JsonNode jsonNode, String filePath) throws IOException {
        try {
            objectMapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
            objectMapper.writeValue(Paths.get(filePath).toFile(), jsonNode);
        } catch (Exception e) {
            throw new IOException("Could not write json to file: ", e);
        }
    }
}
