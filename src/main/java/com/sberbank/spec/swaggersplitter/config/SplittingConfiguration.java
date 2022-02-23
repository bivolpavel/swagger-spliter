package com.sberbank.spec.swaggersplitter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@ConfigurationProperties(prefix = "swagger.splitt")
@Configuration
public class SplittingConfiguration {

    private String inputFilePath;
    private String outputFilePath;

    private List<ApiDefinition> keptApis;

    public String getInputFilePath() {
        return inputFilePath;
    }

    public void setInputFilePath(String inputFilePath) {
        this.inputFilePath = inputFilePath;
    }

    public String getOutputFilePath() {
        return outputFilePath;
    }

    public void setOutputFilePath(String outputFilePath) {
        this.outputFilePath = outputFilePath;
    }

    public List<ApiDefinition> getKeptApis() {
        return keptApis;
    }

    public void setKeptApis(List<ApiDefinition> keptApis) {
        this.keptApis = keptApis;
    }
}
