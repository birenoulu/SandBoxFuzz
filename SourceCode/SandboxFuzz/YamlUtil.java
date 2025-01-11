import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YamlUtil {

    public static final List<Pattern> DATA_PATTERN_LIST = Arrays.asList(Pattern.compile("(dataId: .*?_DATA_)(\\d+)"),
                                                                        Pattern.compile("(logicId: .*?_LOGIC_)(\\d+)"),
                                                                        Pattern.compile("(_index: case_)(\\d+)"));

    public static final List<Pattern> LOGIC_PATTERN_LIST = Arrays.asList(Pattern.compile("(logicId: .*?_LOGIC_)(\\d+)"),
                                                                         Pattern.compile("(ccmock:.*?index='case_)(\\d+)"));

    public static final List<Pattern> REQUEST_PATTERN_LIST = Collections.singletonList(Pattern.compile("(case_)(\\d+)"));

    public Map<String,Object> readYaml(String filePath) throws IOException {
        Map<String,Object> data = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            String currentKey = null;
            StringBuilder currentValue = new StringBuilder();
            while ((line = br.readLine()) != null) {
                if (line.startsWith("-")) {
                    if (currentKey != null) {
                        data.put(currentKey, currentValue.toString());
                    }
                    currentKey = line.substring(1).trim();
                    currentValue = new StringBuilder();
                } else {
                    currentValue.append(line).append("\n");
                }
            }
            if (currentKey != null) {
                data.put(currentKey, currentValue.toString());
            }
        }
        return data;
    }

    public void writeYaml(Map<String,Object> data, String filePath) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filePath))) {
            for (Map.Entry<String,Object> entry : data.entrySet()) {
                bw.write("- " + entry.getKey() + "\n");
                bw.write(entry.getValue().toString() + "\n");
            }
        }
    }

    //YAML files are found recursively in the specified root directory and stored in different lists according to the filename and path.
    public static void findYamlFiles(String rootDirectory, List<String> dataYamlFiles, List<String> logicYamlFiles,
                                     List<String> requestYamlFiles, List<String> mockFiles
    ) {
        File rootDir = new File(rootDirectory);
        File[] files = rootDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    //ccmock目录
                    findYamlFiles(file.getAbsolutePath(), dataYamlFiles, logicYamlFiles, requestYamlFiles, mockFiles);
                } else {
                    if (file.isFile() && file.getName().endsWith(".yaml")) {
                        if (file.getParent().toLowerCase().contains("ccmock")) {
                            mockFiles.add(file.getAbsolutePath());
                        } else {
                            String fileName = file.getName().toLowerCase();
                            if (fileName.contains("data.yaml")) {
                                dataYamlFiles.add(file.getAbsolutePath());
                            } else if (fileName.contains("logic.yaml")) {
                                logicYamlFiles.add(file.getAbsolutePath());
                            } else if (fileName.contains("request")) {
                                requestYamlFiles.add(file.getAbsolutePath());
                            }
                        }
                    }
                }
            }
        }
    }

    //Read the YAML file at the specified path and extract the appropriate block based on the type.
    public static List<String> processYamlFile(String filePath, String type) throws Exception {
        List<String> blocks = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            StringBuilder currentBlock = new StringBuilder();
            boolean isInBlock = false;
            String blockStart = getBlockStartPattern(type);

            while ((line = reader.readLine()) != null) {
                if (line.trim().startsWith(blockStart)) {
                    if (isInBlock) {
                        blocks.add(currentBlock.toString());
                        currentBlock.setLength(0);
                    }
                    isInBlock = true;
                }
                if (isInBlock) {
                    if (!line.trim().isEmpty()) {
                        currentBlock.append(line).append("\n");
                    }
                }
            }
            if (isInBlock) {
                blocks.add(currentBlock.toString());
            }
        }
        return blocks;
    }

    private static String getBlockStartPattern(String type) {
        switch (type) {
            case "data":
                return "- !!com.ipay.itest.common.model.TestData";
            case "logic":
                return "- !!com.ipay.itest.common.model.TestLogic";
            case "request":// request block starts with 'case_'
            case "mock":
                return "case_"; // mock block also starts with 'case_'
            default:
                return "";
        }
    }

    //Modify the number part in YAML block according to type and increment value.
    public static String modifyBlockPatternV2(String block, int incrementValue, String type) {
        List<Pattern> resp = new ArrayList<>();
        switch (type) {
            case "data":
                resp = DATA_PATTERN_LIST;
                break;
            case "logic":
                resp = LOGIC_PATTERN_LIST;
                break;
            case "request":
            case "mock":
                resp = REQUEST_PATTERN_LIST;
                break;
            default:
                break;
        }
        if (resp.size() != 0) {
            StringBuilder result = new StringBuilder(block);
            for (Pattern pattern : resp) {
                Matcher matcher = pattern.matcher(result);
                StringBuffer tempBuffer = new StringBuffer();
                while (matcher.find()) {
                    int newValue = Integer.parseInt(matcher.group(2)) + incrementValue;
                    matcher.appendReplacement(tempBuffer, matcher.group(1) + newValue);
                }
                matcher.appendTail(tempBuffer);
                result = new StringBuilder(tempBuffer.toString());
            }
            return result.toString();
        }
        return "";
    }

    //Modify the number part in YAML block according to type and increment value.
    public static String modifyBlockPattern(String block, int incrementValue, String type) {
        List<Pattern> resp = new ArrayList<>();
        switch (type) {
            case "data":
                resp = DATA_PATTERN_LIST;
                break;
            case "logic":
                resp = LOGIC_PATTERN_LIST;
                break;
            case "request":
            case "mock":
                resp = REQUEST_PATTERN_LIST;
                break;
            default:
                break;
        }
        if (resp.size() != 0) {
            StringBuilder result = new StringBuilder(block);
            for (Pattern pattern : resp) {
                Matcher matcher = pattern.matcher(result);
                StringBuffer tempBuffer = new StringBuffer();
                while (matcher.find()) {
                    matcher.appendReplacement(tempBuffer, matcher.group(1) + incrementValue);
                }
                matcher.appendTail(tempBuffer);
                result = new StringBuilder(tempBuffer.toString());
            }
            return result.toString();
        }
        return "";
    }
}
