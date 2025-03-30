package com.anon.rag.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.anon.rag.model.Dataset;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DatasetUtils {

    private DatasetUtils() {
    }

    /**
     * Đọc file JSON chứa các bản ghi "clean" (is_vul == false) của MegaVul.
     * Các trường được sử dụng:
     * - file_path: dùng làm key định danh duy nhất.
     * - func: chứa function code sau khi đã fix (clean).
     * @param path Đường dẫn đến file JSON.
     * @return Dataset chứa các cặp (file_path, header, func) được trích xuất.
     * @throws Exception Nếu có lỗi khi đọc file.
     */
    public static Dataset readCleanMegaVul(String path) throws Exception {
        Dataset dataset = new Dataset();
        ObjectMapper objectMapper = new ObjectMapper();

        // Read entire file content into a String
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        String fileContent = sb.toString();

        // Parse the entire content as a JSON array
        JsonNode root = objectMapper.readTree(fileContent);
        if (root.isArray()) {
            for (JsonNode jsonNode : root) {
                JsonNode isVul = jsonNode.get("is_vul");

                // Debug: in ra giá trị của is_vul
                // System.out.println("Record is_vul: " + isVul);

                boolean isVulFlag = false;
                if (isVul != null) {
                    if (isVul.isBoolean()) {
                        isVulFlag = isVul.asBoolean();
                    } else {
                        isVulFlag = "true".equalsIgnoreCase(isVul.asText());
                    }
                }

                // Clean record: is_vul == false
                if (!isVulFlag) {
                    JsonNode filePath = jsonNode.get("file_path");
                    JsonNode func = jsonNode.get("func");
                    if (filePath != null && func != null) {
                        String funcText = func.asText();
                        String header = extractMethodHeader(funcText);
                        String codeWithoutHeader = "";
                        if (funcText.length() > header.length()) {
                            codeWithoutHeader = funcText.substring(header.length()).trim();
                        } else {
                            // Nếu toàn bộ chuỗi bằng header hoặc ngắn hơn, ta có thể gán chuỗi rỗng hoặc giữ nguyên
                            codeWithoutHeader = funcText;
                        }
                        dataset.addPair(filePath.asText(), header, codeWithoutHeader);
                    }
                }
            }
        }
        return dataset;
    }

    /**
     * Đọc file JSON chứa các bản ghi vulnerable (is_vul == true) của MegaVul.
     * Các trường được sử dụng:
     * - file_path: dùng làm key định danh duy nhất.
     * - func_before: chứa function code vulnerable (trước khi fix).
     * - diff_line_info: chứa thông tin diff (để xác nhận record có đủ dữ liệu diff).
     * @param path Đường dẫn đến file JSON.
     * @return Dataset chứa các cặp (file_path, header, func_before) được trích xuất.
     * @throws Exception Nếu có lỗi khi đọc file.
     */
    public static Dataset readFormattedVulsMegaVul(String path) throws Exception {
        Dataset dataset = new Dataset();
        ObjectMapper objectMapper = new ObjectMapper();
    
        int totalRecords = 0;
        int validRecords = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                totalRecords++;
                // Mỗi dòng là 1 JSON object
                JsonNode jsonNode = objectMapper.readTree(line);
                JsonNode isVul = jsonNode.get("is_vul");
                if (isVul != null) {
                    boolean isVulFlag = false;
                    if (isVul.isBoolean()) {
                        isVulFlag = isVul.asBoolean();
                    } else {
                        isVulFlag = "true".equalsIgnoreCase(isVul.asText());
                    }
                    if (isVulFlag) {

                        JsonNode filePath = jsonNode.get("file_path");
                        JsonNode funcBefore = jsonNode.get("func_before");
                        JsonNode diffLineInfo = jsonNode.get("diff_line_info");
                        // System.out.println(line);
                        // System.out.println(jsonNode.toPrettyString());
                        // System.out.println(diffLineInfo.toString().trim().length()>= 5);
                        if (filePath != null && funcBefore != null 
                                && diffLineInfo != null 
                                && diffLineInfo.toString().trim().length() >= 5) {
                            String header = DatasetUtils.extractMethodHeader(funcBefore.asText());
                            dataset.addPair(filePath.asText(), header, funcBefore.asText());
                            validRecords++;
                        } else {
                            // System.out.println("[DEBUG] Record skipped: file_path=" + filePath 
                            //     + ", func_before=" + funcBefore 
                            //     + ", diff_line_info=" + diffLineInfo);
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        System.out.println("[DEBUG] Total records in file: " + totalRecords);
        System.out.println("[DEBUG] Valid records added: " + validRecords);
        return dataset;
    }
    
    // public static Dataset readFormattedVulsMegaVul(String path) throws Exception {
    //     Dataset dataset = new Dataset();
    //     ObjectMapper objectMapper = new ObjectMapper();

    //     // Read entire file content into a String
    //     StringBuilder sb = new StringBuilder();
    //     int totalRecords = 0;
    //     int validRecords = 0;
    //     try (BufferedReader br = new BufferedReader(new FileReader(path))) {
    //         String line;
    //         while ((line = br.readLine()) != null) {
    //             sb.append(line);
    //         }
    //     } catch (IOException e) {
    //         System.err.println("Error reading file: " + e.getMessage());
    //     }
    //     String fileContent = sb.toString();

    //     // Parse the entire content as a JSON array
    //     JsonNode root = objectMapper.readTree(fileContent);
    //     if (root.isArray()) {
    //         for (JsonNode jsonNode : root) {
    //             totalRecords++;

    //             JsonNode isVul = jsonNode.get("is_vul");
    //             if (isVul != null) {
    //                 boolean isVulFlag = false;
    //                 if (isVul.isBoolean()) {
    //                     isVulFlag = isVul.asBoolean();
    //                 } else {
    //                     isVulFlag = "true".equalsIgnoreCase(isVul.asText());
    //                 }

    //                 // Vulnerable record: is_vul == true
    //                 if (isVulFlag) {
    //                     JsonNode filePath = jsonNode.get("file_path");
    //                     JsonNode funcBefore = jsonNode.get("func_before");
    //                     JsonNode diffLineInfo = jsonNode.get("diff_line_info");
    //                     // Kiểm tra diff_line_info
    //                     if (filePath != null && funcBefore != null 
    //                             && diffLineInfo != null 
    //                             && diffLineInfo.toString().trim().length() >= 5) {
    //                         String funcBeforeText = funcBefore.asText();

    //                         String header = extractMethodHeader(funcBeforeText);
    //                         String codeWithoutHeader = "";
                            
    //                         if (funcBeforeText.length() > header.length()) {
    //                             codeWithoutHeader = funcBeforeText.substring(header.length()).trim();
    //                         } else {
    //                             // Nếu toàn bộ chuỗi bằng header hoặc ngắn hơn, ta có thể gán chuỗi rỗng hoặc giữ nguyên
    //                             codeWithoutHeader = funcBeforeText;
    //                         }
    //                         dataset.addPair(filePath.asText(), header, codeWithoutHeader);
    //                         validRecords++;
    //                     } else {
    //                         System.out.println("[DEBUG] Record skipped: file_path=" + filePath 
    //                             + ", func_before=" + funcBefore 
    //                             + ", diff_line_info=" + diffLineInfo);
    //                     }
    //                 }
    //             }
    //         }
    //     }
    //     System.out.println("[DEBUG] Total records in file: " + totalRecords);
    //     System.out.println("[DEBUG] Valid records added: " + validRecords);
    //     return dataset;
    // }

    /**
     * Trích xuất phần header của phương thức từ nội dung code.
     * Header được định nghĩa là phần văn bản trước dấu '{'.
     * @param fileContent Nội dung code của function.
     * @return Header của function, hoặc null nếu không tìm thấy.
     */
    public static String extractMethodHeader(String fileContent) {
        int braceIndex = fileContent.indexOf('{');
        if (braceIndex != -1) {
            // Lấy phần trước dấu '{'
            String header = fileContent.substring(0, braceIndex);
            // Loại bỏ khoảng trắng thừa và định dạng lại header
            header = header
                    .replaceAll("\\s+$", "")
                    .replaceAll("\n", " ")
                    .replaceAll(",\\s*", ", ")
                    .replaceAll("\\(\\s+", "(")
                    .replaceAll("s+\\)", "\\)")
                    .replaceAll("\\s+&", "& ")
                    .replaceAll("\\s+\\*", "* ")
                    .trim();
            return header;
        }
        // Nếu không tìm thấy dấu '{', trả về toàn bộ nội dung hoặc chuỗi rỗng
        return fileContent;
    }
}
