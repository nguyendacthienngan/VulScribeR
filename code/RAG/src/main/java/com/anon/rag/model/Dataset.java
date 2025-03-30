package com.anon.rag.model;

import java.util.HashMap;

public class Dataset {
    
    private HashMap<String, CodeSnippet> fileHeaders = new HashMap<>();

    public Dataset() {
    }

    public static Dataset merge(Dataset dataset1, Dataset dataset2) {
        for (var entry : dataset2.getDataset().entrySet()) {
            dataset1.addPair(entry.getKey(), entry.getValue());
        }
        return dataset1;
    }

    public void addPair(String id, String header, String code) {
        // Check if the header contains '{'. If not, use the whole header.
        int index = header.indexOf('{');
        String safeHeader = (index != -1) ? header.substring(0, index) : header;
        CodeSnippet snippet = new CodeSnippet(id, safeHeader, code);
        addPair(id, snippet);
    }

    
    // public void addPair(String id, String header, String full) {
    //     int bodyIndex = full.indexOf("{");
    //     addPair(id, new CodeSnippet(id ,header, full.substring(bodyIndex).trim()));
    // }

    public void addPair(String id, CodeSnippet snippet) {
        // snippet.addMetaData(INDEX_STRING, id);
        fileHeaders.put(id, snippet);
    }

    public HashMap<String, CodeSnippet> getDataset() {
        return fileHeaders;
    }

    public int getSize() {
        return fileHeaders.size();
    }
}
