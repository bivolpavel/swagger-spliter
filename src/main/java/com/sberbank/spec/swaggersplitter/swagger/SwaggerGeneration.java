package com.sberbank.spec.swaggersplitter.swagger;

import com.sberbank.spec.swaggersplitter.config.ApiDefinition;
import com.sberbank.spec.swaggersplitter.config.SplittingConfiguration;
import com.sberbank.spec.swaggersplitter.util.Constants;
import com.sberbank.spec.swaggersplitter.util.FileHelper;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.*;

@Component
public class SwaggerGeneration {

    private static final Logger logger = LoggerFactory.getLogger(SwaggerGeneration.class);

    private SplittingConfiguration splittingConfiguration;
    private ObjectMapper mapper;
    private FileHelper fileHelper;

    public SwaggerGeneration(SplittingConfiguration splittingConfiguration, ObjectMapper mapper, FileHelper fileHelper) {
        this.splittingConfiguration = splittingConfiguration;
        this.mapper = mapper;
        this.fileHelper = fileHelper;
    }

    @PostConstruct
    public void splitSwagger(){
        try {
            //Read Json file
            File jsonInputFile = fileHelper.readJsonFromFile(splittingConfiguration.getInputFilePath());
            JsonNode rootNode = mapper.readTree(jsonInputFile);
            JsonNode componentsNode = rootNode.path(Constants.COMPONENTS_NODE_NAME);
            JsonNode oldPathsNode = rootNode.path(Constants.PATHS_NODE_NAME);
            JsonNode newPathsNode = createNewPathsNode(oldPathsNode, splittingConfiguration.getKeptApis());

            //Search all used used definitions for configured methods
            HashMap<String, Set<String>> schemesNames = new HashMap<>();
            pickReferencesFromPathsNode(newPathsNode, schemesNames);
            pickUsedReferencesFromComponentsNode(componentsNode, schemesNames);

            //Create new components Node with only used definitions
            ObjectNode newComponentsNode = createNewComponents(componentsNode, schemesNames);

            //Remove olds nodes and adding new ones with cleaned nodes
            ((ObjectNode)rootNode).remove(Constants.PATHS_NODE_NAME);
            ((ObjectNode)rootNode).put(Constants.PATHS_NODE_NAME, newPathsNode);
            ((ObjectNode)rootNode).remove(Constants.COMPONENTS_NODE_NAME);
            ((ObjectNode)rootNode).put(Constants.COMPONENTS_NODE_NAME, newComponentsNode);

            //Write to Json File
            fileHelper.writeJsonToFile(rootNode, splittingConfiguration.getOutputFilePath());
        } catch (Exception e) {
            logger.error("An exception occurred during json processing: ", e);
        }
    }

    private void addSchemaNameToMap(JsonNode jsonNode, HashMap<String, Set<String>> components){
        String[] componetsNodes = jsonNode.asText().split("/");
        Set<String> subComponents = components.computeIfAbsent(componetsNodes[2], k -> new HashSet<>());
        subComponents.add(componetsNodes[3]);
    }

    private void displayListOfElements(HashMap<String, Set<String>> components){
        for (Map.Entry<String, Set<String>> entry : components.entrySet()) {

            for (String s : entry.getValue()) {
                logger.debug("Key : " + entry.getKey() + ", and value: "+ s);
            }
        }
    }

    private ObjectNode createNewPathsNode(JsonNode oldPathsNode, List<ApiDefinition> apiDefinitions){
        ObjectNode objectNode = mapper.createObjectNode();

        if (oldPathsNode.isObject() && !oldPathsNode.isNull() && apiDefinitions != null && !apiDefinitions.isEmpty()) {
            Iterator<Map.Entry<String, JsonNode>> paths = oldPathsNode.getFields();

            for (ApiDefinition apiDefinition : apiDefinitions) {

                while (paths.hasNext()) {
                    Map.Entry<String, JsonNode> path = paths.next();

                    if (path.getKey().startsWith(apiDefinition.getPath())) {
                        objectNode.put(path.getKey(), path.getValue());
                    }
                }
            }
        }

        return objectNode;
    }

    private void pickReferencesFromPathsNode(JsonNode jsonNode, HashMap<String, Set<String>> schemesNames){
        if (jsonNode.isArray() && jsonNode.size() != 0) {
            Iterator<JsonNode> nodes = jsonNode.getElements();

            while (nodes.hasNext()) {
                JsonNode entry = nodes.next();
                pickReferencesFromPathsNode(entry, schemesNames);
            }
        } else if (jsonNode.isObject() && !jsonNode.isNull()) {
            Iterator<Map.Entry<String, JsonNode>> nodes = jsonNode.getFields();

            while (nodes.hasNext()) {
                Map.Entry<String, JsonNode> entry = nodes.next();
                pickReferencesFromPathsNode(entry.getValue(), schemesNames);
            }
        } else {

            if (jsonNode.isTextual() && jsonNode.asText().startsWith("#/components/")) {
                addSchemaNameToMap(jsonNode, schemesNames);
            }
        }
    }

    private void pickUsedReferencesFromComponentsNode(JsonNode componentsNode, HashMap<String, Set<String>> schemesNames){
        HashMap<String, Set<String>> innerDefinitionDependencies = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry : schemesNames.entrySet()) {
            JsonNode subComponentNode = componentsNode.get(entry.getKey());

            for (String denifitionName : entry.getValue()) {
                JsonNode node = subComponentNode.get(denifitionName);
                parseComponentsNode(componentsNode, node, innerDefinitionDependencies);
            }
        }

        for (Map.Entry<String, Set<String>> entry : innerDefinitionDependencies.entrySet()) {
            schemesNames.get(entry.getKey()).addAll(entry.getValue());
        }
    }

    private void parseComponentsNode(JsonNode componentsNode, JsonNode currentNode,
                                HashMap<String, Set<String>> components){
        if (currentNode.isArray() && (currentNode).size() != 0) {
            Iterator<JsonNode> nodes = currentNode.getElements();

            while (nodes.hasNext()) {
                JsonNode entry = nodes.next();
                parseComponentsNode(componentsNode, entry, components);
            }
        } else if (currentNode.isObject() && !currentNode.isNull()) {
            Iterator<Map.Entry<String, JsonNode>> nodes = currentNode.getFields();

            while (nodes.hasNext()) {
                Map.Entry<String, JsonNode> entry = nodes.next();
                parseComponentsNode(componentsNode, entry.getValue(), components);
            }
        } else {

            if (currentNode.isTextual() && currentNode.asText().startsWith("#/components/")) {
                addSchemaNameToMap(currentNode, components);
                String[] componetsNodes = currentNode.asText().split("/");
                JsonNode subComponentsNode = componentsNode.get(componetsNodes[2]);

                parseComponentsNode(componentsNode, subComponentsNode.get(componetsNodes[3]) , components);
            }
        }
    }

    private ObjectNode createNewComponents(JsonNode componentsNode, HashMap<String, Set<String>> schemesNames) {

        ObjectNode newComponentsNode = mapper.createObjectNode();

        for (Map.Entry<String, Set<String>> entry : schemesNames.entrySet()) {
            JsonNode subComponentNode = componentsNode.get(entry.getKey());
            ObjectNode objectNode = mapper.createObjectNode();

            if (!subComponentNode.isObject() || subComponentNode.isNull()) {
                continue;
            }

            Set<String> set = entry.getValue();
            ArrayList<String> strings = new ArrayList<>(set.size());
            strings.addAll(set);

            Collections.sort(strings);

            for (String definitionName : strings) {
                objectNode.put(definitionName, subComponentNode.get(definitionName));
            }

            newComponentsNode.put(entry.getKey(), objectNode);
        }

        return newComponentsNode;
    }
}
