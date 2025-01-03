import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.Currency;

import org.objenesis.instantiator.ObjectInstantiator;
import org.objenesis.strategy.InstantiatorStrategy;
import org.objenesis.strategy.StdInstantiatorStrategy;

import com.alibaba.fastjson.JSONObject;

/**
 * MutationUtil - Utility class for mutating various types of data.
 */
public class MutationUtil {
    private static final SecureRandom secureRandom   = new SecureRandom();
    // 独立随机源
    private static final SecureRandom secureRandom1  = new SecureRandom();  // integer
    private static final SecureRandom secureRandom2  = new SecureRandom();  // integer size
    private static final SecureRandom secureRandom3  = new SecureRandom();  // float
    private static final SecureRandom secureRandom4  = new SecureRandom();  // float size
    private static final SecureRandom secureRandom5  = new SecureRandom();  // double
    private static final SecureRandom secureRandom6  = new SecureRandom();  // double size
    private static final SecureRandom secureRandom7  = new SecureRandom();  // long
    private static final SecureRandom secureRandom8  = new SecureRandom();  // long size
    private static final SecureRandom secureRandom9  = new SecureRandom();  // string size
    private static final SecureRandom secureRandom10 = new SecureRandom();  // string pool select
    private static final SecureRandom secureRandom11 = new SecureRandom();  // string pool char select
    private static final SecureRandom secureRandom12 = new SecureRandom();  // string op
    private static final SecureRandom secureRandom13 = new SecureRandom();  // string offset
    private static final SecureRandom secureRandom14 = new SecureRandom();  // datetime
    private static final SecureRandom secureRandom15 = new SecureRandom();  // enum
    private static final SecureRandom secureRandom16 = new SecureRandom();  // list op
    private static final SecureRandom secureRandom17 = new SecureRandom();  // map op
    private static final SecureRandom secureRandom18 = new SecureRandom();  // date op
    private static final SecureRandom secureRandom19 = new SecureRandom();  // date
    private static final SecureRandom secureRandom20 = new SecureRandom();  // big decimal op
    private static final SecureRandom secureRandom21 = new SecureRandom(); // big decimal
    private static final SecureRandom secureRandom22 = new SecureRandom(); // cro
    private static final Instant      START_DATE     = Instant.parse("2000-01-01T00:00:00Z");
    private static final Instant      END_DATE       = Instant.now();

    // 定义字符池
    private static final String ALPHANUMERIC_POOL = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String COMPLEX_POOL      = ALPHANUMERIC_POOL + "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~ \b\f\n\r\t\u000b\u0000";

    // 获取随机字符池中的字符
    private static char getRandomChar(String pool) {
        return pool.charAt(secureRandom11.nextInt(pool.length()));
    }

    // 获取随机长度的字符串
    private static String getRandomString(String pool, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(getRandomChar(pool));
        }
        return sb.toString();
    }

    // 变异整数
    private static int mutateInteger(Integer num) {
        if (null == num) {
            return secureRandom1.nextInt();
        } else {
            String bits = String.format("%32s", Integer.toBinaryString(num)).replace(' ', '0');
            int numBitsToModify = Math.min(GeometricSample.sampleGeometricTimes(4, secureRandom2), 32);
            char[] modifiedBits = bits.toCharArray();

            for (int i = 0; i < numBitsToModify; i++) {
                int bitIndex = secureRandom1.nextInt(32);
                modifiedBits[bitIndex] = (modifiedBits[bitIndex] == '0') ? '1' : '0';
            }

            int newNum = Integer.parseUnsignedInt(new String(modifiedBits), 2);
            newNum -= (1);
            return newNum;
        }
    }

    // 变异浮点数（单精度）
    private static float mutateFloat(Float num) {
        if (num == null) {
            // 生成一个随机的float值
            return secureRandom3.nextFloat() * (Float.MAX_VALUE - Float.MIN_VALUE) + Float.MIN_VALUE;
        }

        while (true) {
            int bits = Float.floatToIntBits(num);
            String bitStr = String.format("%32s", Integer.toBinaryString(bits)).replace(' ', '0');
            char[] modifiedBits = bitStr.toCharArray();
            int numBitsToModify = Math.min(GeometricSample.sampleGeometricTimes(4, secureRandom4), 32);

            for (int i = 0; i < numBitsToModify; i++) {
                int bitIndex = secureRandom3.nextInt(32);
                modifiedBits[bitIndex] = (modifiedBits[bitIndex] == '0') ? '1' : '0';
            }

            int newBits = Integer.parseUnsignedInt(new String(modifiedBits), 2);
            float newNum = Float.intBitsToFloat(newBits);

            if (!Float.isNaN(newNum) && !Float.isInfinite(newNum)) {
                return newNum;
            }
        }
    }

    // 变异浮点数（双精度）
    private static double mutateDouble(Double num) {
        if (num == null) {
            // 生成一个随机的float值
            return secureRandom5.nextDouble() * (Double.MAX_VALUE - Double.MIN_VALUE) + Double.MIN_VALUE;
        }

        while (true) {
            long bits = Double.doubleToLongBits(num);
            String bitStr = String.format("%64s", Long.toBinaryString(bits)).replace(' ', '0');
            char[] modifiedBits = bitStr.toCharArray();

            int numBitsToModify = Math.min(GeometricSample.sampleGeometricTimes(8, secureRandom6), 64);
            for (int i = 0; i < numBitsToModify; i++) {
                int bitIndex = secureRandom5.nextInt(64);
                modifiedBits[bitIndex] = (modifiedBits[bitIndex] == '0') ? '1' : '0';
            }

            long newBits = Long.parseUnsignedLong(new String(modifiedBits), 2);
            double newNum = Double.longBitsToDouble(newBits);

            if (!Double.isNaN(newNum) && !Double.isInfinite(newNum)) {
                return newNum;
            }
        }
    }

    // 变异长整数
    private static long mutateLong(Long num) {
        if (num == null) {
            return secureRandom7.nextLong();
        } else {
            String bits = String.format("%64s", Long.toBinaryString(num)).replace(' ', '0');
            int numBitsToModify = Math.min(GeometricSample.sampleGeometricTimes(8, secureRandom8), 64);
            char[] modifiedBits = bits.toCharArray();

            for (int i = 0; i < numBitsToModify; i++) {
                int bitIndex = secureRandom7.nextInt(64);
                modifiedBits[bitIndex] = (modifiedBits[bitIndex] == '0') ? '1' : '0';
            }

            long newNum = Long.parseUnsignedLong(new String(modifiedBits), 2);
            newNum -= (1L);

            return newNum;
        }
    }

    // 变异字符串
    private static String mutateString(String input) {
        int length = GeometricSample.sampleGeometricTimes(16, secureRandom9);
        String pool;
        double randomValue = secureRandom10.nextDouble();

        if (randomValue < 0.25) {
            pool = "0123456789";
        } else if (randomValue < 0.5) {
            pool = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        } else if (randomValue < 0.75) {
            pool = ALPHANUMERIC_POOL;
        } else {
            pool = COMPLEX_POOL;
        }

        String mutatedString = getRandomString(pool, length);

        if (input == null || input.isEmpty()) {
            return mutatedString;
        } else {
            int operation = secureRandom12.nextInt(3);
            switch (operation) {
                case 0: // add
                    int addOffset = secureRandom13.nextInt(input.length());
                    return input.substring(0, addOffset) + mutatedString + input.substring(addOffset);
                case 1: // delete
                    int delLength = length;
                    if (delLength > input.length()) {
                        return null;
                    } else if (delLength == input.length()) {
                        return "";
                    } else {
                        int delOffset = secureRandom13.nextInt(input.length() - delLength + 1);
                        return input.substring(0, delOffset) + input.substring(delOffset + delLength);
                    }
                case 2: // modify
                    int modLength = length;
                    if (modLength >= input.length()) {
                        return mutatedString;
                    }
                    int modOffset = secureRandom13.nextInt(input.length() - modLength + 1);
                    return input.substring(0, modOffset) + mutatedString.substring(0, modLength) + input.substring(modOffset + modLength);
            }
        }
        return input; // 不应到这里
    }

    // 生成随机日期时间
    private static Instant generateRandomDateTime() {
        long startEpochSecond = START_DATE.getEpochSecond();
        long endEpochSecond = END_DATE.getEpochSecond();
        long randomEpochSecond = startEpochSecond + (long) (secureRandom14.nextDouble() * (endEpochSecond - startEpochSecond));
        return Instant.ofEpochSecond(randomEpochSecond);
    }

    // 变异日期时间
    private static Instant mutateDateTime(Instant input) {
        if (secureRandom14.nextDouble() < 0.1) {
            return input;
        }
        return generateRandomDateTime();
    }

    private static Object mutateCurrency() {
        CurrencyEnum[] enumConstants = CurrencyEnum.class.getEnumConstants();
        if (enumConstants != null) {
            // 随机选择一个Enum值
            int randomIndex = secureRandom15.nextInt(enumConstants.length);
            return Currency.getInstance(enumConstants[randomIndex].getCurrencyCode());
        }
        return null;
    }

    public static Object mutate(Object input, Class<?> clazz, Type genericType) {
        if (clazz == Integer.class || clazz == int.class) {
            return mutateInteger((Integer) input);
        } else if (clazz == Float.class || clazz == float.class) {
            return mutateFloat((Float) input);
        } else if (clazz == Double.class || clazz == double.class) {
            return mutateDouble((Double) input);
        } else if (clazz == Long.class || clazz == long.class) {
            return mutateLong((Long) input);
        } else if (clazz == Boolean.class || clazz == boolean.class) {
            return secureRandom.nextBoolean();
        } else if (clazz == String.class) {
            return mutateString((String) input);
        } else if (clazz == Instant.class) {
            return mutateDateTime((Instant) input);
        } else if (clazz.isEnum()) {
            return mutateEnum(input, clazz);
        } else if (List.class.isAssignableFrom(clazz)) {
            return mutateList((List<?>) input, genericType);
        } else if (Map.class.isAssignableFrom(clazz)) {
            return mutateMap((Map<?,?>) input, genericType);
        } else if (Date.class.isAssignableFrom(clazz)) {
            return mutateDate((Date) input);
        } else if (BigDecimal.class.isAssignableFrom(clazz)) {
            return mutateBigDecimal((BigDecimal) input);
        } else if (clazz == Currency.class) {
            return mutateCurrency();
        } else {
            return input;
        }
    }

    private static Object mutateEnum(Object input, Class<?> clazz) {
        Object[] enumConstants = clazz.getEnumConstants();
        if (enumConstants != null) {
            // 随机选择一个Enum值
            int randomIndex = secureRandom15.nextInt(enumConstants.length);
            return enumConstants[randomIndex];
        }
        return input;
    }

    // 变异列表
    private static List<?> mutateList(List<?> input, Type genericType) {
        if (null == input) {
            try {
                Constructor<?> declaredConstructor = ArrayList.class.getDeclaredConstructor();
                declaredConstructor.setAccessible(true);
                return (List<?>) declaredConstructor.newInstance();
            } catch (Exception e) {
                System.out.println("[TestController] mutateList error:" + e.getMessage());
                e.printStackTrace(); // 输出堆栈信息，方便调试
            }
        }
        // 获取List中包含的对象类型
        List<Object> newList = new ArrayList<>(input);
        int operation = secureRandom16.nextInt(3);
        switch (operation) {
            case 0: // add an element
                try {
                    int i = GeometricSample.sampleGeometricTimes(4, secureRandom16);
                    if (!newList.isEmpty()) {
                        int i1 = secureRandom16.nextInt(2);
                        switch (i1) {
                            case 0:
                                //复制
                                for (int j = 0; j < i; j++) {
                                    Object o = newList.get(0);
                                    if (null != o) {
                                        //加判断
                                        Object object = RedisUtil.deserializeObject(RedisUtil.serializeObject(o));
                                        newList.add(object);
                                    }
                                }
                                break;
                            case 1:
                                //新建
                                Object o = newList.get(0);
                                if (null != o) {
                                    Class<?> clazz = o.getClass();
                                    for (int j = 0; j < i; j++) {
                                        try {
                                            newList.add(mutateObject(clazz));
                                        } catch (Exception e) {
                                            System.out.println("[TestController] mutateList DeclaredConstructor error:" + e.getMessage());
                                            e.printStackTrace(); // 输出堆栈信息，方便调试
                                        }
                                    }
                                } else {
                                    if (genericType instanceof ParameterizedType) {
                                        try {
                                            ParameterizedType parameterizedType = (ParameterizedType) genericType;
                                            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                                            Class<?> clazz = getMutateClass(actualTypeArguments[0]);
                                            for (int j = 0; j < i; j++) {
                                                newList.add(mutateObject(clazz));
                                            }
                                        } catch (Exception e) {
                                            System.out.println("[TestController] mutateList DeclaredConstructor error:" + e.getMessage());
                                            e.printStackTrace(); // 输出堆栈信息，方便调试
                                        }
                                    }
                                }
                                break;
                        }
                    } else {
                        if (genericType instanceof ParameterizedType) {
                            try {
                                ParameterizedType parameterizedType = (ParameterizedType) genericType;
                                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                                Class<?> clazz = getMutateClass(actualTypeArguments[0]);
                                for (int j = 0; j < i; j++) {
                                    newList.add(mutateObject(clazz));
                                }
                            } catch (Exception e) {
                                System.out.println("[TestController] mutateList DeclaredConstructor error:" + e.getMessage());
                                e.printStackTrace(); // 输出堆栈信息，方便调试
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("[TestController] mutateList 0 error:" + e.getMessage());
                    e.printStackTrace(); // 输出堆栈信息，方便调试
                }
                break;
            case 1: // delete an element
                if (!newList.isEmpty()) {
                    int i = GeometricSample.sampleGeometricTimes(4, secureRandom16);
                    int size = newList.size();
                    if (i >= size) {
                        return new ArrayList<>();
                    }

                    if (i > 0) {
                        newList.subList(0, i).clear();
                    }
                }
                break;
            case 2: // modify an element
                if (!newList.isEmpty()) {
                    int i = GeometricSample.sampleGeometricTimes(4, secureRandom16);
                    for (int j = 0; j < i; j++) {
                        int modIndex = secureRandom16.nextInt(newList.size());
                        Object input1 = newList.get(modIndex);
                        if (null != input1) {
                            if (isPrimitiveOrWrapper(input1.getClass())) {
                                Set<Object> visited = new HashSet<>();
                                newList.set(modIndex, modifyFields(input1, visited));
                            } else {
                                for (Field declaredField : input1.getClass().getDeclaredFields()) {
                                    newList.set(modIndex, mutate(input1, input1.getClass(), declaredField.getGenericType()));
                                    break;
                                }
                            }
                        }
                    }
                }
                break;
        }
        return newList;
    }

    private static List<Field> getAllMutableFields(Class<?> clazz) {
        List<Field> fieldList = new ArrayList<>();
        getAllMutableFieldsRecursive(clazz, fieldList);
        return fieldList;
    }

    public static void getAllMutableFieldsRecursive(Class<?> clazz, List<Field> fieldList) {
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

    public static Object modifyFields(Object obj, Set<Object> visited) {
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
        int[] resultArray = new int[mutateTimes];
        for (int i = 0; i < mutateTimes; i++) {
            resultArray[i] = secureRandom.nextInt(totalFields);
        }
        for (int i : resultArray) {
            Field field = fields.get(i);
            // 检查字段是否是静态的
            if (Modifier.isStatic(field.getModifiers()) || Modifier.isFinal(field.getModifiers())) {
                // 跳过静态字段
                continue;
            }
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                Class<?> fieldType = field.getType();
                if (isPrimitiveOrWrapper(fieldType)) {
                    if (null == value) {
                        field.set(obj, mutateObject(fieldType));
                    } else {
                        // 如果是嵌套对象，递归调用
                        modifyFields(value, visited);
                    }
                } else {
                    Type genericType = field.getGenericType();
                    Object mutatedValue = mutate(value, fieldType, genericType);

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
                e.printStackTrace();
                System.out.println("[TestController] MutationUtil modifyFields error:" + e.getMessage());
            }
        }
        return obj;
    }

    private static <K, V> Map<K,V> mutateMap(Map<K,V> input, Type genericType) {
        if (null == input) {
            try {
                Constructor<?> declaredConstructor = HashMap.class.getDeclaredConstructor();
                declaredConstructor.setAccessible(true);
                return (Map<K,V>) declaredConstructor.newInstance();
            } catch (Exception e) {
                System.out.println("[TestController] mutateMap error:" + e.getMessage());
                e.printStackTrace(); // 输出堆栈信息，方便调试
            }
        }
        // 获取Map中包含的对象类型
        Map<K,V> newMap = new HashMap<>(input);
        int operation = secureRandom17.nextInt(3);
        switch (operation) {
            case 0: // mutate values
                try {
                    if (!newMap.isEmpty()) {
                        int i = GeometricSample.sampleGeometricTimes(4, secureRandom17);
                        for (int j = 0; j < i; j++) {
                            List<K> keys = new ArrayList<>(newMap.keySet());
                            K key = keys.get(secureRandom17.nextInt(keys.size()));
                            // 变异键
                            K newKey = (K) mutate(key, key.getClass(), null);
                            if (null != newKey && newMap.get(key) != null) {
                                int i1 = secureRandom17.nextInt(2);
                                switch (i1) {
                                    case 0:
                                        //序列化
                                        newMap.put(newKey, (V) RedisUtil.deserializeObject(RedisUtil.serializeObject(newMap.get(key))));
                                        break;
                                    case 1:
                                        //新建
                                        V v = newMap.get(key);
                                        if (v != null) {
                                            Class<?> clazz = v.getClass();
                                            try {
                                                newMap.put(newKey, (V) mutateObject(clazz));
                                            } catch (Exception e) {
                                                System.out.println(
                                                        "[TestController] mutateMap DeclaredConstructor error:" + e.getMessage());
                                                e.printStackTrace(); // 输出堆栈信息，方便调试
                                            }
                                        } else {
                                            if (genericType instanceof ParameterizedType) {
                                                try {
                                                    ParameterizedType parameterizedType = (ParameterizedType) genericType;
                                                    Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                                                    Class<?> clazz1 = getMutateClass(actualTypeArguments[1]);
                                                    //初始化完key 会有空的情况
                                                    newMap.put(newKey, (V) mutateObject(clazz1));
                                                } catch (Exception e) {
                                                    System.out.println(
                                                            "[TestController] mutateMap DeclaredConstructor error:" + e.getMessage());
                                                    e.printStackTrace(); // 输出堆栈信息，方便调试
                                                }
                                            }
                                        }
                                        break;
                                }
                            }
                        }
                    } else {
                        if (genericType instanceof ParameterizedType) {
                            try {
                                ParameterizedType parameterizedType = (ParameterizedType) genericType;
                                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                                Class<?> clazz = getMutateClass(actualTypeArguments[0]);
                                Class<?> clazz1 = getMutateClass(actualTypeArguments[1]);
                                //初始化完key 会有空的情况
                                newMap.put((K) mutate(mutateObject(clazz), clazz, null), (V) mutateObject(clazz1));
                            } catch (Exception e) {
                                System.out.println("[TestController] mutateMap DeclaredConstructor error:" + e.getMessage());
                                e.printStackTrace(); // 输出堆栈信息，方便调试
                            }
                        }
                    }
                } catch (java.lang.Exception e) {
                    System.out.println("[TestController] modifyFields map 0 error:" + e.getMessage());
                    e.printStackTrace(); // 输出堆栈信息，方便调试
                }
                break;
            case 1: // delete an entry
                try {
                    if (!newMap.isEmpty()) {
                        int i = GeometricSample.sampleGeometricTimes(4, secureRandom17);
                        List<K> keys = new ArrayList<>(newMap.keySet());
                        int size = keys.size();
                        if (i >= size) {
                            return new HashMap<>();
                        }

                        if (i > 0) {
                            for (int j = 0; j < i; j++) {
                                keys = new ArrayList<>(newMap.keySet());
                                K delKey = keys.get(secureRandom17.nextInt(keys.size()));
                                newMap.remove(delKey);
                            }
                        }
                    }
                } catch (java.lang.Exception e) {
                    System.out.println("[TestController] modifyFields map 1 error:" + e.getMessage());
                    e.printStackTrace(); // 输出堆栈信息，方便调试
                }
                break;
            case 2: // modify a value
                try {
                    if (!newMap.isEmpty()) {
                        int i = GeometricSample.sampleGeometricTimes(4, secureRandom17);
                        for (int j = 0; j < i; j++) {
                            List<K> keys = new ArrayList<>(newMap.keySet());
                            K modKey = keys.get(secureRandom17.nextInt(keys.size()));
                            V input1 = newMap.get(modKey);
                            if (null != input1) {
                                if (isPrimitiveOrWrapper(input1.getClass())) {
                                    Set<Object> visited = new HashSet<>();
                                    newMap.put(modKey, (V) modifyFields(input1, visited));
                                } else {
                                    for (Field declaredField : input1.getClass().getDeclaredFields()) {
                                        newMap.put(modKey, (V) mutate(input1, input1.getClass(), declaredField.getGenericType()));
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } catch (java.lang.Exception e) {
                    System.out.println("[TestController] modifyFields map 2 error:" + e.getMessage());
                    e.printStackTrace(); // 输出堆栈信息，方便调试
                }
                break;
        }
        return newMap;
    }

    public static Object mutateObject(Class<?> fieldType) {
        if (null == fieldType) {
            return null;
        }
        try {
            if (fieldType == Float.class || fieldType == float.class) {
                return secureRandom3.nextFloat() * (Float.MAX_VALUE - Float.MIN_VALUE) + Float.MIN_VALUE;
            } else if (fieldType == Double.class || fieldType == double.class) {
                return secureRandom5.nextDouble() * (Double.MAX_VALUE - Double.MIN_VALUE) + Double.MIN_VALUE;
            } else if (fieldType == Long.class || fieldType == long.class) {
                return secureRandom7.nextLong();
            } else if (fieldType == Instant.class) {
                return generateRandomDateTime();
            } else if (fieldType == BigDecimal.class) {
                return BigDecimal.ZERO;
            } else if (fieldType == Currency.class) {
                return mutateCurrency();
            } else if (fieldType == Boolean.class || fieldType == boolean.class) {
                return secureRandom.nextBoolean();
            } else if (fieldType == Integer.class || fieldType == int.class) {
                return secureRandom1.nextInt();
            } else if (fieldType.isEnum()) {
                return mutateEnum(null, fieldType);
            } else if (List.class.isAssignableFrom(fieldType)) {
                return new ArrayList<>();
            } else if (Map.class.isAssignableFrom(fieldType)) {
                return new HashMap<>();
            } else {
                if (!Modifier.isAbstract(fieldType.getModifiers())) {
                    try {
                        Constructor<?> ctor;
                        try {
                            ctor = fieldType.getConstructor();
                        } catch (Exception e) {
                            System.out.println("no public no-arg constructor");
                            ctor = fieldType.getDeclaredConstructor((Class[]) null);
                            ctor.setAccessible(true);
                        }
                        return ctor.newInstance();
                    } catch (Exception e) {
                        System.out.println("no private no-arg constructor");
                        e.printStackTrace();
                        InstantiatorStrategy strategy = new StdInstantiatorStrategy();
                        ObjectInstantiator instantiator = strategy.newInstantiatorOf(fieldType);
                        return instantiator.newInstance();
                    }
                } else {
                    System.out.println("Cannot instantiate an abstract class: " + fieldType.getName());
                }
            }
        } catch (Exception e) {
            System.out.println("[TestController] mutateObject error:" + e.getMessage());
            e.printStackTrace(); // 输出堆栈信息，方便调试
        }
        return null;
    }

    // 变异时间
    private static Date mutateDate(Date input) {
        Calendar calendar = Calendar.getInstance();
        if (input == null) {
            return calendar.getTime();
        }

        int operation = secureRandom18.nextInt(2);

        calendar.setTime(input);
        switch (operation) {
            case 0:// 使用 Calendar 进行变异
                // 增加随机天数
                int randomDays = (int) (secureRandom19.nextDouble() * 10); // 0到9天
                calendar.add(Calendar.DAY_OF_MONTH, randomDays);
                return calendar.getTime();
            case 1:
                // 随机修改年份
                int randomYear = 2000 + secureRandom19.nextInt(50);
                calendar.set(Calendar.YEAR, randomYear);

                // 随机修改月份
                int randomMonth = secureRandom19.nextInt(12); // 随机月份0-11
                calendar.set(Calendar.MONTH, randomMonth);

                // 随机修改日期
                int randomDay = 1 + secureRandom19.nextInt(28); // 确保不超过28天
                calendar.set(Calendar.DAY_OF_MONTH, randomDay);
                return calendar.getTime();
        }
        return input;
    }

    // 变异数值 BigDecimal
    private static BigDecimal mutateBigDecimal(BigDecimal input) {
        if (input == null) {
            return BigDecimal.valueOf(secureRandom21.nextLong());
        }

        int operation = secureRandom20.nextInt(3);
        switch (operation) {
            case 0: // 随机数增减
                // Generate a random increment/decrement
                BigDecimal change = BigDecimal.valueOf(secureRandom21.nextDouble() * 10).setScale(2, RoundingMode.HALF_UP);
                // Randomly decide whether to add or subtract
                if (secureRandom21.nextBoolean()) {
                    return input.add(change);
                } else {
                    return input.subtract(change);
                }
            case 1: //随机变换小数位
                int scale = secureRandom21.nextInt(5); // 0到4位小数
                return input.setScale(scale, RoundingMode.HALF_UP);
            case 2: // 随机乘法或除法
                BigDecimal factor = BigDecimal.valueOf(1 + (secureRandom21.nextDouble() * 0.5)); // 1到1.5之间
                if (secureRandom21.nextBoolean()) {
                    return input.multiply(factor);
                } else {
                    return input.divide(factor, RoundingMode.HALF_UP);
                }
        }
        return input;
    }

    public static Object mutateCrossover(Object seed1, Object seed2) throws Exception {
        if (seed1 == null && seed2 == null) {
            return null;
        }

        if (seed1 == null || seed2 == null) {
            return seed1 != null ? seed1 : seed2;
        }

        Object copy = RedisUtil.kryo.copy(seed1);
        try {
            Class<?> clazz = seed1.getClass();
            List<Field> fields = new ArrayList<>();
            getAllMutableFieldsRecursive(clazz, fields);

            int size = fields.size();
            double std = (double) size / 6;
            double mean = (double) size / 2;
            long mid = Math.round(std * secureRandom21.nextGaussian() + mean);
            mid = mid < 0 ? 0 : mid > (size - 1) ? (size - 1) : mid;
            for (int i = 0; i < fields.size(); i++) {
                Field field = fields.get(i);
                // 检查字段是否是静态的
                if (Modifier.isStatic(field.getModifiers())) {
                    // 跳过静态字段
                    continue;
                }
                field.setAccessible(true); // 允许访问私有字段
                Object valueFromSeed1 = field.get(seed1);
                Object valueFromSeed2 = field.get(seed2);
                if (i <= mid) {
                    // 保持 seed1 的前一半字段的值
                    if (valueFromSeed1 != null && isPrimitiveOrWrapper(valueFromSeed1.getClass())) {
                        // 如果是嵌套对象，递归调用 mutateCrossover
                        field.set(copy, mutateCrossover(valueFromSeed1, valueFromSeed2));
                    } else {
                        // 检查字段是否为 ConcurrentMap 类型
                        if (ConcurrentMap.class.isAssignableFrom(field.getType()) && valueFromSeed1 != null) {
                            Map mapA = (Map) valueFromSeed1;
                            // 移除 null 值
                            mapA.values().removeIf(Objects::isNull);
                            field.set(copy, new ConcurrentHashMap<>(mapA));
                        } else {
                            field.set(copy, valueFromSeed1);
                        }
                    }
                } else {
                    // 替换为 seed2 的后一半字段的值
                    if (valueFromSeed2 != null && isPrimitiveOrWrapper(valueFromSeed2.getClass())) {
                        // 如果是嵌套对象，递归调用 mutateCrossover
                        field.set(copy, mutateCrossover(valueFromSeed1, valueFromSeed2));
                    } else {
                        // 检查字段是否为 ConcurrentMap 类型
                        if (ConcurrentMap.class.isAssignableFrom(field.getType()) && valueFromSeed2 != null) {
                            Map mapA = (Map) valueFromSeed2;
                            // 移除 null 值
                            mapA.values().removeIf(Objects::isNull);
                            field.set(copy, new ConcurrentHashMap<>(mapA));
                        } else {
                            field.set(copy, valueFromSeed2);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("mutateCrossover error:" + e.getMessage());
            e.printStackTrace();
        }
        return copy; // 返回新对象
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

    public static Class<?> getMutateClass(Type actualTypeArgument) {
        if (actualTypeArgument instanceof Class<?>) {
            return (Class<?>) actualTypeArgument;
        } else if (actualTypeArgument instanceof ParameterizedType) {
            ParameterizedType typeParam = (ParameterizedType) actualTypeArgument;
            Type rawType = typeParam.getRawType();
            if (rawType instanceof Class<?>) {
                return (Class<?>) rawType;
            }
        }
        return null;
    }

}
