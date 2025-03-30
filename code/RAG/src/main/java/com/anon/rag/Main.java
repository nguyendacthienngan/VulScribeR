package com.anon.rag;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.anon.rag.model.CodeSnippet;
import com.anon.rag.model.Dataset;
import com.anon.rag.model.LuceneCodeSearchFacade;
import com.anon.rag.model.SearchResult;
import com.anon.rag.utils.DatasetUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class Main {

    // Hàm indexCluster: tạo chỉ mục từ file dataset của cluster tương ứng
    static LuceneCodeSearchFacade indexCluster(int clusterNumber) throws Exception {
        String address = "C:\\Users\\Administrator\\Downloads\\mapr\\VulScribeR-main\\code\\dataset\\container_data\\megavul_vuls_cls_" + clusterNumber + ".jsonl";
        Dataset fixedVuls = DatasetUtils.readFormattedVulsMegaVul(address);
        LuceneCodeSearchFacade lucene = new LuceneCodeSearchFacade();
        lucene.index(fixedVuls.getDataset().values());
        return lucene;
    }

    // Hàm thực hiện tìm kiếm qua các cluster và gộp kết quả lại
    static List<SearchResult> ClusteredSearchWithFunction(List<LuceneCodeSearchFacade> indexPerCluster, Dataset cleans) {
        ArrayList<CodeSnippet> cleansDataset = new ArrayList<>(cleans.getDataset().values());
        double[] scoresSum = {0d, 0d, 0d, 0d, 0d};
        double[] scoresMax = {0d, 0d, 0d, 0d, 0d};
        double[] scoresMin = {0d, 0d, 0d, 0d, 0d};

        List<SearchResult> crossClusterResults = new ArrayList<>();
        for (CodeSnippet searchItem : cleansDataset) {
            // System.out.println("Function sample: " + searchItem.getFunction());

            List<SearchResult> allClusterResults = new ArrayList<>();
            for (int clusterIndex = 0; clusterIndex < 5; clusterIndex++) {
                String clusterIndexValue = String.valueOf(clusterIndex);
                SearchResult result = indexPerCluster.get(clusterIndex)
                        .findSimilarFunction(searchItem.getId(), searchItem.getFunction(), 1);
                // System.out.println("Cluster " + clusterIndex + " result: " + result.getScoredCodeSnippets());

                result.getScoredCodeSnippets().forEach(z -> z.addMetaData("clusterIndex", clusterIndexValue));
                double score = Double.parseDouble(result.getScoredCodeSnippets().stream()
                        .findFirst().map(z -> z.getScore()).orElse("0"));
                scoresSum[clusterIndex] += score;
                scoresMax[clusterIndex] = scoresMax[clusterIndex] < score ? score : scoresMax[clusterIndex];
                scoresMin[clusterIndex] = scoresMin[clusterIndex] > score ? score : scoresMin[clusterIndex];
                allClusterResults.add(result);
            }
            // Gộp tất cả kết quả từ các cluster lại
            List<CodeSnippet> scoredCodeSnippets = allClusterResults.stream()
                    .flatMap(z -> z.getScoredCodeSnippets().stream()).toList();
            crossClusterResults.add(new SearchResult(searchItem.getId(), scoredCodeSnippets));
        }
        System.out.println("PerClusterScoreSum: " + Arrays.toString(scoresSum));
        System.out.println("PerClusterScoreMax: " + Arrays.toString(scoresMax));
        System.out.println("PerClusterScoreMin: " + Arrays.toString(scoresMin));
        return crossClusterResults;
    }

    // Hàm ghi kết quả tìm kiếm ra file JSON
    static void writeToJson(List<SearchResult> results, String fileName) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(new File(fileName), results);
    }

    // Hàm chính thực hiện quy trình:
    // - Đọc dataset Devign sạch
    // - Tạo các chỉ mục theo cluster
    // - Thực hiện tìm kiếm chéo các cluster
    // - Ghi kết quả ra file JSON
    static void generateNaiveDevignFunctionClustered() throws Exception {
        Dataset cleans = DatasetUtils.readCleanMegaVul("C:\\Users\\Administrator\\Downloads\\mapr\\VulScribeR-main\\dataset\\simple\\megavul_clean.json");
        List<LuceneCodeSearchFacade> indexPerCluster = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            indexPerCluster.add(indexCluster(i));
        }
        writeToJson(ClusteredSearchWithFunction(indexPerCluster, cleans), "results_full_code2code_clustered.json");
    }

    // Hàm main: chỉ gọi hàm generateNaiveDevignFunctionClustered()
    public static void main(String[] args) throws Exception {
        System.out.println("Starting Clustered Search Based on Function Similarity");
        generateNaiveDevignFunctionClustered();
    }
}
