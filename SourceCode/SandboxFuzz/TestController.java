import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.SecureRandom;
import java.util.Currency;

/**
 * TestController - Controls the iteration and testing process.
 */
public class TestController {

    private YamlUtil   yamlUtil;
    private RedisUtil  redisUtil;
    private Properties config;
    private int        childBaseline;
    private int        favourFactor;
    private int        maxNoUpdateIterations;
    private boolean    maxNoUpdateEnabled;
    private int        crossoverListSize;
    /**
     * generate the number of test cases
     */
    private int        initialNum;

    // save the cases that need to be removed from redis at the end of each round because they are no longer needed as seed mutations to save memory
    public static Set<Integer>     clearList = new HashSet<Integer>();
    // test cases that are in clearList, but temporarily retained by the crossover pool, are later removed from redis once they are removed from the crossover pool
    public static HashSet<Integer> pendList  = new HashSet<Integer>();
    // seed pool, which only houses the test cases that contribute to the coverage
    public static List<TestCase> seedPoolList = new ArrayList<>();
    // crossover pool, maintain coverage in topK test case, small root heap, K is externally configurable parameter
    public static int                     K        = 10;
    public static PriorityQueue<TestCase> topKpool = new PriorityQueue<TestCase>(K, (c1, c2) -> Integer.compare(c1.coverage, c2.coverage));

    private static final String ITERATION_KEY             = "itest_iteration_count";
    private static final String COVERAGE_KEY              = "itest_max_coverage";
    private static final String ITERATION_NO_UPDATE_COUNT = "iteration_no_update_count";
    private static final String CURR_MAX_ID               = "curr_max_id";
    private static final String SEED_POOL_LIST            = "seed_pool_list";
    private static final String SERIALIZE_TIME            = "serialize_time";
    private static final String DESERIALIZE_TIME          = "deserialize_time";
    private static final String SERIALIZE_TIME_TEMP       = "serialize_time_temp";
    private static final String DESERIALIZE_TIME_TEMP     = "deserialize_time_temp";
    private static final String DESERIALIZE_CASE_COUNT    = "deserialize_case_count";
    private static final String SERIALIZE_CASE_COUNT      = "serialize_case_count";
    private static final String MUTATE_FIELDS_TIME        = "mutate_fields_time";
    private static final String MUTATE_FIELDS_COUNT       = "mutate_fields_count";
    private static final String MUTATE_FIELDS_TIME_TEMP   = "mutate_fields_time_temp";
    private static final String MOCK_TIME                 = "mock_time";
    private static final String MOCK_TIME_TEMP            = "mock_time_temp";
    private static final String REQUEST_TIME              = "request_time";
    private static final String REQUEST_TIME_TEMP         = "request_time_temp";

    private static final SecureRandom secureRandom = new SecureRandom();

    public TestController() throws Exception {
        yamlUtil = new YamlUtil();
        redisUtil = new RedisUtil();
        config = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            config.load(input);
        }
        childBaseline = Integer.parseInt(config.getProperty("child_baseline"));
        favourFactor = Integer.parseInt(config.getProperty("favour_factor"));
        maxNoUpdateIterations = Integer.parseInt(config.getProperty("max_no_update_iterations"));
        maxNoUpdateEnabled = Boolean.parseBoolean(config.getProperty("max_no_update_iterations_enabled"));
        crossoverListSize = Integer.parseInt(config.getProperty("crossover_list_size"));
        initialNum = Integer.parseInt(config.getProperty("initial_num"));
    }

    public boolean processInitialRound() throws Exception {
        System.out.println("processInitialRound start-------------");
        // initial logic
        String command = "python LogFilter.py 0";
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
        // execute the maven command
        Process process = pb.start();
        // read input stream and error stream
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        // Printing standard output 
        String s;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }
        // print the error output
        while ((s = stdError.readLine()) != null) {
            System.err.println(s);
        }
        // generate seeds.txt
        String seedsDirectory = config.getProperty("seeds_directory");
        String round0Path = seedsDirectory + "/round_0";
        Files.createDirectories(Paths.get(round0Path));
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(round0Path, "seeds.txt"))) {
            writer.write("1\n");
            for (int i = 1; i <= initialNum; i++) {
                writer.write(i + " + 1\n");
            }
        }
        // generate initial test case data
        addFile(config.getProperty("output_directory"));
        redisUtil.incrementKey(ITERATION_KEY);
        return true;
    }

    private void addFile(String outputDirectory) {
        //data.yaml
        List<String> dataFiles = new ArrayList<>();
        //logic.yaml
        List<String> logicFiles = new ArrayList<>();
        //request.yaml
        List<String> requestFiles = new ArrayList<>();
        //file under ccmock
        List<String> mockFiles = new ArrayList<>();

        // find matching files in the path
        YamlUtil.findYamlFiles(outputDirectory, dataFiles, logicFiles, requestFiles, mockFiles);

        // maximum number of blocks
        // int maxNum = findMaxBlocks(dataFiles, logicFiles, requestFiles, mockFiles);
        // generate initialNum test cases
        for (String dataFile : dataFiles) {
            processYamlFile(dataFile, "data", initialNum);
        }

        for (String logicFile : logicFiles) {
            processYamlFile(logicFile, "logic", initialNum);
        }

        for (String requestFile : requestFiles) {
            processYamlFile(requestFile, "request", initialNum);
        }

        for (String mockFile : mockFiles) {
            processYamlFile(mockFile, "mock", initialNum);
        }

        //maxNum = findMaxBlocks(dataFiles, logicFiles, requestFiles, mockFiles);
        redisUtil.setKeyOrValue(CURR_MAX_ID, initialNum);
    }

    private int findMaxBlocks(List<String> dataFiles, List<String> logicFiles, List<String> requestFiles, List<String> mockFiles) {
        int maxCount = 0;
        try {
            for (String dataFile : dataFiles) {
                List<String> blocks = YamlUtil.processYamlFile(dataFile, "data");
                maxCount = Math.max(maxCount, blocks.size());
            }

            for (String logicFile : logicFiles) {
                List<String> blocks = YamlUtil.processYamlFile(logicFile, "logic");
                maxCount = Math.max(maxCount, blocks.size());
            }

            for (String requestFile : requestFiles) {
                List<String> blocks = YamlUtil.processYamlFile(requestFile, "request");
                maxCount = Math.max(maxCount, blocks.size());
            }

            for (String mockFile : mockFiles) {
                List<String> blocks = YamlUtil.processYamlFile(mockFile, "mock");
                maxCount = Math.max(maxCount, blocks.size());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return maxCount;
    }

    private void processYamlFile(String filePath, String type, int initialNum) {
        try {
            // get source file test case information
            List<String> blocks = YamlUtil.processYamlFile(filePath, type);
            if (blocks.size() == 0) {
                return;
            }
            // calculate the number of blocks to add
            int blocksToAdd = initialNum;
            // take out the last block
            String lastBlock = blocks.get(blocks.size() - 1);
            // boolean firstWrite = true;
            // ipdate an existing file to add the specified number of test cases
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                for (int i = 1; i <= blocksToAdd; i++) {
                    // modify the test case information according to the type to generate a new test case
                    String modifiedBlock = YamlUtil.modifyBlockPattern(lastBlock, i, type);
                    writer.write(modifiedBlock);
                    writer.newLine();
                }
            }
        } catch (Exception e) {
            System.out.println("[TestController] processYamlFile error: " + e.getMessage());
        }
    }

    public boolean processSubsequentRounds() throws Exception {
        System.out.println("processSubsequentRounds start-------------");
        long iteration = redisUtil.getLongByKey(ITERATION_KEY);
        String mavenCommand = config.getProperty("maven_command");
        String seedsDirectory = config.getProperty("seeds_directory");

        try {
            // Execute mvn command with JVM-Sandbox agent using pipe  |  2>&1 stdbuf -oL python LogFilter.py
            String command = "(cd .. && " + mavenCommand + " 2>&1 )" + "| stdbuf -oL python LogFilter.py " + iteration;
            //String command = "(cd .. && " + mavenCommand + " )";
            System.out.println("Executing command: " + command);
            ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
            // execute the maven command
            Process process = pb.start();
            // read the input stream and error stream
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            // Printing standard output
            String s;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }
            // print the error output
            while ((s = stdError.readLine()) != null) {
                System.err.println(s);
            }
        } catch (java.lang.Exception e) {
            System.out.println("[TestController] Error executing command: " + e.getMessage());
        }

        // update max coverage
        String seedsFilePath = seedsDirectory + "/round_" + iteration + "/seeds.txt";
        List<String> seeds = Files.readAllLines(Paths.get(seedsFilePath));

        long maxCoverage = Long.parseLong(seeds.get(0));
        if (maxCoverage > redisUtil.getLongByKey(COVERAGE_KEY)) {
            redisUtil.setKeyOrValue(COVERAGE_KEY, maxCoverage);
            redisUtil.setKeyOrValue(ITERATION_NO_UPDATE_COUNT, "0");
        } else {
            redisUtil.incrementKey(ITERATION_NO_UPDATE_COUNT);
        }

        // If no change in coverage in N rounds, stop the iteration
        if (maxNoUpdateEnabled && redisUtil.getLongByKey(ITERATION_NO_UPDATE_COUNT) >= maxNoUpdateIterations) {
            System.out.println("Maximum coverage has not been updated for " + maxNoUpdateIterations + " iterations. Stopping
            the process.");
            return false;
        }

        // Maintain two pools:
        // Seed pool: holds the test cases that have contributed (+, ++, F+, f ++) to coverage so far (test case id coverage label)
        // Coverage pool: holds the test cases with coverage topK so far (test case id coverage label) (K should be an externally configurable parameter)
        // Maintenance suggestion: keep case ids, coverage and tags for both pools in memory, while the case itself is stored in redis (easy to fetch and construct as object)
        // Remember the id of the current next newly generated case (curr_id)
        // In addition, a global test case id counter is needed to maintain the id of the next newly generated test case (next_id) in real time.
        //
        // Note that all test case ids must be globally unique (monotonically increasing and not repeated across rounds) (e.g. first round: 1,2,3, then second round: 4,5,6)
        long currStartId = redisUtil.getLongByKey(CURR_MAX_ID);
        // the first part, generation is based on seed generation
        seedPoolGeneration(seeds);
        // the second part is based on crossover generation
        crossoverGeneration(seeds);

        // clear the test case in clearList
        for (int id : clearList) {
            String pattern = String.format("case_*_%s", id);
            List<String> keys = redisUtil.getKeyByPattern(pattern);
            if (keys.size() > 0) {
                redisUtil.deleteKey(keys);
            }
        }
        clearList.clear();

        long currEndId = redisUtil.getLongByKey(CURR_MAX_ID);
        long intervalCount = currEndId - currStartId;
        if (0 != intervalCount) {
            // Update yaml id after generating new test case
            updateYamlFile(intervalCount);
        }

        // store the duration of each serialization and deserialization round
        String filePath = seedsDirectory + "/case_time.txt";
        File file = new File(filePath);
        // create a new file if it doesn't exist
        if (!file.exists()) {
            file.createNewFile();
        }

        // use BufferedWriter to write files
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write("round " + iteration + ":");
            writer.newLine(); // add a new line
            long serializeCaseCount = redisUtil.getLongByKey(SERIALIZE_CASE_COUNT);
            writer.write("serialize_case_count: " + serializeCaseCount + ", ");
            long serializeTime = redisUtil.getLongByKey(SERIALIZE_TIME);
            long serializeTimeTemp = 0l;
            if (null != redisUtil.getObjectByKey(SERIALIZE_TIME_TEMP)) {
                serializeTimeTemp = redisUtil.getLongByKey(SERIALIZE_TIME_TEMP);
            }
            writer.write("serialize_time: " + (serializeTime - serializeTimeTemp) + "ms, ");
            writer.write("serialize_case_coverage_time: " + String
                    .format("%.2f", ((double) (serializeTime - serializeTimeTemp) / serializeCaseCount)) + "ms");
            redisUtil.setKeyOrValue(SERIALIZE_TIME_TEMP, serializeTime);

            writer.newLine();
            long deserializeCaseCount = redisUtil.getLongByKey(DESERIALIZE_CASE_COUNT);
            writer.write("deserialize_case_count: " + deserializeCaseCount + ", ");
            long deserializeTime = redisUtil.getLongByKey(DESERIALIZE_TIME);
            long deserializeTimeTemp = 0l;
            if (null != redisUtil.getObjectByKey(DESERIALIZE_TIME_TEMP)) {
                deserializeTimeTemp = redisUtil.getLongByKey(DESERIALIZE_TIME_TEMP);
            }
            writer.write("deserialize_time: " + (deserializeTime - deserializeTimeTemp) + "ms, ");
            writer.write("deserialize_case_coverage_time: " + String
                    .format("%.2f", ((double) (deserializeTime - deserializeTimeTemp) / deserializeCaseCount)) + "ms");
            redisUtil.setKeyOrValue(DESERIALIZE_TIME_TEMP, deserializeTime);
            writer.newLine();
        }

        // store the duration of each round
        String mutateFilePath = seedsDirectory + "/mutate_fields_time.txt";
        File mutateFile = new File(mutateFilePath);
        // create a new file if it doesn't exist
        if (!mutateFile.exists()) {
            mutateFile.createNewFile();
        }

        // use BufferedWriter to write files
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(mutateFile, true))) {
            writer.write("round " + iteration + ":");
            writer.newLine(); // add a new line
            long mutateFieldsCount = redisUtil.getLongByKey(MUTATE_FIELDS_COUNT);
            writer.write("mutate_fields_count: " + mutateFieldsCount + ", ");
            long mutateFieldsTime = redisUtil.getLongByKey(MUTATE_FIELDS_TIME);
            long mutateFieldsTimeTemp = 0l;
            if (null != redisUtil.getObjectByKey(MUTATE_FIELDS_TIME_TEMP)) {
                mutateFieldsTimeTemp = redisUtil.getLongByKey(MUTATE_FIELDS_TIME_TEMP);
            }
            writer.write("mutate_fields_time: " + (mutateFieldsTime - mutateFieldsTimeTemp) + "ms, ");
            writer.write("mutate_fields_case_coverage_time: " + String
                    .format("%.2f", ((double) (mutateFieldsTime - mutateFieldsTimeTemp) / mutateFieldsCount)) + "ms");
            redisUtil.setKeyOrValue(MUTATE_FIELDS_TIME_TEMP, mutateFieldsTime);
            writer.newLine();
        }

        // store the duration of each round
        String excuteFilePath = seedsDirectory + "/excute_time.txt";
        File excuteFile = new File(excuteFilePath);
        // create a new file if it doesn't exist
        if (!excuteFile.exists()) {
            excuteFile.createNewFile();
        }

        // use BufferedWriter to write files
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(excuteFile, true))) {
            writer.write("round " + iteration + ":");
            long mutateFieldsCount = redisUtil.getLongByKey(MUTATE_FIELDS_COUNT);
            writer.write("mutate_fields_count: " + mutateFieldsCount + ", ");
            writer.newLine(); // add a new line
            long mockTime = redisUtil.getLongByKey(MOCK_TIME);
            long mockTimeTemp = 0l;
            if (null != redisUtil.getObjectByKey(MOCK_TIME_TEMP)) {
                mockTimeTemp = redisUtil.getLongByKey(MOCK_TIME_TEMP);
            }
            writer.write("mock_time: " + (mockTime - mockTimeTemp) + "ms, ");
            writer.write(
                    "mock_case_coverage_time: " + String.format("%.2f", ((double) (mockTime - mockTimeTemp) / mutateFieldsCount)) + "ms");
            writer.newLine();
            long requestTime = redisUtil.getLongByKey(REQUEST_TIME);
            long requestTimeTemp = 0l;
            if (null != redisUtil.getObjectByKey(REQUEST_TIME_TEMP)) {
                requestTimeTemp = redisUtil.getLongByKey(REQUEST_TIME_TEMP);
            }
            writer.write("request_time: " + (requestTime - requestTimeTemp) + "ms, ");
            writer.write(
                    "request_case_coverage_time: " + String.format("%.2f", ((double) (requestTime - requestTimeTemp) / mutateFieldsCount))
                            + "ms");
            redisUtil.setKeyOrValue(MOCK_TIME_TEMP, mockTime);
            redisUtil.setKeyOrValue(REQUEST_TIME_TEMP, requestTime);
            writer.newLine();
        }

        // increment iteration count
        redisUtil.incrementKey(ITERATION_KEY);
        // clear test cases 
        redisUtil.setKeyOrValue(SERIALIZE_CASE_COUNT, 0);
        redisUtil.setKeyOrValue(DESERIALIZE_CASE_COUNT, 0);
        redisUtil.setKeyOrValue(MUTATE_FIELDS_COUNT, 0);
        return true;
    }

    public void updateYamlFile(long intervalCount) {
        String outputDirectory = config.getProperty("output_directory");
        //data.yaml
        List<String> dataFiles = new ArrayList<>();
        //logic.yaml
        List<String> logicFiles = new ArrayList<>();
        //request.yaml
        List<String> requestFiles = new ArrayList<>();
        // files under ccmock
        List<String> mockFiles = new ArrayList<>();

        // find matching files in the path
        YamlUtil.findYamlFiles(outputDirectory, dataFiles, logicFiles, requestFiles, mockFiles);

        // the number of test cases generattion is initialNum
        for (String dataFile : dataFiles) {
            processYamlFileV2(dataFile, "data", intervalCount);
        }

        for (String logicFile : logicFiles) {
            processYamlFileV2(logicFile, "logic", intervalCount);
        }

        for (String requestFile : requestFiles) {
            processYamlFileV2(requestFile, "request", intervalCount);
        }

        for (String mockFile : mockFiles) {
            processYamlFileV2(mockFile, "mock", intervalCount);
        }
    }

    private void processYamlFileV2(String filePath, String type, long intervalCount) {
        try {
            // get source file test case information
            List<String> blocks = YamlUtil.processYamlFile(filePath, type);
            if (blocks.size() == 0) {
                return;
            }
            // fetch the last block
            String lastBlock = blocks.get(blocks.size() - 1);
            // overwrite existing file with specified number of test cases
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                for (int i = 1; i <= intervalCount; i++) {
                    // modify the test case information according to the type to generate a new test case
                    String modifiedBlock = YamlUtil.modifyBlockPatternV2(lastBlock, i, type);
                    writer.write(modifiedBlock);
                    writer.newLine();
                }
            }
        } catch (Exception e) {
            System.out.println("[TestController] processYamlFileV2 error " + e.getMessage());
        }
    }

    public void crossoverGeneration(List<String> seedsLines) {
        // the new seed is processed first to see if it wants to be added to the crossover pool
        try {
            for (int i = 1; i < seedsLines.size(); i++) {
                String[] status = seedsLines.get(i).split(" ");
                int id = Integer.parseInt(status[0]);
                String label = status[1];
                int coverage = Integer.parseInt(status[2]);

                if (label.equals("YF") || label.equals("SF") || label.equals("IF")) {
                    // exclude three kinds of test cases
                    continue;
                }
                // put the ones with higher coverage into the pool
                if (topKpool.size() < K) {
                    // add K new test cases
                    topKpool.add(new TestCase(id, coverage, label));
                    // temporarily move the seeds needed for crossover to pendList
                    if (clearList.contains(id)) {
                        clearList.remove(id);
                        pendList.add(id);
                    }
                } else {
                    if (topKpool.peek().getCoverage() < coverage) {
                        // remove the lowest coverage test case in the pool
                        TestCase toRemove = topKpool.poll();
                        // move back from pendList to clearList
                        if (pendList.contains(toRemove.getId())) {
                            pendList.remove(toRemove.getId());
                            clearList.add(toRemove.getId());
                        }
                        // add test cases with higher coverage
                        topKpool.add(new TestCase(id, coverage, label));
                        // temporarily move the seeds needed for crossover to pendList
                        if (clearList.contains(id)) {
                            clearList.remove(id);
                            pendList.add(id);
                        }
                    }
                }
            }
        } catch (java.lang.Exception e) {
            System.out.println("Error in crossoverGeneration: " + e.getMessage());
            e.printStackTrace();
        }

        // add seeds to temporary list for later pairwise operations
        List<TestCase> crossoverSeeds = null;
        try {
            Iterator<TestCase> iterator = topKpool.iterator();
            crossoverSeeds = new LinkedList<TestCase>();
            while (iterator.hasNext()) {
                TestCase currentTestCase = iterator.next();
                crossoverSeeds.add(currentTestCase);
            }
        } catch (java.lang.Exception e) {
            System.out.println("Error in crossoverGeneration  crossoverSeeds: " + e.getMessage());
            e.printStackTrace();
        }

        // new test case generation basing  on the updated crossover pool
        // we can't use K here because topKpool is not always full
        try {
            int crossoverCnt = crossoverSeeds.size();
            for (int i = 0; i < crossoverCnt - 1; i++) {
                for (int j = i + 1; j < crossoverCnt; j++) {
                    long currid = (long) redisUtil.incrementKey(CURR_MAX_ID);
                    long currid_1 = (long) redisUtil.incrementKey(CURR_MAX_ID);
                    long currid_2 = (long) redisUtil.incrementKey(CURR_MAX_ID);
                    long currid_3 = (long) redisUtil.incrementKey(CURR_MAX_ID);
                    int case1ID = crossoverSeeds.get(i).getId();
                    int case2ID = crossoverSeeds.get(j).getId();
                    String pattern = String.format("case_*_%s", case1ID);
                    // get the key stored by redis
                    List<String> keys = redisUtil.getKeyByPattern(pattern);
                    for (String key : keys) {
                        try {
                            // to merge the two test case information into one, we need to take the first half of the fields of seed1 and the second half of the fields of seed2
                            Object seed1 = redisUtil.loadTestCaseObject(key);
                            String[] parts = key.split("_");
                            parts[parts.length - 1] = String.valueOf(case2ID);
                            // coalesce into a new string
                            String newKey = String.join("_", parts);
                            Object seed2 = redisUtil.loadTestCaseObject(newKey);
                            long start = System.currentTimeMillis();
                            Object newSeed1 = MutationUtil.mutateCrossover(seed1, seed2);
                            long time = System.currentTimeMillis() - start;
                            redisUtil.incrementKey(MUTATE_FIELDS_TIME, time);
                            redisUtil.incrementKey(MUTATE_FIELDS_COUNT, 1);

                            // store the new test case
                            parts = key.split("_");
                            parts[parts.length - 1] = String.valueOf(currid);
                            // coalesce into a new string
                            newKey = String.join("_", parts);
                            redisUtil.saveTestCaseObject(newSeed1, newKey);

                            // mutation
                            start = System.currentTimeMillis();
                            Object modifyTestCase = modifyFields(newSeed1, new HashSet<>());
                            time = System.currentTimeMillis() - start;
                            redisUtil.incrementKey(MUTATE_FIELDS_TIME, time);
                            redisUtil.incrementKey(MUTATE_FIELDS_COUNT, 1);

                            // store the new test case
                            parts[parts.length - 1] = String.valueOf(currid_1);
                            // coalesce into a new string
                            newKey = String.join("_", parts);
                            redisUtil.saveTestCaseObject(modifyTestCase, newKey);

                            // to merge the two test case information into one, we need to take the last half of the fields of seed1 and the first half of the fields of seed2
                            start = System.currentTimeMillis();
                            Object newSeed2 = MutationUtil.mutateCrossover(seed2, seed1);
                            time = System.currentTimeMillis() - start;
                            redisUtil.incrementKey(MUTATE_FIELDS_TIME, time);
                            redisUtil.incrementKey(MUTATE_FIELDS_COUNT, 1);

                            // store the new test case
                            parts[parts.length - 1] = String.valueOf(currid_2);
                            // coalesce into a new string
                            newKey = String.join("_", parts);
                            redisUtil.saveTestCaseObject(newSeed2, newKey);

                            // mutation
                            start = System.currentTimeMillis();
                            Object modifyTestCase1 = modifyFields(newSeed2, new HashSet<>());
                            time = System.currentTimeMillis() - start;
                            redisUtil.incrementKey(MUTATE_FIELDS_TIME, time);
                            redisUtil.incrementKey(MUTATE_FIELDS_COUNT, 1);

                            // store the new test case
                            parts[parts.length - 1] = String.valueOf(currid_3);
                            // coalesce into a new string
                            newKey = String.join("_", parts);
                            redisUtil.saveTestCaseObject(modifyTestCase1, newKey);
                        } catch (java.lang.Exception e) {
                            System.out.println("[TestController] Generation testCase error " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (java.lang.Exception e) {
            System.out.println("mutateCrossover error:" + e.getMessage());
            e.printStackTrace();
        }
    }

    public void seedPoolGeneration(List<String> seedsLines) {
        long iteration = redisUtil.getLongByKey(ITERATION_KEY);
        long maxCoverage = redisUtil.getLongByKey(COVERAGE_KEY);
        if (seedPoolList != null) {
            // Process the old seeds and generate new tetst cases
            for (TestCase testCase : seedPoolList) {
                int seedCoverage = testCase.getCoverage();
                String label = testCase.getLabel();
                int id = testCase.getId();
                addTestCase(id, label, seedCoverage, maxCoverage);
            }
        }
        // process new seeds, add them to the seed pool, and generate new test cases
        for (int i = 1; i < seedsLines.size(); i++) {
            // Parse each line in seeds.txt
            String[] status = seedsLines.get(i).split(" ");
            int id = Integer.parseInt(status[0]);
            String label = status[1];
            int coverage = Integer.parseInt(status[2]);
            boolean result = addTestCase(id, label, coverage, maxCoverage);
            // add new seeds to the seed pool
            if (result) {
                seedPoolList.add(new TestCase(id, coverage, label));
            }
        }
    }

    public boolean addTestCase(int id, String label, int seedCoverage, long maxCoverage) {
        Set<Object> visited = new HashSet<>();
        /**
         * Parameter description:
         * child_baseline：For each seed, the number of benchmarks to generate child inputs
         * seed_coverage：The coverage of the current seed
         * max_coverage：The coverage achieved by the union of all seeds in the current pool (this is returned in seeds.txt in the previous result)
         * favor_factor：1 for + and F；M for ++ and F++（M should be an externally configurable parameter）
         */
        // check if this is a new seed and how many seeds to generate
        long numOfChildren = 0;
        if (label.equals("++") || label.equals("F++")) {
            numOfChildren = num_generation_favour(seedCoverage, maxCoverage);
        } else if (label.equals("+") || label.equals("F+")) {
            numOfChildren = num_generation(seedCoverage, maxCoverage);
        } else {
            // This test case does not contribute to coverage, add it to the list to be cleared
            // If this test case is also not required by the crossover pool, it can be removed from redis later
            clearList.add(id);
            return false;
        }
        // generate a new test case based on the current seed test case
        for (int i = 0; i < numOfChildren; i++) {
            long currid = (long) redisUtil.incrementKey(CURR_MAX_ID);
            String pattern = String.format("case_*_%s", id);
            List<String> keys = redisUtil.getKeyByPattern(pattern);
            for (String key : keys) {
                // get initial data
                Object seed = redisUtil.loadTestCaseObject(key);
                visited = new HashSet<>();
                // do mutation
                long start = System.currentTimeMillis();
                Object modifyTestCase = modifyFields(seed, visited);
                long time = System.currentTimeMillis() - start;
                redisUtil.incrementKey(MUTATE_FIELDS_TIME, time);
                redisUtil.incrementKey(MUTATE_FIELDS_COUNT, 1);
                // save new test case
                String[] parts = key.split("_");
                parts[parts.length - 1] = String.valueOf(currid);
                // coalesce into a new string
                String newKey = String.join("_", parts);
                redisUtil.saveTestCaseObject(modifyTestCase, newKey);
            }
        }
        return true;
    }

    public Object modifyFields(Object obj, Set<Object> visited) {
        if (obj == null || visited.contains(obj)) {
            return obj; // return if the object is null or has been accessed
        }
        // mark the object as accessed
        visited.add(obj);
        if (!isPrimitiveOrWrapper(obj.getClass())) {
            return MutationUtil.mutate(obj, obj.getClass(), null);
        }
        List<Field> fields = getAllMutableFields(obj.getClass());
        int totalFields = fields.size();
        if (totalFields == 0) {
            return obj;
        }
        int meanTime = Math.max(1, totalFields / 2);
        int mutateTimes = GeometricSample.sampleGeometricTimes(meanTime);
        if (mutateTimes == 0) {
            return obj;
        }
        int[] resultArray = new int[mutateTimes];
        for (int i = 0; i < mutateTimes; i++) {
            resultArray[i] = secureRandom.nextInt(totalFields);
        }
        for (int i : resultArray) {
            Field field = fields.get(i);
            // check if the field is static
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                // skip static fields and Final
                continue;
            }
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                Class<?> fieldType = field.getType();
                if (isPrimitiveOrWrapper(fieldType)) {
                    if (null == value) {
                        field.set(obj, MutationUtil.mutateObject(fieldType));
                    } else {
                        // If nested, call recursively
                        modifyFields(value, visited);
                    }
                } else {
                    Type genericType = field.getGenericType();
                    Object mutatedValue = MutationUtil.mutate(value, fieldType, genericType);

                    // check if field is ConcurrentMap
                    if (ConcurrentMap.class.isAssignableFrom(fieldType) && mutatedValue != null) {
                        Map mapA = (Map) mutatedValue;
                        // remove null values
                        mapA.values().removeIf(Objects::isNull);
                        field.set(obj, new ConcurrentHashMap<>(mapA));
                    } else {
                        field.set(obj, mutatedValue);
                    }
                }
            } catch (Exception e) {
                System.out.println("[TestController] modifyFields error:" + e.getMessage());
            }
        }
        return obj;
    }

    private static List<Field> getAllMutableFields(Class<?> clazz) {
        List<Field> fieldList = new ArrayList<>();
        getAllMutableFieldsRecursive(clazz, fieldList);
        return fieldList;
    }

    private static void getAllMutableFieldsRecursive(Class<?> clazz, List<Field> fieldList) {
        try {
            if (clazz == null || clazz == Object.class) {
                return;
            }

            // get all the fields and check
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                // if the field is not final and mutable
                if (!Modifier.isFinal(field.getModifiers()) && !fieldList.contains(field) && !Modifier.isStatic(field.getModifiers())) {
                    fieldList.add(field);
                }
            }

            // recursive call handles the parent class
            getAllMutableFieldsRecursive(clazz.getSuperclass(), fieldList);
        } catch (Exception e) {
            System.out.println("[TestController] getAllMutableFieldsRecursive error:" + e.getMessage());
            e.printStackTrace(); // output stack info for debugging
        }
    }

    public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            return false;
        }
        // if it's not primitive, it's considered nested
        return clazz != Boolean.class && clazz != Character.class && clazz != Byte.class && clazz != Short.class && clazz != Integer.class
                && clazz != Long.class && clazz != Float.class && clazz != Double.class && clazz != Date.class && clazz != String.class
                && !clazz.isEnum() && !(List.class.isAssignableFrom(clazz)) && !(Map.class.isAssignableFrom(clazz))
                && clazz != Instant.class && clazz != Currency.class;
    }

    private Map<String,Object> modifyHashMapFields(HashMap<String,Object> map, Set<Object> visited) {
        int size = map.size();
        if (size == 0) {
            return map;
        }

        int mutateTimes = GeometricSample.sampleGeometricTimes(Math.max(1, (int) Math.floor(Math.log(size) / Math.log(2))));

        for (int i = 0; i < mutateTimes; i++) {
            int randomIndex = secureRandom.nextInt(size);
            Object key = map.keySet().toArray()[randomIndex];
            Object value = map.get(key.toString());

            if (value != null) {
                // Mutate the value if it's an object or a primitive/wrapper
                if (isPrimitiveOrWrapper(value.getClass())) {
                    modifyFields(value, visited);  // Recursive call for nested objects
                }

                // Mutate the value
                Object mutatedValue = MutationUtil.mutate(value, value.getClass(), null);
                map.put(key.toString(), mutatedValue);
            }
        }
        return map;
    }

    private long num_generation_favour(int coverage, long maxCoverage) {
        if (maxCoverage == 0) {
            return childBaseline;
        } else {
            long result = (long) childBaseline * coverage * favourFactor;
            return result / maxCoverage;
        }
    }

    private long num_generation(int coverage, long maxCoverage) {
        if (maxCoverage == 0) {
            return childBaseline;
        } else {
            long result = (long) childBaseline * coverage;
            return result / maxCoverage;
        }
    }

    private long num_generation_favour_with_baseline(int coverage, long maxCoverage, int baseline) {
        if (maxCoverage == 0) {
            return baseline;
        } else {
            long result = (long) baseline * coverage * favourFactor;
            return result / maxCoverage;
        }
    }

    private long num_generation_with_baseline(int coverage, long maxCoverage, int baseline) {
        if (maxCoverage == 0) {
            return baseline;
        } else {
            long result = (long) baseline * coverage;
            return result / maxCoverage;
        }
    }

    public void startTesting() throws Exception {
        boolean result = true;
        while (result) {
            try {
                long iteration = redisUtil.getLongByKey(ITERATION_KEY);
                if (iteration == 0) {
                    result = processInitialRound();
                } else {
                    result = processSubsequentRounds();
                }
            } catch (Exception e) {
                e.printStackTrace();
                result = false;
            }
        }
        // saving history recode
        String sourcePath = "/home/admin/lq01145628/internal_release/igtransferprod/sand-box/fuzzing_results";
        String filePath = "/home/admin/lq01145628/internal_release/igtransferprod/sand-box/fuzzing_results_new";

        // find an existing round_x directory
        int roundNumber = getNextRoundNumber(filePath);
        String newDirectoryPath = filePath + "/round_" + roundNumber;
        // creating a new directory
        File newDir = new File(newDirectoryPath);
        if (!newDir.exists()) {
            newDir.mkdirs();
        }
        createAndCopy(sourcePath, newDirectoryPath);

        //保存前100用例记录
        /*String sourceLogPath1 = "/home/admin/lq01145628/internal_release/igtransferprod/sand-box/sandbox_log";
        String filePath1 = "/home/admin/lq01145628/internal_release/igtransferprod/sand-box/sandbox_log_new";

        // 查找当前已有的 round_x 目录
        int roundNumber1 = getNextRoundNumber(filePath1);
        String newDirectoryPath1 = filePath1 + "/round_" + roundNumber1;
        // 创建新目录
        File newDir1 = new File(newDirectoryPath1);
        if (!newDir1.exists()) {
            newDir1.mkdirs();
        }
        createAndCopy(sourceLogPath1, newDirectoryPath1);*/
        //清空mock.json  和request.json
        //clearFileContent("/home/admin/lq01145628/internal_release/igtransferprod/sand-box/sandbox_log/mock.json");
        //clearFileContent("/home/admin/lq01145628/internal_release/igtransferprod/sand-box/sandbox_log/request.json");
    }

    public static void clearFileContent(String filePath) {
        File file = new File(filePath);
        try (FileWriter fileWriter = new FileWriter(file)) {
            // clear the contents of the file
            fileWriter.write("");
        } catch (IOException e) {
            System.err.println("An error occurred while clearing the file: " + e.getMessage());
        }
    }

    public static void createAndCopy(String sourcePathStr, String targetPathStr) {
        Path sourcePath = Paths.get(sourcePathStr);
        Path targetPath = Paths.get(targetPathStr);

        // copy the file
        try {
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path targetFilePath = targetPath.resolve(sourcePath.relativize(file));
                    Files.createDirectories(targetFilePath.getParent()); // create parent directory
                    Files.copy(file, targetFilePath, StandardCopyOption.REPLACE_EXISTING);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path targetDirPath = targetPath.resolve(sourcePath.relativize(dir));
                    Files.createDirectories(targetDirPath);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static int getNextRoundNumber(String basePath) {
        File baseDir = new File(basePath);
        int highestNumber = 0;

        // get the existing round_x directory
        File[] files = baseDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && file.getName().matches("round_\\d+")) {
                    String name = file.getName().substring(6); // get the number part
                    try {
                        int number = Integer.parseInt(name);
                        highestNumber = Math.max(highestNumber, number);
                    } catch (Exception e) {
                        // if the conversion fails, skip
                    }
                }
            }
        }
        return highestNumber + 1; // next iteration
    }

    public static void main(String[] args) {
        try {
            TestController controller = new TestController();
            controller.startTesting();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

