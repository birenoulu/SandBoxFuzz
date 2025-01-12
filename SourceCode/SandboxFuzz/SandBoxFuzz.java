package com.alibaba.jvm.sandbox.module.example;

import com.alibaba.jvm.sandbox.api.listener.ext.Advice;
import com.alibaba.jvm.sandbox.api.listener.ext.AdviceListener;
import com.alibaba.jvm.sandbox.api.listener.ext.EventWatchBuilder;
import com.alibaba.jvm.sandbox.api.resource.ModuleEventWatcher;
import com.alibaba.jvm.sandbox.api.Information;
import com.alibaba.jvm.sandbox.api.annotation.Command;
import com.alibaba.jvm.sandbox.api.Module;
import com.alibaba.jvm.sandbox.api.ModuleLifecycle;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import redis.clients.jedis.Jedis;
import com.ipay.itest.common.util.mockito.MockYaml;
import com.ipay.itest.common.component.InvokeContext;
import com.ipay.itest.common.support.TestContext;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.*;

@Information(id = "SandBoxFuzz", mode = {Information.Mode.AGENT})
public class SandBoxFuzz implements Module, ModuleLifecycle {

    private static final int MAX_DEPTH = 5;

    @Resource
    private ModuleEventWatcher moduleEventWatcher;

    private Kryo kryo;
    private Jedis jedis;
    private RedisUtil redisUtil;
    private static final String LOG_PREFIX = "[SandBoxFuzz]";
    private static final String SPEC_KEY = "sandbox_specification";
    private static final String MUTATED_KEY = "sandbox_mutated";

    @Override
    public void onLoad() throws Throwable {
        // Initialize Kryo for object serialization
        kryo = new Kryo();
        // Connect to local Redis instance
        jedis = new Jedis("localhost", 6379);
        // Initialize Redis utility class
        redisUtil = new RedisUtil();
        // Set up mock-related watches
        SandBoxFuzz_mock();
        // Set up request-related watches
        SandBoxFuzz_request();
    }

    @Override
    public void onUnload() throws Throwable {
        // Close Redis connection
        jedis.close();
    }

    @Override
    public void onActive() throws Throwable {}

    @Override
    public void onFrozen() throws Throwable {}

    @Override
    public void loadCompleted() {}

    // command annotation to set mock-related listening
    @Command("SandBoxFuzz_mock")
    public void SandBoxFuzz_mock() {
        // Watch for method calls in specified class and behavior
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("*********************.class1") // Class name masked for security
                .onBehavior("method1") // Method name masked for security
                .onWatch(new AdviceListener() {
                    @Override
                    protected void before(Advice advice) throws Throwable {
                        // Get current iteration count from Redis
                        int iteration = redisUtil.getIterationCount();
                        // Attach current time to advice
                        advice.attach(System.currentTimeMillis());
                        Object[] params = advice.getParameterArray();
                        // Set<Object> visited = new HashSet<>();
                        if (params != null && params.length > 0) {
                            if (params[1] instanceof MockYaml) {
                                MockYaml param = (MockYaml) params[1];
                                Object result = param.getResult();
                                if (iteration == 1) {
                                    // modifyAndSaveFields(result, 0, visited);
                                    // first modify
                                    modifyFields(result);
                                    // then save
                                    redisUtil.saveTestCaseObject(result, key);
                                } else if (iteration > 1) {
                                    // Load modified object from Redis and replace
                                    Object newResult = redisUtil.loadTestCaseObject(key);
                                    param.setResult(newResult);
                                }
                            }
                        }
                        logBefore(advice);
                    }

                    @Override
                    public void afterReturning(Advice advice) throws Throwable {
                        logAfter(advice);
                    }
                    
                    // Modify object fields
                    private void modifyFields(Object obj) {
                        // Get all mutable fields of the object
                        List<Field> fields = getAllMutableFields(obj.getClass());
                        int totalFields = fields.size();
                        int meanTime = (int) Math.floor(Math.log(totalFields) / Math.log(2));
                        int mutateTimes = GeometricSample.sampleGeometricTimes(meanTime);
                        int[] resultArray = new int[mutateTimes];
                        Random random = new Random();
                        for (int i = 0; i < mutateTimes; i++) {
                            resultArray[i] = random.nextInt(totalFields);
                        }
                        for (int i : resultArray) {
                            Field field = fields.get(i);
                            field.setAccessible(true);
                            try {
                                Object value = field.get(obj);
                                Object mutatedValue = MutationUtil.mutate(value);
                                field.set(obj, mutatedValue);
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // Get all variable fields
                    private List<Field> getAllMutableFields(Class<?> clazz) {
                        List<Field> fieldList = new ArrayList<>();
                        getAllMutableFieldsRecursive(clazz, fieldList);
                        return fieldList;
                    }

                    // Get all variable fields recursively
                    private void getAllMutableFieldsRecursive(Class<?> clazz, List<Field> fieldList) {
                        if (clazz == null || clazz == Object.class) {
                            return;
                        }
                        Field[] fields = clazz.getDeclaredFields();
                        for (Field field : fields) {
                            if (!Modifier.isFinal(field.getModifiers())) {
                                if(MutationUtil.isAtomicField(field)) {
                                    fieldList.add(field);
                                } else {
                                    getAllMutableFieldsRecursive(field.getType(), fieldList);
                                }
                            }
                        }
                    }

                    // Modify and save the fields to Redis
                    private void modifyAndSaveFields(Object obj, int depth, Set<Object> visited) {
                        if (obj == null || visited.contains(obj) || depth > MAX_DEPTH) return;
                        visited.add(obj);
                        for (Field field : getAllFields(obj.getClass())) {
                            field.setAccessible(true);
                            try {
                                Object value = field.get(obj);
                                if (value != null) {
                                    Object mutatedValue = MutationUtil.mutate(value);
                                    saveMutatedToRedis(mutatedValue);
                                    saveSpecificationToRedis(obj);
                                    modifyAndSaveFields(mutatedValue, depth + 1, visited);
                                }
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // Replace fields from Redis
                    private void replaceFieldsFromRedis(Object obj, int caseIndex, Set<Object> visited) {
                        if (obj == null || visited.contains(obj)) return;
                        visited.add(obj);

                        for (Field field : getAllFields(obj.getClass())) {
                            field.setAccessible(true);
                            try {
                                String redisKey = MUTATED_KEY + "_" + caseIndex + "_" + field.getName();
                                byte[] serializedValue = jedis.get(redisKey.getBytes());
                                if (serializedValue != null) {
                                    Object newValue = kryo.readClassAndObject(new Input(new ByteArrayInputStream(serializedValue)));
                                    field.set(obj, newValue);
                                }
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    // Save the specification to Redis
                    private void saveSpecificationToRedis(Object obj) {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        Output output = new Output(bos);
                        kryo.writeClassAndObject(output, obj);
                        output.close();
                        jedis.set(SPEC_KEY.getBytes(), bos.toByteArray());
                    }

                    // Save the mutated object to Redis
                    private void saveMutatedToRedis(Object obj) {
                        ByteArrayOutputStream bos = new ByteArrayOutputStream();
                        Output output = new Output(bos);
                        kryo.writeClassAndObject(output, obj);
                        output.close();
                        jedis.set(MUTATED_KEY.getBytes(), bos.toByteArray());
                    }

                    // Get all fields
                    private List<Field> getAllFields(Class<?> clazz) {
                        List<Field> fields = new ArrayList<>();
                        while (clazz != null) {
                            fields.addAll(Arrays.asList(clazz.getDeclaredFields()));
                            clazz = clazz.getSuperclass();
                        }
                        return fields;
                    }

                    // Log before method invocation
                    private void logBefore(Advice advice) {
                        String className = advice.getBehavior().getDeclaringClass().getName();
                        String methodName = advice.getBehavior().getName();
                        System.out.println(LOG_PREFIX + " Before " + className + "." + methodName + " with input params: " + Arrays.toString(advice.getParameterArray()));
                    }

                    // Log after method invocation
                    private void logAfter(Advice advice) {
                        long startTime = (long) advice.attachment();
                        long endTime = System.currentTimeMillis();
                        String className = advice.getBehavior().getDeclaringClass().getName();
                        String methodName = advice.getBehavior().getName();
                        Object returnValue = advice.getReturnObj();
                        System.out.println(LOG_PREFIX + " After " + className + "." + methodName + " executed in " + (endTime - startTime) + " ms with return value: " + returnValue);
                    }
                });
    }

    @Command("SandBoxFuzz_request")
    public void SandBoxFuzz_request() {
        // Watch for method calls in specified class and behavior
        new EventWatchBuilder(moduleEventWatcher)
                .onClass("*********************.class2") // Class name masked for security
                .onBehavior("method2") // Method name masked for security
                .onWatch(new AdviceListener() {
                    @Override
                    protected void before(Advice advice) throws Throwable {
                        int iteration = redisUtil.getIterationCount();
                        advice.attach(System.currentTimeMillis());
                        Object[] params = advice.getParameterArray();

                        if (params != null && params.length > 0) {
                            if (params[0] instanceof InvokeContext) {
                                InvokeContext param = (InvokeContext) params[0];
                                TestContext old_TestContext = param.getTestContext();
                                Map<String, Object> old_TestCase = old_TestContext.getAttributes();

                                if (iteration > 1) {
                                    int caseIndex = extractCaseIndexFromParams(params);
                                    TestCase testCase = redisUtil.loadTestCase(iteration - 1, caseIndex);
                                    replaceTestContextAttributes(old_TestContext,testCase);
                                }
                            }
                        }
                    }

                    @Override
                    public void afterReturning(Advice advice) throws Throwable {
                    }
                });
    }

    // Replace the TestContext attribute
    private void replaceTestContextAttributes(TestContext old_TestContext, TestCase testCase) {
        Map<String, Object> newAttributes = testCase.getData(); // or get other part of test case as needed
        old_TestContext.getAttributes().clear();
        old_TestContext.getAttributes().putAll(newAttributes);
    }


    private int extractCaseIndexFromParams(Object[] params) {
        // Extract case index from parameters (implementation not provided)
        return 0;
    }
}
