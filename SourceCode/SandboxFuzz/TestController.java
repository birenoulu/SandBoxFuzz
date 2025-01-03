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
     * 生成用例数
     */
    private int        initialNum;

    // 保存每一轮结束后，因不再需要作为种子变异而需要被从redis中移除的用例，以节省内存
    public static Set<Integer>     clearList = new HashSet<Integer>();
    // 在clearList中，但是暂时被crossover池保留的用例，之后一旦从crossover池中移除，则从redis中移除
    public static HashSet<Integer> pendList  = new HashSet<Integer>();
    // 种子池，只存放对覆盖率有贡献的用例
    public static List<TestCase> seedPoolList = new ArrayList<>();
    // crossover池, 维护覆盖率在topK的用例，小根堆，K为外部可配置参数
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
        // 初始逻辑
        String command = "python LogFilter.py 0";
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", command);
        //执行maven命令
        Process process = pb.start();
        // 读取输入流和错误流
        BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
        BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        // 打印标准输出
        String s;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }
        // 打印错误输出
        while ((s = stdError.readLine()) != null) {
            System.err.println(s);
        }
        // 生成seeds.txt
        String seedsDirectory = config.getProperty("seeds_directory");
        String round0Path = seedsDirectory + "/round_0";
        Files.createDirectories(Paths.get(round0Path));
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(round0Path, "seeds.txt"))) {
            writer.write("1\n");
            for (int i = 1; i <= initialNum; i++) {
                writer.write(i + " + 1\n");
            }
        }
        //生成初始用例数据
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
        //ccmock下的文件
        List<String> mockFiles = new ArrayList<>();

        //查找路径下符合的文件
        YamlUtil.findYamlFiles(outputDirectory, dataFiles, logicFiles, requestFiles, mockFiles);

        // 最大块数量
        //int maxNum = findMaxBlocks(dataFiles, logicFiles, requestFiles, mockFiles);
        //生成initialNum个测试用例
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
            //获取源文件用例信息
            List<String> blocks = YamlUtil.processYamlFile(filePath, type);
            if (blocks.size() == 0) {
                return;
            }
            // 计算需要添加的块数
            int blocksToAdd = initialNum;
            // 取出最后一个块
            String lastBlock = blocks.get(blocks.size() - 1);
            //boolean firstWrite = true;
            //更新现有文件，增加指定用例数
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                for (int i = 1; i <= blocksToAdd; i++) {
                    //根据类型修改用例信息，生成新的用例
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
            //执行maven命令
            Process process = pb.start();
            // 读取输入流和错误流
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            // 打印标准输出
            String s;
            while ((s = stdInput.readLine()) != null) {
                System.out.println(s);
            }
            // 打印错误输出
            while ((s = stdError.readLine()) != null) {
                System.err.println(s);
            }
        } catch (java.lang.Exception e) {
            System.out.println("[TestController] Error executing command: " + e.getMessage());
        }

        // 更新max coverage
        String seedsFilePath = seedsDirectory + "/round_" + iteration + "/seeds.txt";
        List<String> seeds = Files.readAllLines(Paths.get(seedsFilePath));

        long maxCoverage = Long.parseLong(seeds.get(0));
        if (maxCoverage > redisUtil.getLongByKey(COVERAGE_KEY)) {
            redisUtil.setKeyOrValue(COVERAGE_KEY, maxCoverage);
            redisUtil.setKeyOrValue(ITERATION_NO_UPDATE_COUNT, "0");
        } else {
            redisUtil.incrementKey(ITERATION_NO_UPDATE_COUNT);
        }

        //如果N轮覆盖率没有变化，停止迭代
        if (maxNoUpdateEnabled && redisUtil.getLongByKey(ITERATION_NO_UPDATE_COUNT) >= maxNoUpdateIterations) {
            System.out.println("Maximum coverage has not been updated for " + maxNoUpdateIterations + " iterations. Stopping
            the process.");
            return false;
        }

        //维护两个池：
        //种子池：保存了到目前为止中，对覆盖率有贡献（+、++、F+、F++）的用例 (用例id 覆盖率 标签)
        //覆盖率池：保存了到目前为止，覆盖率topK的用例 (用例id 覆盖率 标签) （K应该是一个外部可配置调整的参数）
        //维护建议：在内存中维护两个池的用例id，覆盖率和标签，而用例本身存放在redis里（方便取出然后构造为对象）
        //记住当前下一个新生成用例的id（curr_id）
        //此外，需要一个全局的用例id计数器，实时维护下一个新生成用例的id（next_id）
        //
        //注意，所有的用例id一定是全局唯一的（id单调递增，不同轮次之间id不重复）（例如第一轮：1，2，3，那么第二轮就是：4，5，6）
        long currStartId = redisUtil.getLongByKey(CURR_MAX_ID);
        // 第一部分，生成基于种子生成
        seedPoolGeneration(seeds);
        // 第二部分，基于crossover生成
        crossoverGeneration(seeds);

        // 清除clearList中的用例
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
            // 生成完新用例需要用更新yaml id自增
            updateYamlFile(intervalCount);
        }

        //存储每一轮序列化和反序列化运行时长
        String filePath = seedsDirectory + "/case_time.txt";
        File file = new File(filePath);
        // 如果文件不存在，则创建新文件
        if (!file.exists()) {
            file.createNewFile();
        }

        // 使用 BufferedWriter 来写文件
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write("round " + iteration + ":");
            writer.newLine(); // 添加新行
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

        //存储每一轮变异时长
        String mutateFilePath = seedsDirectory + "/mutate_fields_time.txt";
        File mutateFile = new File(mutateFilePath);
        // 如果文件不存在，则创建新文件
        if (!mutateFile.exists()) {
            mutateFile.createNewFile();
        }

        // 使用 BufferedWriter 来写文件
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(mutateFile, true))) {
            writer.write("round " + iteration + ":");
            writer.newLine(); // 添加新行
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

        //存储每一轮执行时长
        String excuteFilePath = seedsDirectory + "/excute_time.txt";
        File excuteFile = new File(excuteFilePath);
        // 如果文件不存在，则创建新文件
        if (!excuteFile.exists()) {
            excuteFile.createNewFile();
        }

        // 使用 BufferedWriter 来写文件
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(excuteFile, true))) {
            writer.write("round " + iteration + ":");
            long mutateFieldsCount = redisUtil.getLongByKey(MUTATE_FIELDS_COUNT);
            writer.write("mutate_fields_count: " + mutateFieldsCount + ", ");
            writer.newLine(); // 添加新行
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

        // Increment iteration count
        redisUtil.incrementKey(ITERATION_KEY);
        //用例数清空
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
        //ccmock下的文件
        List<String> mockFiles = new ArrayList<>();

        //查找路径下符合的文件
        YamlUtil.findYamlFiles(outputDirectory, dataFiles, logicFiles, requestFiles, mockFiles);

        //生成initialNum个测试用例
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
            //获取源文件用例信息
            List<String> blocks = YamlUtil.processYamlFile(filePath, type);
            if (blocks.size() == 0) {
                return;
            }
            // 取出最后一个块
            String lastBlock = blocks.get(blocks.size() - 1);
            //覆盖现有文件，增加指定用例数
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
                for (int i = 1; i <= intervalCount; i++) {
                    //根据类型修改用例信息，生成新的用例
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
        // 先处理新的种子，看是否要加入到crossover池中
        try {
            for (int i = 1; i < seedsLines.size(); i++) {
                String[] status = seedsLines.get(i).split(" ");
                int id = Integer.parseInt(status[0]);
                String label = status[1];
                int coverage = Integer.parseInt(status[2]);

                if (label.equals("YF") || label.equals("SF") || label.equals("IF")) {
                    // 排除这三种用例
                    continue;
                }
                // 将覆盖率更高的放进池中
                if (topKpool.size() < K) {
                    // 添加K个新的用例
                    topKpool.add(new TestCase(id, coverage, label));
                    // 将crossover需要的种子暂时移动到pendList
                    if (clearList.contains(id)) {
                        clearList.remove(id);
                        pendList.add(id);
                    }
                } else {
                    if (topKpool.peek().getCoverage() < coverage) {
                        // 删除池中覆盖率最低的用例
                        TestCase toRemove = topKpool.poll();
                        // 从pendList重新移动回clearList
                        if (pendList.contains(toRemove.getId())) {
                            pendList.remove(toRemove.getId());
                            clearList.add(toRemove.getId());
                        }
                        // 添加覆盖率更高的用例
                        topKpool.add(new TestCase(id, coverage, label));
                        // 将crossover需要的种子暂时移动到pendList
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

        // 将堆中的种子添加到临时列表中，方便后续两两操作
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

        // 然后对更新后的crossover池做新用例生成
        // 因为topKpool不一定满，所以这里不能直接用K作为大小
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
                    //获取redis存的key
                    List<String> keys = redisUtil.getKeyByPattern(pattern);
                    for (String key : keys) {
                        try {
                            // 将两个用例信息合并为一个,这里需要取seed1的前一半字段和seed2的后一半字段
                            Object seed1 = redisUtil.loadTestCaseObject(key);
                            String[] parts = key.split("_");
                            parts[parts.length - 1] = String.valueOf(case2ID);
                            // 合并成新字符串
                            String newKey = String.join("_", parts);
                            Object seed2 = redisUtil.loadTestCaseObject(newKey);
                            long start = System.currentTimeMillis();
                            Object newSeed1 = MutationUtil.mutateCrossover(seed1, seed2);
                            long time = System.currentTimeMillis() - start;
                            redisUtil.incrementKey(MUTATE_FIELDS_TIME, time);
                            redisUtil.incrementKey(MUTATE_FIELDS_COUNT, 1);

                            //存储新用例
                            parts = key.split("_");
                            parts[parts.length - 1] = String.valueOf(currid);
                            // 合并成新字符串
                            newKey = String.join("_", parts);
                            redisUtil.saveTestCaseObject(newSeed1, newKey);

                            //进行变异
                            start = System.currentTimeMillis();
                            Object modifyTestCase = modifyFields(newSeed1, new HashSet<>());
                            time = System.currentTimeMillis() - start;
                            redisUtil.incrementKey(MUTATE_FIELDS_TIME, time);
                            redisUtil.incrementKey(MUTATE_FIELDS_COUNT, 1);

                            //存储新用例
                            parts[parts.length - 1] = String.valueOf(currid_1);
                            // 合并成新字符串
                            newKey = String.join("_", parts);
                            redisUtil.saveTestCaseObject(modifyTestCase, newKey);

                            // 将两个用例信息合并为一个,这里需要取seed1的后一半字段和seed2的前一半字段
                            start = System.currentTimeMillis();
                            Object newSeed2 = MutationUtil.mutateCrossover(seed2, seed1);
                            time = System.currentTimeMillis() - start;
                            redisUtil.incrementKey(MUTATE_FIELDS_TIME, time);
                            redisUtil.incrementKey(MUTATE_FIELDS_COUNT, 1);

                            //存储新用例
                            parts[parts.length - 1] = String.valueOf(currid_2);
                            // 合并成新字符串
                            newKey = String.join("_", parts);
                            redisUtil.saveTestCaseObject(newSeed2, newKey);

                            //进行变异
                            start = System.currentTimeMillis();
                            Object modifyTestCase1 = modifyFields(newSeed2, new HashSet<>());
                            time = System.currentTimeMillis() - start;
                            redisUtil.incrementKey(MUTATE_FIELDS_TIME, time);
                            redisUtil.incrementKey(MUTATE_FIELDS_COUNT, 1);

                            //存储新用例
                            parts[parts.length - 1] = String.valueOf(currid_3);
                            // 合并成新字符串
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
            // 先处理旧的种子，生成新用例
            for (TestCase testCase : seedPoolList) {
                int seedCoverage = testCase.getCoverage();
                String label = testCase.getLabel();
                int id = testCase.getId();
                addTestCase(id, label, seedCoverage, maxCoverage);
            }
        }
        // 然后处理新的种子，加入到种子池中，并生成新用例
        for (int i = 1; i < seedsLines.size(); i++) {
            // 解析seeds.txt中的每一行
            String[] status = seedsLines.get(i).split(" ");
            int id = Integer.parseInt(status[0]);
            String label = status[1];
            int coverage = Integer.parseInt(status[2]);
            boolean result = addTestCase(id, label, coverage, maxCoverage);
            // 添加新种子到种子池中
            if (result) {
                seedPoolList.add(new TestCase(id, coverage, label));
            }
        }
    }

    public boolean addTestCase(int id, String label, int seedCoverage, long maxCoverage) {
        Set<Object> visited = new HashSet<>();
        /**
         * 参数说明：
         * child_baseline：对于每个种子，生成子输入的基准数量
         * seed_coverage：当前种子的覆盖率
         * max_coverage：当前池中所有种子的并集达到的覆盖率（这个在上一轮结果中的seeds.txt里返回了）
         * favor_factor：对于+和F+，为1；对于++和F++，为M（M应该是一个外部可配置调整的参数）
         */
        // 判断是否为新种子，以及对应要生成种子数量
        long numOfChildren = 0;
        if (label.equals("++") || label.equals("F++")) {
            numOfChildren = num_generation_favour(seedCoverage, maxCoverage);
        } else if (label.equals("+") || label.equals("F+")) {
            numOfChildren = num_generation(seedCoverage, maxCoverage);
        } else {
            // 这个用例对覆盖率无贡献，加入到待清除列表中
            // 如果这个用例同样也不是crossover池子里面所需要的，后面就可以从redis中移除
            clearList.add(id);
            return false;
        }
        // 根据当前种子用例，生成新用例
        for (int i = 0; i < numOfChildren; i++) {
            long currid = (long) redisUtil.incrementKey(CURR_MAX_ID);
            String pattern = String.format("case_*_%s", id);
            List<String> keys = redisUtil.getKeyByPattern(pattern);
            for (String key : keys) {
                //获取初始对象数据
                Object seed = redisUtil.loadTestCaseObject(key);
                visited = new HashSet<>();
                //进行变异
                long start = System.currentTimeMillis();
                Object modifyTestCase = modifyFields(seed, visited);
                long time = System.currentTimeMillis() - start;
                redisUtil.incrementKey(MUTATE_FIELDS_TIME, time);
                redisUtil.incrementKey(MUTATE_FIELDS_COUNT, 1);
                //存储新用例
                String[] parts = key.split("_");
                parts[parts.length - 1] = String.valueOf(currid);
                // 合并成新字符串
                String newKey = String.join("_", parts);
                redisUtil.saveTestCaseObject(modifyTestCase, newKey);
            }
        }
        return true;
    }

    public Object modifyFields(Object obj, Set<Object> visited) {
        if (obj == null || visited.contains(obj)) {
            return obj; // 如果对象为 null 或已访问，则返回
        }
        // 标记当前对象为已访问
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
            // 检查字段是否是静态的
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                // 跳过静态字段以及Final
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
                        // 如果是嵌套对象，递归调用
                        modifyFields(value, visited);
                    }
                } else {
                    Type genericType = field.getGenericType();
                    Object mutatedValue = MutationUtil.mutate(value, fieldType, genericType);

                    // 检查字段是否为 ConcurrentMap 类型
                    if (ConcurrentMap.class.isAssignableFrom(fieldType) && mutatedValue != null) {
                        Map mapA = (Map) mutatedValue;
                        // 移除 null 值
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

            // 获得所有字段并检查
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                // 如果字段不是final且是可变的
                if (!Modifier.isFinal(field.getModifiers()) && !fieldList.contains(field) && !Modifier.isStatic(field.getModifiers())) {
                    fieldList.add(field);
                }
            }

            // 递归调用处理父类
            getAllMutableFieldsRecursive(clazz.getSuperclass(), fieldList);
        } catch (Exception e) {
            System.out.println("[TestController] getAllMutableFieldsRecursive error:" + e.getMessage());
            e.printStackTrace(); // 输出堆栈信息，方便调试
        }
    }

    public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
        if (clazz.isPrimitive()) {
            return false;
        }
        //如果不是基本类型，则认为是嵌套对象
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
        //保存历史记录
        String sourcePath = "/home/admin/lq01145628/internal_release/igtransferprod/sand-box/fuzzing_results";
        String filePath = "/home/admin/lq01145628/internal_release/igtransferprod/sand-box/fuzzing_results_new";

        // 查找当前已有的 round_x 目录
        int roundNumber = getNextRoundNumber(filePath);
        String newDirectoryPath = filePath + "/round_" + roundNumber;
        // 创建新目录
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
            // 清空文件内容
            fileWriter.write("");
        } catch (IOException e) {
            System.err.println("An error occurred while clearing the file: " + e.getMessage());
        }
    }

    public static void createAndCopy(String sourcePathStr, String targetPathStr) {
        Path sourcePath = Paths.get(sourcePathStr);
        Path targetPath = Paths.get(targetPathStr);

        // 复制文件
        try {
            Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path targetFilePath = targetPath.resolve(sourcePath.relativize(file));
                    Files.createDirectories(targetFilePath.getParent()); // 创建父目录
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

        // 获取已存在的 round_x 目录
        File[] files = baseDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory() && file.getName().matches("round_\\d+")) {
                    String name = file.getName().substring(6); // 获取数字部分
                    try {
                        int number = Integer.parseInt(name);
                        highestNumber = Math.max(highestNumber, number);
                    } catch (Exception e) {
                        // 如果转换失败，跳过
                    }
                }
            }
        }
        return highestNumber + 1; // 下一个轮次
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

