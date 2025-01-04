The source code include two approaches, FatFuzz and SandBoxFuzz.  
Because we deploy those two approaches in Ant Group, we conduct testing on one of the internal test frameworks of AntGroup, namely iTest, which derives from TestNG.
iTest has been one of the mainstream test frameworks in AntGroup.  

### A. Preparation  
There are some parameters need to be changed to fix different system under the test.

###### _**config.yaml**_:
This file is the configuration file of LogInstrumentorJar.java. The following describes the roles of each field and how to set them:  
**homePath**: The directory path of the maven project **needs** to be modified.

**externalLibrariesDir**: The directory where the maven project external library files are located is generally fixed and does **not need** to be modified.

**tmpSootOutputDir**: The temporary dump directory used by bytecode conversion does **not need** to be modified.

**targetPaths**: Organize all modules under a maven project in a list form (whether or not they are bytecode converted) because for LogInstrumentorJar, you need to define all the class names used in class parsing.  
  -name: The name of the submodule can be based on the path.  
  -dir: The relative path to the target directory.  
  -tar: The name of the tar file
  
**methodList**: Optionally, a list of methods that should not be inserted into the log statement.This can be used to mask constructors, static constructors, static blocks, lambda expressions, toString, hashCode, and other simple methods when needed, specifying the name property and ignore as true. Don't write if you don't need to.

###### _**LogInstrumentorJar.java**_:
To convert the bytecode source file, the parts that may need to be changed are as follows:  
```
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
```
STMT and BLOCK have the same status (the hierarchy is class-method-block or class-method-stmt), and only BLOCK is described here

**HASHMAP_SIZE_BLOCK**: The size of the hash table, which is set to 2^16=65536 by default, needs to be adjusted according to the information output by LogInstrumen torJar log. 
If the coverage of the overall hash table is about 30-60%, it is appropriate. You may need to increase or decrease this value (usually related to the size of codebase).

**MAX_CLASS_NUM, MAX_METHOD_NUM, MAX_BLOCK_NUM**: Those add up to 32 bits, each representing the maximum number of classes/methods/blocks that can be represented, which can be adjusted according to the relevant information output by the LogInstrumentorJar log.

**METHOD_SHIFT_TO_BLOCK, CLASS_SHIFT_TO_BLOCK**: Corresponding to the above three fields, this indicates how many bits of method to block and class to block are offset, that is, METHOD_SHIFT_TO_BLOCK = MAX_BLOCK_ NUM, CLASS_SHIFT_TO_BLOCK = MAX_METHOD_NUM + MAX_BLOCK_NUM, while 32-CL ASS_SHIFT_TO_BLOCK = MAX_CLASS_NUM.

### B. Execute Fuzzer  
FatFuzz and SandBoxFuzz have the different method to execute, we will intruduce them respectively.  
#### a. How to excute FatFuzz  
FatFuzz needs to much manual efforts on preparation to write two extra files to suppot FatFuzz execution.

The first one is **enums.py**, In this file, the range of all enum types is given as a list, and the values are strings. 
enum ranges are usually obtained from the type of the field 
(i.e., from the name of the field, it is usually either enum or string, or the field type is enum. Then look for the code converted to enum under the same class, tracing back to the enum definition, and get the scope from).  
```
currency = [
 "CNY",
 "USD",
 "HKD",
 ...
]
currencyValue = [
 "156",
 "840",
 "344",
 ...
]
IgtpCommonResultCode = [
 "SUCCESS",
 "UNKNOWN_EXCEPTION",
 "PARAM_ILLEGAL",
 ...
]
...
```

And another one is **mutation.py**, which explains the data types and ranges of all the parameters in the current seed to provide a mutation strategy. 
```
field_list = {
 "#request_IgtpQueryQuoteRequest# transferFromCurrency": {
 "name": "transferFromCurrency",
 "type": "range",
 "datatype": "enum",
 "extend": enums.currency
 },
 "#request_IgtpQueryQuoteRequest# transferToAmount": {
 "name": "transferToAmount",
 "type": "all",
 "datatype": "int",
 "extend": {}
 },
 "#request_IgtpQueryQuoteRequest# payerAgentId": {
 "name": "payerAgentId",
 "type": "all",
 "datatype": "string",
 "extend": {}
 },
 "#IfxQuoteClientMock# cent": {
 "name": "cent",
 "type": "skip",
 "datatype": "enum",
 "extend": enums.centRange
 },
...
}

```
That's a very complex file, which need to near 1 hour to prepare.
In addition, any mistakes will affect the FatFuzz execution failed.

After that we can run command in console to execute FatFuzz
```
cd instrumentation/
sh backup.sh
sh replace.sh
cd ../
cd fuzzing/
python yaml_generation.py
```
The result will be written in txt files.

#### b. How to excute SandBoxFuzz
Comaring with FatFuzz, SandBoxFuzz is easy to run, which saves substantial manual works.
```
cd instrumentation/
sh backup.sh
sh replace.sh
cd ../

```

