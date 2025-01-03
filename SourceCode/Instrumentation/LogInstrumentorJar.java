import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import soot.ArrayType;
import soot.Body;
import soot.BodyTransformer;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.VoidType;
import soot.Modifier;
import soot.PackManager;
import soot.PatchingChain;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.SootField;
import soot.Transform;
import soot.Type;
import soot.Unit;
import soot.jimple.GotoStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.IfStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.jimple.LookupSwitchStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.StringConstant;
import soot.jimple.TableSwitchStmt;
import soot.jimple.ThrowStmt;
import soot.options.Options;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.BriefBlockGraph;
import soot.toolkits.graph.CompleteBlockGraph;
import soot.toolkits.graph.ExceptionalBlockGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.graph.ZonedBlockGraph;

import org.yaml.snakeyaml.Yaml;

public class LogInstrumentorJar {
    // size
    private static final int HASHMAP_SIZE_BLOCK = 1 << 16;
    private static final int HASHMAP_SIZE_STMT = 1 << 16;
    private static final int MAX_CLASS_NUM = 1 << 12;
    private static final int MAX_METHOD_NUM = 1 << 10;
    private static final int MAX_BLOCK_NUM = 1 << 10;
    private static final int MAX_STMT_NUM = 1 << 10;
    private static final int METHOD_SHIFT_TO_BLOCK = 10;
    private static final int CLASS_SHIFT_TO_BLOCK = 20;
    private static final int METHOD_SHIFT_TO_STMT = 10;
    private static final int CLASS_SHIFT_TO_STMT = 20;
    // classes
    private static Object classMapLock = new Object();
    private static HashMap<String, Integer> classMap = new HashMap<>();
    private static Object classIDMapLock = new Object();
    private static boolean[] classIDMap = new boolean[MAX_CLASS_NUM];
    private static Object staticMethodIsAddLock = new Object();
    private static HashSet<String> staticMethodIsAdd = new HashSet<>();
    // methods
    private static Object methodIDOfClassMapLock = new Object();
    private static HashMap<String, boolean[]> methodIDOfClassMap = new HashMap<>();
    private static Object classToMethodCountsLock = new Object();
    private static HashMap<String, Integer> classToMethodCounts = new HashMap<>();
    private static Object methodNameCountMapLock = new Object();
    private static HashMap<String, Integer> methodNameCountMap = new HashMap<>();
    private static Object methodCountLock = new Object();
    private static int methodCount = 0;
    // blocks
    private static Object classToMaxBlockNumLock = new Object();
    private static HashMap<String, Integer> classToMaxBlockNum = new HashMap<>();
    private static Object maxBlockNumInClassesLock = new Object();
    private static int maxBlockNumInClasses = 0;
    private static String methodSigWithMaxBlockNum = "";
    private static Object blockCountLock = new Object();
    private static int blockCount = 0;
    // stmts
    private static Object stmtCountsLock = new Object();
    private static HashMap<String, Integer> stmtCounts = new HashMap<>();
    private static Object insertStmtCountsLock = new Object();
    private static int insertStmtCount = 0;
    private static Object maxStmtNumInClassesLock = new Object();
    private static int maxStmtNumInClasses = 0;
    private static String classWithMaxStmtNum = "";
    private static String methodSigWithMaxStmtNum = "";
    // hashmap of class, method, block
    private static Object jointMapLock = new Object();
    private static int[] jointMapClass = new int[MAX_CLASS_NUM];
    private static int[] jointMapMethod = new int[MAX_METHOD_NUM];
    private static int[] jointMapBlock = new int[MAX_BLOCK_NUM];
    private static int[] jointMapStmt = new int[MAX_STMT_NUM];
    // hashmap
    private static Object blockHashMapWithListCollisionLock = new Object();
    @SuppressWarnings("unchecked")
    private static final ArrayList<String>[] blockHashMapWithListCollision = new ArrayList[HASHMAP_SIZE_BLOCK];
    private static final int[][] blockHashCount = new int[HASHMAP_SIZE_BLOCK][2];
    private static Object stmtHashMapWithListCollisionLock = new Object();
    @SuppressWarnings("unchecked")
    private static final ArrayList<String>[] stmtHashMapWithListCollision = new ArrayList[HASHMAP_SIZE_STMT];
    private static final int[][] stmtHashCount = new int[HASHMAP_SIZE_STMT][2];
    static {
        for (int idx = 0; idx < HASHMAP_SIZE_BLOCK; idx++) {
            blockHashMapWithListCollision[idx] = new ArrayList<String>();
            blockHashCount[idx][0] = idx;
        }
        for (int idx = 0; idx < HASHMAP_SIZE_STMT; idx++) {
            stmtHashMapWithListCollision[idx] = new ArrayList<String>();
            stmtHashCount[idx][0] = idx;
        }
    }

    // static hash function
    private static int hash(long x, int bound) {
        return knuth(x, bound);
    }
    private static int hash1(long x, long y, int bound) {
        return knuth(x*31 + y, bound);
    }
    private static int knuth(long x, int bound) {
        return cap(x*2654435761L, bound);
    }
    private static int cap(long x, int bound) {
        int res = (int) (x % bound);
        if (res < 0) {
            res += bound;
        }
        return res;
    }

    // traverse recursively to construct external libraries
    private static void traverseDirectory(File directory, StringBuilder sb) {
        File[] files = directory.listFiles();

        for (File file: files) {
            if (file.isFile() && file.getName().endsWith(".jar")) {
                sb.append(file.getAbsolutePath());
                sb.append(":");
            } else if (file.isDirectory()) {
                traverseDirectory(file, sb);
            }
        }
    }

    private static void executeCommand(String... command) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        try {
            Process process = processBuilder.start();
            // InputStream is = process.getInputStream();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Command execution failed with exit code " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return;
        }
    }

    private static Boolean transformInfo = false;
    private static HashSet<String> transformFilter = new HashSet<>();

    public static void main(String[] args) {
        // init log metadata
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        String timeStamp = dateFormat.format(new Date());
        PrintStream fileOut = System.out;

        // args to be parsed from config.yaml
        String homePath;
        String externalLibrariesDir;
        String tmpSootOutputDir;
        String logMode;
        String logLevel;
        String outputFormat;
        HashMap<String, Boolean> sootLog = new HashMap<>();
        HashMap<String, Long> seeds = new HashMap<>();
        HashMap<String, String> targetPaths = new HashMap<>();
        HashMap<String, String> targetJars = new HashMap<>();
        StringBuilder jresb = new StringBuilder();
        String jrePaths;
        StringBuilder ompsb = new StringBuilder();
        String otherModulePaths;
        HashSet<String> methodsToIgnore = new HashSet<>();
        HashSet<String> modulesToIgnore = new HashSet<>();

        // parse the config.yaml file
        String configPath = "config.yaml";
        Yaml yaml = new Yaml();
        try (Reader r = new FileReader(configPath)) {
            Map<String, Object> configYaml = yaml.load(r);

            // parse the paths
            homePath = (String) configYaml.get("homePath");
            externalLibrariesDir = (String) configYaml.get("externalLibrariesDir");
            tmpSootOutputDir = (String) configYaml.get("tmpSootOutputDir");
            logLevel = (String) configYaml.get("logLevel");
            logMode = (String) configYaml.get("logMode");
            outputFormat = (String) configYaml.get("outputFormat");

            if (homePath == null || externalLibrariesDir == null || tmpSootOutputDir == null || logLevel == null || logMode == null || outputFormat == null) {
                System.err.println("config.yaml format error!");
                return;
            }

            // parse the seeds
            @SuppressWarnings("unchecked")
            Map<String, Object> seedsYaml = (Map<String, Object>) configYaml.get("seeds");
            if (seedsYaml != null) {
                for (Map.Entry<String, Object> entry : seedsYaml.entrySet()) {
                    seeds.put(entry.getKey(), (Long) entry.getValue());
                }
            }

            // parse the targetPaths and targetJars
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> targetPathsYaml = (List<Map<String, Object>>) configYaml.get("targetPaths");
            if (targetPathsYaml != null) {
                for (Map<String, Object> targetPathYaml : targetPathsYaml) {
                    targetPaths.put((String) targetPathYaml.get("name"), (String) targetPathYaml.get("dir"));
                    targetJars.put((String) targetPathYaml.get("name"), (String) targetPathYaml.get("tar"));
                }
            } else {
                System.err.println("config.yaml format error!");
                return;
            }

            // parse ignoreModules
            @SuppressWarnings("unchecked")
            List<String> ignoreModulesYaml = (List<String>) configYaml.get("ignoreModules");
            if (ignoreModulesYaml != null) {
                for (String ignoreModuleYaml : ignoreModulesYaml) {
                    modulesToIgnore.add(ignoreModuleYaml);
                }
            }

            // parse jrePaths
            @SuppressWarnings("unchecked")
            List<String> jrePathsYaml = (List<String>) configYaml.get("jrePaths");
            if (jrePathsYaml != null) {
                for (String jrePathYaml : jrePathsYaml) {
                    jresb.append(jrePathYaml);
                    jresb.append(":");
                }
                jrePaths = jresb.toString();
            } else {
                System.err.println("config.yaml format error!");
                return;
            }

            // parse otherModulePaths
            @SuppressWarnings("unchecked")
            List<String> otherModulePathsYaml = (List<String>) configYaml.get("otherModulePaths");
            if (otherModulePathsYaml != null) {
                for (String otherModulePathYaml : otherModulePathsYaml) {
                    ompsb.append(homePath);
                    ompsb.append(otherModulePathYaml);
                    ompsb.append(":");
                }
                otherModulePaths = ompsb.toString();
            } else {
                System.err.println("config.yaml format error!");
                return;
            }

            // parse method list
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> methodListYaml = (List<Map<String, Object>>) configYaml.get("methodList");
            if (methodListYaml != null) {
                for (Map<String, Object> methodYaml : methodListYaml) {
                    if (Boolean.TRUE.equals((Boolean) methodYaml.get("ignore"))) {
                        methodsToIgnore.add((String) methodYaml.get("name"));
                    }
                }
            }

            // parse soot log settings
            @SuppressWarnings("unchecked")
            Map<String, Object> sootLogYaml = (Map<String, Object>) configYaml.get("sootLog");
            if (sootLogYaml != null) {
                for (Map.Entry<String, Object> entry : sootLogYaml.entrySet()) {
                    sootLog.put(entry.getKey(), (Boolean) entry.getValue());
                }
            } else {
                System.err.println("config.yaml format error!");
                return;
            }

            // parse transformFilter
            @SuppressWarnings("unchecked")
            List<String> transfromFilterYaml = (List<String>) configYaml.get("transformFilter");
            if (transfromFilterYaml != null) {
                for (String methodname : transfromFilterYaml) {
                    transformFilter.add(methodname);
                }
            }

        } catch (IOException | NumberFormatException e) {
            e.printStackTrace();
            return;
        }

        // init soot log settings
        Boolean logFile = false;
        Boolean summaryInfo = false;
        Boolean hashMapInfo = false;
        if (sootLog.containsKey("logFile") && sootLog.get("logFile")) {
            logFile = true;
        }
        if (sootLog.containsKey("transformInfo") && sootLog.get("transformInfo")) {
            transformInfo = true;
        }
        if (sootLog.containsKey("summaryInfo") && sootLog.get("summaryInfo")) {
            summaryInfo = true;
        }
        if (sootLog.containsKey("hashMapInfo") && sootLog.get("hashMapInfo")) {
            hashMapInfo = true;
        }

        // init log file
        if (logFile) {
            try {
                fileOut = new PrintStream(new FileOutputStream("instJar-" + timeStamp + "-output.log"));
                System.setOut(fileOut);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return;
            }
        }

        // init the seed
        Random random;
        long methodSeed;
        long blockSeed;
        long stmtSeed;

        if (seeds.containsKey("initSeed")) {
            System.out.println("init seed: " + seeds.get("initSeed"));
            random = new Random(seeds.get("initSeed"));
        } else {
            random = new Random();
        }

        if (seeds.containsKey("methodSeed")) {
            methodSeed = seeds.get("methodSeed");
            random.nextLong();
        } else {
            methodSeed = random.nextLong();
        }

        if (seeds.containsKey("blockSeed")) {
            blockSeed = seeds.get("blockSeed");
            random.nextLong();
        } else {
            blockSeed = random.nextLong();
        }

        if (seeds.containsKey("stmtSeed")) {
            stmtSeed = seeds.get("stmtSeed");
            random.nextLong();
        } else {
            stmtSeed = random.nextLong();
        }

        Random methodRandom = new Random(methodSeed);
        Random blockRandom = new Random(blockSeed);
        Random stmtRandom = new Random(stmtSeed);
        System.out.println("method seed: " + methodSeed);
        System.out.println("block seed: " + blockSeed);
        System.out.println("stmt seed: " + stmtSeed);

        // construct classPathAll
        StringBuilder cpasb = new StringBuilder();
        for (Map.Entry<String, String> entry : targetPaths.entrySet()) {
            cpasb.append(homePath);
            cpasb.append(entry.getValue());
            cpasb.append("classes/");
            cpasb.append(":");
        }
        String classPathAll = cpasb.toString();

         // construct externalLibrariesPath
         StringBuilder elsb = new StringBuilder();
         traverseDirectory(new File(externalLibrariesDir), elsb);
         String externalLibrariesPaths = elsb.toString();

        // parse the command arguments
        if (args.length != 1 && args.length != 2) {
            System.err.println("Usage: java -cp <soot classpath> LogInstrumentorJar <target module name to transfrom / all> <output dir>");
            System.exit(0);
        }

        String inputModule = args[0];
        if (!inputModule.equals("all") && !targetPaths.containsKey(inputModule)) {
            System.err.println("input module doesn't exist!");
            return;
        }

        // printed log message mode
        // level:
        // - block: per basic block
        //   mode:
        //    - hash: (class, method) + hashcode
        //    - sign: (class, method) + block_id + stmt_id
        //    - fuzz: (class, method) + hashcode + block_id + stmt_id, if use fuzzing, please select this format
        // - stmt: per stmt (more fine-grained than block)
        //   mode:
        //   - hash: (class, method) + hashcode
        //   - sign: (class, method) + stmt_id
        // - method: per method
        //   mode:
        //   - sign: (class, method)
        // - none: don't do any instrument
        if (!logLevel.equals("block") && !logLevel.equals("stmt") && !logLevel.equals("method") && !logLevel.equals("none")) {
            System.err.println("log level doesn't exist!");
            return;
        }
        if (logLevel.equals("block") && (!logMode.equals("hash") && !logMode.equals("sign") && !logMode.equals("fuzz"))) {
            System.err.println("log mode doesn't exist in block level!");
            return;
        }
        if (logLevel.equals("stmt") && (!logMode.equals("hash") && !logMode.equals("sign"))) {
            System.err.println("log mode doesn't exist in stmt level!");
            return;
        }
        if (logLevel.equals("method") && !logMode.equals("sign")) {
            System.err.println("log mode doesn't exist in method level!");
            return;
        }

        // add args to soot
        LinkedList<String> sootArgs = new LinkedList<>();
        sootArgs.add("-cp");
        sootArgs.add(classPathAll + ":" + jrePaths + ":" + externalLibrariesPaths + ":" + otherModulePaths);
        sootArgs.add("-d");
        // output to target dir or specified dir
        if (args.length == 1) {
            executeCommand("rm", "-rf", tmpSootOutputDir);
            sootArgs.add(tmpSootOutputDir);
        } else {
            sootArgs.add(args[1]);
        }
        sootArgs.add("-output-format");
        sootArgs.add(outputFormat);
        // sootArgs.add("class");
        sootArgs.add("-hierarchy-dirs");
        sootArgs.add("-java-version");
        sootArgs.add("1.8");
        //sootArgs.add("-allow-phantom-refs");
        sootArgs.add("-keep-line-number");
        // construct input target module or all modules
        if (inputModule.equals("all")) {
            for (String key : targetPaths.keySet()) {
                if (!modulesToIgnore.contains(key)) {
                    sootArgs.add("-process-dir");
                    sootArgs.add(homePath + targetPaths.get(key) + targetJars.get(key));
                }
            }
        } else {
            sootArgs.add("-process-dir");
            sootArgs.add(homePath + targetPaths.get(inputModule) + targetJars.get(inputModule));
        }

        Transform instrumentation = new Transform("jtp.LogTrans", new BodyTransformer() {
            @Override
            protected void internalTransform (Body body, String phase, Map options) {
                SootMethod method = body.getMethod();
                SootClass thisClass = method.getDeclaringClass();
                String className = thisClass.getName();
                String methodName = method.getName();
                String classNameNoPackage = thisClass.getShortName();
                String packageName = thisClass.getPackageName();
                String methodSig = method.getSignature();
                String methodSubSig = method.getSubSignature();

                if (methodsToIgnore.contains(methodName)) {
                    return;
                }

                // ****************** processing class ******************
                int classID;
                synchronized (classMapLock) {
                    if (!classMap.containsKey(className)) {
                        int classIdx = Math.abs(className.hashCode()) % MAX_CLASS_NUM;
                        synchronized (classIDMapLock) {
                            if (!classIDMap[classIdx]) {
                                classIDMap[classIdx] = true;
                                classID = classIdx;
                                classMap.put(className, classID);
                            } else {
                                // find next index
                                for (int i=1; i < MAX_CLASS_NUM; i++) {
                                    classIdx = (classIdx + i) % MAX_CLASS_NUM;
                                    if (!classIDMap[classIdx]) {
                                        classIDMap[classIdx] = true;
                                        break;
                                    }
                                }
                                classID = classIdx;
                                classMap.put(className, classID);
                            }
                        }
                    } else {
                        classID = classMap.get(className);
                    }
                }

                // ****************** processing method ******************
                int methodID;
                int methodIdx = methodRandom.nextInt(MAX_METHOD_NUM);
                synchronized (methodIDOfClassMapLock) {
                    boolean[] methodIDOfClass = methodIDOfClassMap.get(className);
                    if (methodIDOfClass == null) {
                        boolean[] methodIDOfClassToInsert = new boolean[MAX_METHOD_NUM];
                        methodIDOfClassToInsert[methodIdx] = true;
                        methodIDOfClassMap.put(className, methodIDOfClassToInsert);
                        methodID = methodIdx;
                    } else {
                        if (!methodIDOfClass[methodIdx]) {
                            methodIDOfClass[methodIdx] = true;
                            methodID = methodIdx;
                        } else {
                            // find next index
                            for (int i=1; i < MAX_METHOD_NUM; i++) {
                                methodIdx = (methodIdx + i) % MAX_METHOD_NUM;
                                if (!methodIDOfClass[methodIdx]) {
                                    methodIDOfClass[methodIdx] = true;
                                    break;
                                }
                            }
                            methodID = methodIdx;
                        }
                    }
                }

                // add a static method to this class at the first time this class is loaded
                synchronized (staticMethodIsAddLock) {
                    if (!staticMethodIsAdd.contains(className)) {
                        // create new method
                        List<Type> parameterTypes = Arrays.asList(RefType.v("java.lang.String"));
                        SootMethod newMethod = new SootMethod("println", parameterTypes, VoidType.v(), Modifier.PRIVATE | Modifier.STATIC);

                        // add method to this class
                        thisClass.addMethod(newMethod);

                        // create method body
                        JimpleBody newbody = Jimple.v().newBody(newMethod);
                        newMethod.setActiveBody(newbody);

                        // create and add param local
                        Local paramStr = Jimple.v().newLocal("str", RefType.v("java.lang.String"));
                        newbody.getLocals().add(paramStr);

                        // get ref of System.out
                        StaticFieldRef outRef = Jimple.v().newStaticFieldRef(Scene.v().getField("<java.lang.System: java.io.PrintStream out>").makeRef());

                        // get ref of println
                        SootMethod printlnMethod = Scene.v().getMethod("<java.io.PrintStream: void println(java.lang.String)>");

                        // create and add local variable out = System.out
                        Local outLocal = Jimple.v().newLocal("out", RefType.v("java.io.PrintStream"));
                        newbody.getLocals().add(outLocal);

                        // create identity and assign stmts
                        newbody.getUnits().add(Jimple.v().newIdentityStmt(paramStr, Jimple.v().newParameterRef(RefType.v("java.lang.String"), 0)));
                        newbody.getUnits().add(Jimple.v().newAssignStmt(outLocal, outRef));
                        Unit callPrintln = Jimple.v().newInvokeStmt(Jimple.v().newVirtualInvokeExpr(outLocal, printlnMethod.makeRef(), paramStr));

                        // add invoke stmt and return stmt
                        newbody.getUnits().add(callPrintln);
                        newbody.getUnits().add(Jimple.v().newReturnVoidStmt());

                        staticMethodIsAdd.add(className);
                    }
                }

                // ****************** preparing field and method references ******************
                SootMethod printlnMethod = thisClass.getMethodByName("println");

                // ****************** processing blocks ******************
                // graph construction and block processing
                // BlockGraph blockGraph = new BriefBlockGraph(body);
                BlockGraph blockGraph = new ZonedBlockGraph(body);
                Iterator<Block> blocksIx = blockGraph.getBlocks().iterator();
                int blocknum = blockGraph.size();
                HashSet<Integer> blockIDOfMethodMap = new HashSet<>();
                boolean isFirstBlock = true;

                // if (transformInfo && (transformFilter.isEmpty() || transformFilter.contains(methodName))) {
                //     System.out.println("package:" + packageName + ", classname:" + classNameNoPackage + ", classID:" + classID + ", methodName(" + blocknum + "):" + methodName + ", methodIdx -> methodID:" + methodIdx + "->" + methodID);
                // }

                while (blocksIx.hasNext()) {
                    Block block = blocksIx.next();
                    int blockIdxInMethod = block.getIndexInMethod();

                    // ****************** processing block ******************
                    int blockID;
                    int blockIdx = blockRandom.nextInt(MAX_BLOCK_NUM);
                    if (!blockIDOfMethodMap.contains(blockIdx)) {
                        blockIDOfMethodMap.add(blockIdx);
                        blockID = blockIdx;
                    } else {
                        for (int i=1; i < MAX_BLOCK_NUM; i++) {
                            blockIdx = (blockIdx + i) % MAX_BLOCK_NUM;
                            if (!blockIDOfMethodMap.contains(blockIdx)) {
                                blockIDOfMethodMap.add(blockIdx);
                                break;
                            }
                        }
                        blockID = blockIdx;
                    }

                    // do bit operation to get the masked ID
                    int IDEncoded = (classID << CLASS_SHIFT_TO_BLOCK) | (methodID << METHOD_SHIFT_TO_BLOCK) | blockID;
                    int hashcode = hash(IDEncoded, (HASHMAP_SIZE_BLOCK));

                    // ****************** processing log instrument ******************
                    if (logLevel.equals("block") || logLevel.equals("method")) {
                        String logPrinted = "LPS: ";
                        if (logMode.equals("hash")) {
                            logPrinted = logPrinted + methodSig + ", hashCode: " + hashcode;
                        } else if (logMode.equals("sign")) {
                            logPrinted = logPrinted + methodSig + ", block: " + blockIdxInMethod;
                        } else if (logMode.equals("fuzz")) {
                            logPrinted = logPrinted + methodSig + ", hashCode: " + hashcode + ", block: " + blockIdxInMethod;
                        }

                        if (isFirstBlock || logLevel.equals("block")) {
                            isFirstBlock = false;
                            // insert before or after the last unit of the block
                            boolean insertAfterTail = false;
                            Unit tail = block.getTail();
                            if (tail instanceof InvokeStmt ||
                                    tail instanceof IfStmt ||
                                    tail instanceof ThrowStmt ||
                                    tail instanceof ReturnVoidStmt ||
                                    tail instanceof ReturnStmt ||
                                    tail instanceof GotoStmt ||
                                    tail instanceof TableSwitchStmt ||
                                    tail instanceof LookupSwitchStmt) {
                                insertAfterTail = false;
                            }

                            if (logMode.equals("sign") || logMode.equals("fuzz")) {
                                logPrinted = logPrinted + ", stmt: " + tail.getJavaSourceStartLineNumber();
                            }
                            Unit callPrintln = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(printlnMethod.makeRef(), StringConstant.v(logPrinted)));

                            if (insertAfterTail) {
                                block.insertAfter(callPrintln, tail);
                            } else {
                                block.insertBefore(callPrintln, tail);
                            }
                        }
                    }

                    // ****************** update profiles ******************
                    synchronized (blockHashMapWithListCollisionLock) {
                        blockHashMapWithListCollision[hashcode].add("[" + className + ":" + methodName + ":" + blockIdxInMethod + "]");
                    }

                    synchronized (jointMapLock) {
                        jointMapBlock[blockID] += 1;
                    }

                    synchronized (blockCountLock) {
                        blockCount++;
                    }
                }

                // ****************** processing units ******************
                PatchingChain<Unit> units = body.getUnits();
                Iterator<Unit> unitsIx = units.snapshotIterator();
                HashSet<Integer> stmtIDOfMethodMap = new HashSet<>();
                int insertStmtCountOfMethod = 0;

                while (unitsIx.hasNext()) {
                    Unit unit = unitsIx.next();
                    int lineNum = unit.getJavaSourceStartLineNumber();

                    if ((!(unit instanceof IdentityStmt)) &&
                            (unit instanceof InvokeStmt ||
                                    unit instanceof IfStmt ||
                                    unit instanceof ThrowStmt ||
                                    unit instanceof ReturnVoidStmt ||
                                    unit instanceof ReturnStmt ||
                                    unit instanceof GotoStmt ||
                                    unit instanceof TableSwitchStmt ||
                                    unit instanceof LookupSwitchStmt)) {
                        // ****************** processing unit ******************
                        int stmtID;
                        int stmtIdx = stmtRandom.nextInt(MAX_STMT_NUM);
                        insertStmtCountOfMethod++;
                        if (!stmtIDOfMethodMap.contains(stmtIdx)) {
                            stmtIDOfMethodMap.add(stmtIdx);
                            stmtID = stmtIdx;
                        } else {
                            for (int i=1; i < MAX_STMT_NUM; i++) {
                                stmtIdx = (stmtIdx + i) % MAX_STMT_NUM;
                                if (!stmtIDOfMethodMap.contains(stmtIdx)) {
                                    stmtIDOfMethodMap.add(stmtIdx);
                                    break;
                                }
                            }
                            stmtID = stmtIdx;
                        }

                        // do bit operation to get the masked ID
                        int IDEncoded = (classID << CLASS_SHIFT_TO_STMT) | (methodID << METHOD_SHIFT_TO_STMT) | stmtID;
                        int hashcode = hash(IDEncoded, (HASHMAP_SIZE_STMT));

                        // ****************** processing log instrument ******************
                        if (logLevel.equals("stmt")) {
                            String logPrinted = "LPS: ";
                            if (logMode.equals("hash")) {
                                logPrinted = logPrinted + methodSig + ", hashCode:" + hashcode;
                            } else if (logMode.equals("sign")) {
                                logPrinted = logPrinted + methodSig + ", stmt: " + lineNum;
                            }

                            Unit callPrintln = Jimple.v().newInvokeStmt(Jimple.v().newStaticInvokeExpr(printlnMethod.makeRef(), StringConstant.v(logPrinted)));
                            units.insertBefore(callPrintln, unit);
                        }

                        // ****************** update profiles ******************
                        synchronized (stmtHashMapWithListCollisionLock) {
                            stmtHashMapWithListCollision[hashcode].add("[" + classNameNoPackage + ":" + methodName + ":" + lineNum + "]");
                        }

                        synchronized (jointMapLock) {
                            jointMapStmt[stmtID] += 1;
                        }

                        synchronized (insertStmtCountsLock) {
                            insertStmtCount++;
                        }
                    }

                    String stmtType = unit.getClass().getName();
                    synchronized (stmtCountsLock) {
                        Integer stmtCount = stmtCounts.get(stmtType);
                        if (stmtCount == null) {
                            stmtCounts.put(stmtType, 1);
                        } else {
                            stmtCounts.put(stmtType, stmtCount + 1);
                        }
                    }
                }

                if (transformInfo && (transformFilter.isEmpty() || transformFilter.contains(methodName))) {
                    System.out.println("package:" + packageName + ", classname:" + classNameNoPackage + ", classID:" + classID + ", methodName(" + blocknum + "|" + insertStmtCountOfMethod + "):" + methodName + ", methodIdx -> methodID:" + methodIdx + "->" + methodID);
                }

                // ****************** update profiles ******************
                synchronized (classToMethodCountsLock) {
                    Integer methodCounter = classToMethodCounts.get(className);
                    if (methodCounter == null) {
                        classToMethodCounts.put(className, 1);
                    } else {
                        classToMethodCounts.put(className, methodCounter + 1);
                    }
                }

                synchronized (methodNameCountMapLock) {
                    Integer methodNameCount = methodNameCountMap.get(method.getName());
                    if (methodNameCount == null) {
                        methodNameCountMap.put(method.getName(), 1);
                    } else {
                        methodNameCountMap.put(method.getName(), methodNameCount + 1);
                    }
                }

                synchronized (classToMaxBlockNumLock) {
                    Integer maxBlockNum = classToMaxBlockNum.get(className);
                    if (maxBlockNum == null) {
                        classToMaxBlockNum.put(className, blocknum);
                    } else {
                        if (blocknum > maxBlockNum) {
                            classToMaxBlockNum.put(className, blocknum);
                        }
                    }
                }

                synchronized (maxBlockNumInClassesLock) {
                    if (blocknum > maxBlockNumInClasses) {
                        maxBlockNumInClasses = blocknum;
                        methodSigWithMaxBlockNum = method.getSubSignature();
                    }
                }

                synchronized (maxStmtNumInClassesLock) {
                    if (insertStmtCountOfMethod > maxStmtNumInClasses) {
                        maxStmtNumInClasses = insertStmtCountOfMethod;
                        classWithMaxStmtNum = className;
                        methodSigWithMaxStmtNum = method.getSubSignature();
                    }
                }

                synchronized (methodCountLock) {
                    methodCount++;
                }

                synchronized (jointMapLock) {
                    jointMapClass[classID] += 1;
                    jointMapMethod[methodID] += 1;
                }
            }
        });

        String[] sootArgsArray = new String[sootArgs.size()];
        int i = 0;
        for (Iterator<String> sootArgIx = sootArgs.iterator(); sootArgIx.hasNext(); ) {
            String sootArg = sootArgIx.next();
            sootArgsArray[i] = sootArg;
            ++i;
        }

        // resolve classes
        Scene.v().addBasicClass("java.lang.System", SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.io.PrintStream", SootClass.SIGNATURES);
        PackManager.v().getPack("jtp").add(instrumentation);
        // phase options
        // Options.v().setPhaseOption("jb", "use-original-names:true");
        // Options.v().setPhaseOption("jb", "preserve-source-annotations:true");
        Options.v().setPhaseOption("jb", "stabilize-local-names:true");
        Options.v().setPhaseOption("jb", "model-lambdametafactory:false");
        // Options.v().setPhaseOption("jb.sils", "enabled:false");
        // Options.v().setPhaseOption("jb.ls", "enabled:false");
        soot.Main.main(sootArgsArray);

        // calculate profiles for instance
        int block_occupied = 0;
        int block_collision = 0;
        for (int idx=0; idx < blockHashMapWithListCollision.length; idx++) {
            if (blockHashMapWithListCollision[idx].size() > 0) {
                block_occupied += 1;
                if (blockHashMapWithListCollision[idx].size() > 1) {
                    block_collision += 1;
                }
            }
            blockHashCount[idx][1] = blockHashMapWithListCollision[idx].size();
        }

        int stmt_occupied = 0;
        int stmt_collision = 0;
        for (int idx=0; idx < stmtHashMapWithListCollision.length; idx++) {
            if (stmtHashMapWithListCollision[idx].size() > 0) {
                stmt_occupied += 1;
                if (stmtHashMapWithListCollision[idx].size() > 1) {
                    stmt_collision += 1;
                }
            }
            stmtHashCount[idx][1] = stmtHashMapWithListCollision[idx].size();
        }

        int maxblocknum = 0;
        String maxblocknumclass = null;
        for (Map.Entry<String, Integer> entry : classToMaxBlockNum.entrySet()) {
            if (entry.getValue() > maxblocknum) {
                maxblocknum = entry.getValue();
                maxblocknumclass = entry.getKey();
            }
        }
        maxblocknum++;

        int maxmethodnum = 0;
        String maxmethodnumclass = null;
        for (Map.Entry<String, Integer> entry : classToMethodCounts.entrySet()) {
            if (entry.getValue() > maxmethodnum) {
                maxmethodnum = entry.getValue();
                maxmethodnumclass = entry.getKey();
            }
        }
        maxmethodnum++;

        int classIDocc = 0;
        for (int idx=0; idx < jointMapClass.length; idx++) {
            if (jointMapClass[idx] > 0) {
                classIDocc += 1;
            }
        }

        int methodIDocc = 0;
        for (int idx=0; idx < jointMapMethod.length; idx++) {
            if (jointMapMethod[idx] > 0) {
                methodIDocc += 1;
            }
        }

        int blockIDocc = 0;
        for (int idx=0; idx < jointMapBlock.length; idx++) {
            if (jointMapBlock[idx] > 0) {
                blockIDocc += 1;
            }
        }

        int stmtIDocc = 0;
        for (int idx=0; idx < jointMapStmt.length; idx++) {
            if (jointMapStmt[idx] > 0) {
                stmtIDocc += 1;
            }
        }

        System.out.println("----------------------------------------------------------------------------------------------------------------------------");
        System.out.println("analysing done!");

        if (summaryInfo) {
            System.out.println("block hash map occupied: " + ((float)block_occupied / (float)HASHMAP_SIZE_BLOCK) * 100 + "%," + " collision: " + ((float)block_collision / (float)HASHMAP_SIZE_BLOCK) * 100 + "%");
            System.out.println("stmt hash map occupied: " + ((float)stmt_occupied / (float)HASHMAP_SIZE_STMT) * 100 + "%," + " collision: " + ((float)stmt_collision / (float)HASHMAP_SIZE_STMT) * 100 + "%");
            System.out.println("maxblocknum: " + maxblocknum + " from class: " + maxblocknumclass + " from method: " + methodSigWithMaxBlockNum);
            System.out.println("maxstmtnum: " + maxStmtNumInClasses + " from class: " + classWithMaxStmtNum + "from method: " + methodSigWithMaxStmtNum);
            System.out.println("maxmethodnum: " + maxmethodnum + " from class: " + maxmethodnumclass);
            System.out.println("totalClasses: " + classMap.size() + ", totalMethods: " + methodCount + ", totalBlocks: " + blockCount + ", totalStmts: " + insertStmtCount);
            System.out.println("classID occuiped: " + ((float)classIDocc / (float)MAX_CLASS_NUM) * 100 + "%");
            System.out.println("methodID occuiped: " + ((float)methodIDocc / (float)MAX_METHOD_NUM) * 100 + "%");
            System.out.println("blockID occuiped: " + ((float)blockIDocc / (float)MAX_BLOCK_NUM) * 100 + "%");
            System.out.println("stmtID occuiped: " + ((float)stmtIDocc / (float)MAX_STMT_NUM) * 100 + "%");

            System.out.println("----------------------------------------------------------------------------------------------------------------------------");
            System.out.println("methodname counts, top 50:");
            List<Map.Entry<String, Integer>> entryList = new ArrayList<>(methodNameCountMap.entrySet());
            Collections.sort(entryList, new Comparator<Map.Entry<String, Integer>>() {
                @Override
                public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                    return o2.getValue().compareTo(o1.getValue());
                }
            });

            int methodOutCount = 0;
            for (Map.Entry<String, Integer> entry : entryList) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
                methodOutCount++;
                if (methodOutCount >= 50) {
                    break;
                }
            }

            System.out.println("----------------------------------------------------------------------------------------------------------------------------");
            System.out.println("stmt counts:");
            for (Map.Entry<String, Integer> entry : stmtCounts.entrySet()) {
                System.out.println(entry.getKey() + ": " + entry.getValue());
            }
        }

        if (hashMapInfo) {
            System.out.println("----------------------------------------------------------------------------------------------------------------------------");
            System.out.println("block hashmap:");

            for (int hidx=0; hidx < HASHMAP_SIZE_BLOCK; hidx++) {
                if (blockHashCount[hidx][1] > 0) {
                    // System.out.println(hidx + " -> " + blockhashCount[hidx][1]);
                    System.out.print(blockHashCount[hidx][0] + " -> " + blockHashCount[hidx][1] + " :{ ");
                    for (String tuple : blockHashMapWithListCollision[blockHashCount[hidx][0]]) {
                        System.out.print(tuple + ", ");
                    }
                    System.out.println("}");
                } else {
                    System.out.print(blockHashCount[hidx][0] + " -> " + blockHashCount[hidx][1] + " :{ }");
                }
            }

            System.out.println("----------------------------------------------------------------------------------------------------------------------------");
            System.out.println("block hashmap (non-zero, counts sorted, with tuples):");

            Arrays.sort(blockHashCount, new Comparator<int[]>() {
                @Override
                public int compare(int[] a, int[] b) {
                    return Integer.compare(b[1], a[1]);
                }
            });

            for (int hidx=0; hidx < HASHMAP_SIZE_BLOCK; hidx++) {
                if (blockHashCount[hidx][1] > 0) {
                    // System.out.println(hidx + " -> " + blockhashCount[hidx][1]);
                    System.out.print(blockHashCount[hidx][0] + " -> " + blockHashCount[hidx][1] + " :{ ");
                    for (String tuple : blockHashMapWithListCollision[blockHashCount[hidx][0]]) {
                        System.out.print(tuple + ", ");
                    }
                    System.out.println("}");
                }
            }

            //System.out.println("----------------------------------------------------------------------------------------------------------------------------");
            //System.out.println("stmt hashmap (non-zero, sorted, with tuples):");
            //
            //Arrays.sort(stmtHashCount, new Comparator<int[]>() {
            //    @Override
            //    public int compare(int[] a, int[] b) {
            //        return Integer.compare(b[1], a[1]);
            //    }
            //});
            //
            //for (int hidx=0; hidx < HASHMAP_SIZE_STMT; hidx++) {
            //    if (stmtHashCount[hidx][1] > 0) {
            //        // System.out.println(hidx + " -> " + stmthashCount[hidx][1]);
            //        System.out.print(stmtHashCount[hidx][0] + " -> " + stmtHashCount[hidx][1] + " :{ ");
            //        for (String tuple : stmtHashMapWithListCollision[stmtHashCount[hidx][0]]) {
            //            System.out.print(tuple + ", ");
            //        }
            //        System.out.println("}");
            //    }
            //}
        }

        if (logFile) {
            fileOut.close();
        }

        if (args.length == 1) {
            if (inputModule.equals("all")) {
                for (String key : targetPaths.keySet()) {
                    if (!modulesToIgnore.contains(key)) {
                        String targetClassesPath = homePath + targetPaths.get(key) + "classes/com/";
                        executeCommand("rm", "-rf", targetClassesPath);
                        executeCommand("cp", "-a", tmpSootOutputDir + "com/", targetClassesPath);
                    }
                }
            } else {
                String targetClassesPath = homePath + targetPaths.get(inputModule) + "classes/com/";
                executeCommand("rm", "-rf", targetClassesPath);
                executeCommand("cp", "-a", tmpSootOutputDir + "com/", targetClassesPath);
            }
        }
    }
}
