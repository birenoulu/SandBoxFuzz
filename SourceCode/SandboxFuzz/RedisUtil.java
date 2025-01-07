import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.*;

import org.objenesis.strategy.StdInstantiatorStrategy;

public class RedisUtil {

    private static final String REDIS_HOST             = "localhost";
    private static final int    REDIS_PORT             = 6379;
    private static final String SERIALIZE_TIME         = "serialize_time";
    private static final String DESERIALIZE_TIME       = "deserialize_time";
    private static final String DESERIALIZE_CASE_COUNT = "deserialize_case_count";
    private static final String SERIALIZE_CASE_COUNT   = "serialize_case_count";
    public static final Kryo   kryo                   = new Kryo();

    static {
        kryo.setInstantiatorStrategy(new Kryo.DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));
        RedisUtil.classLoader();
    }

    private Jedis jedis;

    public RedisUtil() {
        jedis = new Jedis(REDIS_HOST, REDIS_PORT);
    }

    public Object incrementKey(String key) {
        return jedis.incr(key);
    }

    public void incrementKey(String key, long value) {
        jedis.incrBy(key, value);
    }

    public long getLongByKey(String key) {
        String count = jedis.get(key);
        return count == null ? 0 : Long.parseLong(count);
    }

    public Object getObjectByKey(String key) {
        return jedis.get(key);
    }

    public void setKeyOrValue(String key, Object value) {
        jedis.set(key, String.valueOf(value));
    }

    // Generate key caseType file name caseIndex pre_total_count for storing test cases in Redis
    public String generateRedisKey(String caseType, int caseIndex) {
        return "case_" + caseType + "_" + caseIndex;
    }

    public void saveTestCaseObject(Object object, String key) {
        long start = System.currentTimeMillis();
        byte[] objectAfterSerialize = serializeObject(object);
        long time = System.currentTimeMillis() - start;
        jedis.incrBy(SERIALIZE_TIME, time);
        jedis.incrBy(SERIALIZE_CASE_COUNT, 1);
        jedis.set(key.getBytes(), objectAfterSerialize);
    }

    public static byte[] serializeObject(Object object) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(); Output output = new Output(byteArrayOutputStream)) {
            kryo.writeClassAndObject(output, object);
            output.flush();
            return byteArrayOutputStream.toByteArray();
        } catch (Exception e) {
            // Generic exception handling if needed
            System.err.println("[TestController] Unexpected error in serializeObject: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public Object loadTestCaseObject(String key) {
        byte[] serializedObject = jedis.get(key.getBytes());
        long start = System.currentTimeMillis();
        Object object = deserializeObject(serializedObject);
        long time = System.currentTimeMillis() - start;
        jedis.incrBy(DESERIALIZE_TIME, time);
        jedis.incrBy(DESERIALIZE_CASE_COUNT, 1);
        return object;
    }

    public static Object deserializeObject(byte[] serializedObject) {
        try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serializedObject); Input input = new Input(
                byteArrayInputStream)) {
            return kryo.readClassAndObject(input);
        } catch (Exception e) {
            System.out.println("[TestController] deserializeObject Error message: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public void close() {
        jedis.close();
    }

    public void deleteKey(List<String> keys) {
        if (keys != null && keys.size() > 0) {
            String[] keyArray = keys.toArray(new String[0]);
            jedis.del(keyArray);
        }
    }

    /**
     * add list
     *
     * @param key
     * @param value
     */
    public void addValueByList(String key, String value) {
        jedis.rpush(key, value);
    }

    /**
     * get list
     *
     * @param key
     * @return
     */
    public List<String> getList(String key) {
        return jedis.lrange(key, 0, -1);
    }

    /**
     * delete some values in list
     *
     * @param key
     * @return
     */
    public void delListValue(String key, String value) {
        jedis.lrem(key, 1, value);
    }

    public List<String> getKeyByPattern(String pattern) {
        List<String> keys = new ArrayList<>();
        String cursor = "0";
        do {
            // The SCAN command returns an array containing the cursor and the matching key
            ScanResult<String> scanResult = jedis.scan(cursor, new ScanParams().match(pattern));
            cursor = scanResult.getStringCursor(); // Update the cursor
            // Get the matching key
            for (String key : scanResult.getResult()) {
                keys.add(key);
            }
        } while (!cursor.equals("0")); // Stop when the cursor returns to 0
        return keys;
    }

    //Load the JAR file in the specified directory and set up Kryo's classloader
    public static void classLoader() {
        System.out.println("RedisUtil classLoader start");
        Set<File> jars = new HashSet<>();
        List<String> directories = Arrays.asList("/home/admin/lq01145628/internal_release/imemberprod/app", "/home/admin/.m2/repository"); // Add the directories containing the JAR files
        for (String file : directories) {
            test1(jars, new File(file));
        }
        //jars.add(new File("/home/admin/lq01145628/internal_release/iexpprod/sand-box/VelocityUtilObj.jar"));
        URLClassLoader classLoader = null;
        if (!jars.isEmpty()) {
            try {
                // Load the JAR files
                URL[] urls = jars.stream().map(File::toURI).map(uri -> {
                    try { return uri.toURL(); } catch (Exception e) {
                        e.printStackTrace();
                        return null;
                    }
                }).filter(url -> url != null).toArray(URL[]::new);
                classLoader = new URLClassLoader(urls, RedisUtil.class.getClassLoader());
                kryo.setClassLoader(classLoader);
            } catch (Exception e) {
                e.printStackTrace();
                // Better handling could be implemented here
            }
        }
    }

    //The JAR file is added to the list of files by recursively traversing the directory.
    public static void test1(Set<File> fileList, File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    test1(fileList, file);
                } else {
                    if (file.isFile() && file.getName().endsWith(".jar") && !file.getName().contains("sources")) {
                        fileList.add(file);
                    }
                }
            }
        }
    }
}
