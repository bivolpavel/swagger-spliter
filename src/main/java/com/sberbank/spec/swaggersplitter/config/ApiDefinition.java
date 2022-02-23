package com.sberbank.spec.swaggersplitter.config;

import java.util.List;

public class ApiDefinition {

    private String path;
    private List<String> methods;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<String> getMethods() {
        return methods;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods;
    }
}
