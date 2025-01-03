from __future__ import print_function

import ast
import copy
import datetime
import json
import math
import os
import random
import re
import shutil
import string
import struct
import subprocess
import sys
from collections import OrderedDict

sys.path.append('/home/admin/lq01145628/internal_release/iexpprod/fuzzing/init-seeds/inquiryRefund')
from mutation import field_list

root_directory = '/home/admin/lq01145628/internal_release/iexpprod/fuzzing/init-seeds/inquiryRefund'
root_mock_directory = '/home/admin/lq01145628/internal_release/iexpprod/fuzzing/init-seeds/inquiryRefund/ccmock'
template_yaml_path = '/home/admin/lq01145628/internal_release/iexpprod/fuzzing/init-seeds/inquiryRefund/template_yaml'
template_mock_path = '/home/admin/lq01145628/internal_release/iexpprod/fuzzing/init-seeds/inquiryRefund/template_yaml/ccmock'
fuzzing_result_path = '/home/admin/lq01145628/internal_release/iexpprod/fuzzing/fuzzing_results/inquiryRefund'
target_directory = '/home/admin/lq01145628/internal_release/iexpprod/app/test/target/test-classes/testcase/AmsPaymentService/inquiryRefund'
target_mock_directory = '/home/admin/lq01145628/internal_release/iexpprod/app/test/target/test-classes/testcase/AmsPaymentService/inquiryRefund/ccmock'
test_path = '/home/admin/lq01145628/internal_release/iexpprod/app/test/src/test/resources/testcase/AmsPaymentService/inquiryRefund'
test_mock_path = '/home/admin/lq01145628/internal_release/iexpprod/app/test/src/test/resources/testcase/AmsPaymentService/inquiryRefund/ccmock'
clear_log_dir1 = '/home/admin/lq01145628/internal_release/iexpprod/app/test/logs'
clear_log_dir2 = '/home/admin/logs'
# test_command = "mvn test -DskipTests=false -Dsurefire.useFile=false -Dmaven.test.skip=false -Djacoco.skip=true -DtestPhrase=install -DmachineEncoding=UTF8  -DtimeZone=USA -Ddbmode=dev -Dcom.alipay.ldc.zone=SGGZ00B -Dzomde=false -Dcom.alipay.confreg.url=iconfregsession-pool.stable.alipay.net -Ddrm_enabled=false -Ditest.sandbox_test_mode=check"
test_command = "mvn test -DskipTests=false -Dsurefire.useFile=false -DmachineEncoding=UTF8 -Dfile.encoding=UTF-8 -Djacoco.skip=true -Ddbmode=dev -Duser.timezone=UTC -Ddomainname=degz00b.dev.alipay.net -Dzmode=false -Ddrm_enabled=true -Dcom.alipay.confreg.url=iconfregsession-pool.stable.alipay.net -Dcom.alipay.ldc.zone=DEGZ00B -Dapp_name=iexpprod -Dcoverage.app=iexpprod"

data_seed_directory_path = os.path.join(root_directory, 'data_seed_pool.yaml')
request_seed_directory_path = os.path.join(root_directory, 'request_seed_pool.yaml')
seed_statement_path = os.path.join(root_directory, 'seed_statement.txt')
crossover_size = 10
crossover_seed_pool = []
crossover_lists_dict = OrderedDict()
children_from_fault_explore = []  # children generated from root fault cases in last round
fault_explore_root_pool = []  # all root failed cases
fault_explore_root_dict = OrderedDict()

fuzzing_rounds = 100
standard_dic = field_list
Max_coverage = 1
Pre_Max_coverage = 0
MEAN_MUTATION_COUNT = 4  # Mean number of mutations to perform in each round.
MEAN_MUTATION_SIZE_STRING = 8  # Mean number of contiguous bytes to mutate in each mutation (string).
MEAN_BITS_FILP = 4
MEAN_INT_STEP = 4
MEAN_FLOAT_STEP = 4
test_case_counter = 0
child_baseline = 10
favour_factor = 5

secure_random1 = random.SystemRandom()  # mutate string size
secure_random2 = random.SystemRandom()  # mutate operation times
secure_random3 = random.SystemRandom()  # explore arithmetics step
secure_random4 = random.SystemRandom()  # mutate arithmetics bits index
secure_random5 = random.SystemRandom()  # mutate arithmetics bits size
secure_random6 = random.SystemRandom()  # mutate datetime
secure_random7 = random.SystemRandom()  # mutate string - char pool selection
secure_random8 = random.SystemRandom()  # mutate string - operation selection
secure_random9 = random.SystemRandom()  # mutate string - offset selection
secure_random10 = random.SystemRandom()  # mutate string - chars selection
secure_random11 = random.SystemRandom()  # mutate field - field selection
secure_random12 = random.SystemRandom()  # crossover - pair selection
secure_random13 = random.SystemRandom()  # mutate enum - enum selection
secure_random14 = random.SystemRandom()  # mutate map - map empty selection
secure_random15 = random.SystemRandom()  # mutate list - embedded object field selection
secure_random16 = random.SystemRandom()  # mutate list - list empty selection
secure_random17 = random.SystemRandom()  # mutate int and float


def num_generation_favour(seed_coverage, Max_coverage):
    if Max_coverage == 0:
        num_g_f = child_baseline
    else:
        num_g_f = (child_baseline * seed_coverage * favour_factor) // Max_coverage
    return num_g_f


def num_generation(seed_coverage, Max_coverage):
    if Max_coverage == 0:
        num_gen = child_baseline
    else:
        num_gen = (child_baseline * seed_coverage) // Max_coverage
    return num_gen


def sample_geometric_size_string(mean):
    if mean <= 0:
        raise ValueError("mean must be positive")
    p = 1 / float(mean)
    uniform = secure_random1.random()
    log_arg = 1 - uniform
    if log_arg <= 0:
        raise ValueError("Invalid log argument: {}".format(log_arg))
    return math.ceil(math.log(log_arg) / math.log(1 - p))


def sample_geometric_size_arithmetics(mean):
    if mean <= 0:
        raise ValueError("mean must be positive")
    p = 1 / float(mean)
    uniform = secure_random5.random()
    log_arg = 1 - uniform
    if log_arg <= 0:
        raise ValueError("Invalid log argument: {}".format(log_arg))
    return math.ceil(math.log(log_arg) / math.log(1 - p))


def sample_geometric_times(mean):
    if mean <= 0:
        raise ValueError("mean must be positive")
    p = 1 / float(mean)
    uniform = secure_random2.random()
    log_arg = 1 - uniform
    if log_arg <= 0:
        raise ValueError("Invalid log argument: {}".format(log_arg))
    return math.ceil(math.log(log_arg) / math.log(1 - p))


def sample_geometric_int_step(mean):
    if mean <= 0:
        raise ValueError("mean must be positive")
    p = 1 / float(mean)
    uniform = secure_random3.random()
    log_arg = 1 - uniform
    if log_arg <= 0:
        raise ValueError("Invalid log argument: {}".format(log_arg))
    return int(math.ceil(math.log(log_arg) / math.log(1 - p)))


def sample_geometric_float_step(mean):
    if mean <= 0:
        raise ValueError("mean must be positive")
    p = 1 / float(mean)
    uniform = secure_random3.random()
    log_arg = 1 - uniform
    if log_arg <= 0:
        raise ValueError("Invalid log argument: {}".format(log_arg))
    return math.log(log_arg) / math.log(1 - p)


def modify_bits(num):
    bits = bin((num + (1 << 32)) % (1 << 32))[2:].zfill(32)
    num_bits_to_modify = min(int(sample_geometric_size_arithmetics(MEAN_BITS_FILP)), 32)
    modified_bits = list(bits)
    for _ in range(num_bits_to_modify):
        bit_index = secure_random4.randint(0, 31)
        modified_bits[bit_index] = '1' if modified_bits[bit_index] == '0' else '0'
    new_num = int("".join(modified_bits), 2)
    if new_num >= (1 << 31):
        new_num -= (1 << 32)
    return new_num


def modify_bits_float(num):
    while True:
        bits = list(struct.unpack('!4B', struct.pack('!f', num)))
        num_bits_to_modify = min(int(sample_geometric_size_arithmetics(MEAN_BITS_FILP)), 32)
        for _ in range(num_bits_to_modify):
            byte_index = secure_random4.randint(0, 3)
            bit_index = secure_random4.randint(0, 7)
            bits[byte_index] ^= (1 << bit_index)
        new_bytes = struct.pack('!4B', *bits)
        new_num = struct.unpack('!f', new_bytes)[0]

        if not (math.isnan(new_num) or math.isinf(new_num)):
            return new_num


def copy_files(file_list, target_directory):
    for file_path in file_list:
        file_name = os.path.basename(file_path)
        target_file_path = os.path.join(target_directory, file_name)
        # if os.path.exists(target_file_path):
        # print("Replacing existing file: '{}'".format(target_file_path))
        shutil.copy(file_path, target_file_path)


def find_yaml_files(seed_statement_path, directory, template_yaml_path, template_mock_path):
    global Max_coverage
    data_yaml_files = []
    request_yaml_files = []
    request_yaml_files_1 = []
    logic_yaml_files = []
    mock_files = []
    seed_array = []
    for root, dirs, files in os.walk(template_yaml_path):
        for file in files:
            if file.endswith(
                    '.data.yaml') and 'request' not in file and 'response' not in file and 'Request' not in file and 'Response' not in file:
                data_yaml_files.append(os.path.join(root, file))
            # if file.endswith('.yaml') and 'request_' in file and 'Response' not in file:
            if file.endswith('.yaml') and 'Response' not in file and not file.endswith('.logic.yaml') and not file.endswith('.data.yaml'):
                request_yaml_files_1.append(os.path.join(root, file))
            if file.endswith(
                    '.logic.yaml') and 'request' not in file and 'response' not in file and 'Request' not in file and 'Response' not in file:
                logic_yaml_files.append(os.path.join(root, file))

    # for root, dirs, files in os.walk(directory):
    #     for file in files:
    #         if file.endswith('.txt'):
    #             seed_path = os.path.join(root, file)
    #             with open(seed_path, 'r') as seed_file:
    #                 lines = seed_file.readlines()

    for item in os.listdir(directory):
        item_path = os.path.join(directory, item)
        if os.path.isfile(item_path) and item.endswith('.txt'):
            with open(item_path, 'r') as seed_file:
                lines = seed_file.readlines()
                locai_Max_coverage = int(lines[0].strip())
                seed_array = [line.strip().split() for line in lines[1:]]
                # print("Seed array!!!!!!!!!!!!!:", seed_array)
                with open(seed_statement_path, "a") as seed_statement:
                    for line in seed_array:
                        seed_statement.write(' '.join(line) + '\n')
                if locai_Max_coverage > Max_coverage:
                    Max_coverage = locai_Max_coverage
                elif locai_Max_coverage == 0:
                    Max_coverage = locai_Max_coverage

    for root, dirs, files in os.walk(template_mock_path):
        for file in files:
            if file.endswith('.yaml'):
                mock_files.append(os.path.join(root, file))

    request_yaml_files = [item for item in request_yaml_files_1 if item not in mock_files]
    return data_yaml_files, request_yaml_files, seed_array, logic_yaml_files, mock_files


def create_data_block(data_yaml_files, data_seed_directory_path, target_path, seed_array, Max_coverage):
    new_seed_count = 0
    a = 0
    global test_case_counter
    pre_test_case_counter = test_case_counter
    for source_file in data_yaml_files:
        file_name = source_file.split('/')[-1]
        target_file_path = os.path.join(target_path, file_name)
        test_case_blocks = []

        with open(source_file, 'r') as s_file:
            test_case_blocks = s_file.read().split('- !!com.ipay.itest.common.model.TestData')

        with open(target_file_path, 'w') as t_file:
            for block in test_case_blocks[1:]:

                for line in block.split('\n'):
                    if '_index: ' in line:
                        index_value = line.split("_index: case_")[-1]
                        statement = None
                        coverage = None
                        for row in seed_array:
                            if row[0] == index_value:
                                a = 1
                                statement = row[1]
                                if statement != 'SF' and statement != 'YF' and statement != 'IF':
                                    coverage = int(row[2])
                                with open(data_seed_directory_path, 'a') as data_seed_file:
                                    new_block = '- !!com.ipay.itest.common.model.TestData' + block
                                    data_seed_file.write(new_block)
                                break
                            else:
                                a = 0

                if a == 0:
                    continue

                num_g = child_baseline
                if statement == '+':
                    # print("Data Statement is +")
                    num_g = num_generation(coverage, Max_coverage)
                    new_seed_count += 1
                elif statement == '++':
                    # print("Data Statement is ++")
                    num_g = num_generation_favour(coverage, Max_coverage)
                    new_seed_count += 1
                elif statement == 'SF':
                    # print("Data Statement is SF")
                    num_g = 0
                elif statement == 'F':
                    # print("Data Statement is F")
                    num_g = 0
                elif statement == 'F+':
                    # print("Data Statement is F+")
                    num_g = num_generation(coverage, Max_coverage)
                    new_seed_count += 1
                elif statement == 'F++':
                    # print("Data Statement is F++")
                    num_g = num_generation_favour(coverage, Max_coverage)
                    new_seed_count += 1
                elif statement == 'N':
                    # print("Request Statement is N")
                    num_g = 0
                elif statement == 'YF':
                    # print("Request Statement is YF")
                    num_g = 0
                elif statement == 'IF':
                    # print("Request Statement is IF")
                    num_g = 0

                for i in range(1, num_g + 1):
                    test_case_counter += 1

                    new_block = '- !!com.ipay.itest.common.model.TestData' + block
                    new_block = re.sub(r'(dataId:\s+.+_DATA_)\S+', lambda x: "{}{}".format(x.group(1), test_case_counter), new_block)
                    new_block = re.sub(r'(logicId:\s+.+_LOGIC_)\S+', lambda x: "{}{}".format(x.group(1), test_case_counter), new_block)
                    new_block = re.sub(r'(_index):\s+\S+', lambda x: "{}: case_{}".format(x.group(1), test_case_counter),
                                       new_block)
                    t_file.write(new_block)

        # if pre_test_case_counter + cases_from_seed_pool_one_round == test_case_counter:
        #     break

    return pre_test_case_counter, new_seed_count


def create_logic_block(logic_yaml_files, target_path, seed_array, pre_test_case_counter, Max_coverage):
    a = 0
    pre_favour = 0

    for source_file in logic_yaml_files:
        file_name = source_file.split('/')[-1]
        target_file_path = os.path.join(target_path, file_name)
        test_case_blocks = []

        with open(source_file, 'r') as s_file:
            test_case_blocks = s_file.read().split('- !!com.ipay.itest.common.model.TestLogic')

        with open(target_file_path, 'w') as t_file:
            for block in test_case_blocks[1:]:
                for line in block.split('\n'):
                    if "case_" in line:
                        index_value = line.split("case_")[-1]
                        index_value = index_value.rstrip("'")
                        statement = None
                        coverage = None
                        for row in seed_array:
                            if row[0] == index_value:
                                a = 1
                                statement = row[1]
                                if statement != 'SF' and statement != 'YF' and statement != 'IF':
                                    coverage = int(row[2])
                                break
                            else:
                                a = 0

                if a == 0:
                    continue

                num_g = child_baseline
                if statement == '+':
                    # print("Logic Statement is +")
                    num_g = num_generation(coverage, Max_coverage)
                elif statement == '++':
                    # print("Logic Statement is ++")
                    num_g = num_generation_favour(coverage, Max_coverage)
                elif statement == 'SF':
                    # print("Data Statement is SF")
                    num_g = 0
                elif statement == 'F':
                    # print("Data Statement is F")
                    num_g = 0
                elif statement == 'F+':
                    # print("Data Statement is F+")
                    num_g = num_generation(coverage, Max_coverage)
                elif statement == 'F++':
                    # print("Data Statement is F++")
                    num_g = num_generation_favour(coverage, Max_coverage)
                elif statement == 'N':
                    # print("Request Statement is N")
                    num_g = 0
                elif statement == 'YF':
                    # print("Request Statement is YF")
                    num_g = 0
                elif statement == 'IF':
                    # print("Request Statement is IF")
                    num_g = 0

                start_request_count = pre_test_case_counter + 1 + pre_favour
                end_request_count = start_request_count + num_g
                for i in range(start_request_count, end_request_count):
                    pattern1 = r"(ccmock:mock_facade\?targetName='[^']+'&index=')\S+(')"
                    pattern2 = r"(ccmock:mock_agent\?targetName='[^']+'&index=')\S+(')"
                    new_block = '- !!com.ipay.itest.common.model.TestLogic' + block
                    new_block = re.sub(r'(logicId:\s+.+_LOGIC_)\S+', lambda x: "{}{}".format(x.group(1), i), new_block)
                    new_block = re.sub(pattern1, r"\g<1>case_{}\g<2>".format(i), new_block)
                    new_block = re.sub(pattern2, r"\g<1>case_{}\g<2>".format(i), new_block)
                    t_file.write(new_block)
                pre_favour += num_g


def parse_block_to_dict(block):
    block_dict = OrderedDict()
    for line in block.split('\n'):
        if ':' in line:
            key, raw_value = line.split(':', 1)
            value = raw_value.strip()
            if '!!' in value:
                if (value.startswith('"') and value.endswith('"')) or (value.startswith("'") and value.endswith("'")):
                    value = value[1:-1]
                value_parts = value.split(' ')
                new_value_parts = []
                for part in value_parts:
                    if (part.startswith('"') and part.endswith('"')) or (part.startswith("'") and part.endswith("'")):
                        new_value_parts.append(part[1:-1])
                    else:
                        new_value_parts.append(part)
                value = ' '.join(new_value_parts)
                block_dict[key] = value
            else:
                value = value.strip().strip("\'\"")
                block_dict[key] = value
    return block_dict


def create_request_mock_block(request_yaml_files, request_seed_directory_path, target_path, mock_files, target_mock_directory, seed_array,
                              pre_test_case_counter, Max_coverage):
    global crossover_seed_pool
    pre_favour = 0
    num_g = child_baseline
    num_g_list = []
    filtered_request_dic = OrderedDict()
    for request_file in request_yaml_files:
        file_name = request_file.split('/')[-1]
        target_request_path = os.path.join(target_path, file_name)
        with open(target_request_path, 'w') as t_file:
            pass
        request_blocks = []
        with open(request_file, 'r') as s_file:
            content = s_file.read()
            request_blocks = re.findall(r'(case_\d+:.*?(?=\ncase_\d+:|\Z))', content, re.DOTALL | re.MULTILINE)

        seed_indexes = [row[0] for row in seed_array]
        filtered_request_list = []
        for block in request_blocks:
            match = re.match(r'^case_(\d+):', block.strip())
            if match:
                block_index = match.group(1)
                if block_index in seed_indexes:
                    filtered_request_list.append(block)

        counter = 1
        for block in filtered_request_list:
            match = re.match(r'^case_(\d+):', block.strip())
            if match:
                block_index = match.group(1)
                request_dict = {}
                statement = ''
                for row in seed_array:
                    if row[0] == block_index:
                        statement = row[1]
                        if statement != 'SF' and statement != 'YF' and statement != 'IF':
                            coverage = int(row[2])
                if statement == '+':
                    # print("Request Statement is +")
                    request_dict = {block_index: coverage}
                    num_g = num_generation(coverage, Max_coverage)
                elif statement == '++':
                    # print("Request Statement is ++")
                    request_dict = {block_index: coverage}
                    num_g = num_generation_favour(coverage, Max_coverage)
                elif statement == 'SF':
                    # print("Data Statement is SF")
                    num_g = 0
                elif statement == 'F':
                    # print("Data Statement is F")
                    request_dict = {block_index: coverage}
                    num_g = 0
                elif statement == 'F+':
                    # print("Data Statement is F+")
                    request_dict = {block_index: coverage}
                    num_g = num_generation(coverage, Max_coverage)
                elif statement == 'F++':
                    # print("Data Statement is F++")
                    request_dict = {block_index: coverage}
                    num_g = num_generation_favour(coverage, Max_coverage)
                elif statement == 'N':
                    # print("Request Statement is N")
                    request_dict = {block_index: coverage}
                    num_g = 0
                elif statement == 'YF':
                    # print("Request Statement is YF")
                    num_g = 0
                elif statement == 'IF':
                    # print("Request Statement is IF")
                    num_g = 0
                with open(request_seed_directory_path, "a") as request_seed_file:
                    request_seed_file.write(block + '\n')
                num_g_list.append(num_g)
                if block_index in seed_indexes:
                    if len(request_dict) > 0:
                        if len(crossover_seed_pool) < crossover_size:
                            crossover_seed_pool.append(request_dict)
                        elif len(crossover_seed_pool) >= crossover_size:
                            # print("before0crossover_seed_pool", crossover_seed_pool)
                            crossover_seed_pool.append(request_dict)
                            # print("before1crossover_seed_pool", crossover_seed_pool)
                            crossover_seed_pool = sorted(crossover_seed_pool, key=lambda x: x.values()[0], reverse=True)  # sort ranking
                            # print("before2crossover_seed_pool", crossover_seed_pool)
                            crossover_seed_pool = crossover_seed_pool[:crossover_size]
                            # print("before3crossover_seed_pool", crossover_seed_pool)
                        block_dic = parse_block_to_dict(block)
                        request_file_name = file_name.split('.')[0]
                        request_file_name_lab = '#' + request_file_name + '#_' + str(counter)
                        filtered_request_dic[request_file_name_lab] = block_dic
                        crossover_lists_dict[request_file_name + '_' + block_index] = block_dic
                        counter += 1
                    else:
                        block_dic = parse_block_to_dict(block)
                        request_file_name = file_name.split('.')[0]
                        request_file_name_lab = '#' + request_file_name + '#_' + str(counter)
                        filtered_request_dic[request_file_name_lab] = block_dic
                        counter += 1

    filtered_mock_dic = OrderedDict()
    for mock_file in mock_files:
        file_name = mock_file.split('/')[-1]
        target_mock_path = os.path.join(target_mock_directory, file_name)
        with open(target_mock_path, 'w') as t_file:
            pass
        mock_blocks = []
        with open(mock_file, 'r') as s_file:
            content = s_file.read()
            mock_blocks = re.findall(r'(case_\d+:.*?(?=\ncase_\d+:|\Z))', content, re.DOTALL | re.MULTILINE)
        seed_indexes = [row[0] for row in seed_array]
        counter = 1
        for block in mock_blocks:
            match = re.match(r'^case_(\d+):', block.strip())
            if match:
                block_index = match.group(1)
                if block_index in seed_indexes:
                    mock_dic = parse_block_to_dict(block)
                    mock_file_name = file_name.split('.')[0]
                    mock_file_name_lab = '#' + mock_file_name + '#_' + str(counter)
                    filtered_mock_dic[mock_file_name_lab] = mock_dic
                    crossover_lists_dict[mock_file_name + '_' + block_index] = mock_dic
                    counter += 1

    for i in range(1, len(filtered_request_dic) + 1):
        num_g_out = num_g_list[i - 1]
        dict_component_rm = OrderedDict()
        pattern = re.compile(r'(#.*#)_' + str(i))
        for key in filtered_request_dic:
            match = pattern.match(key)
            if match:
                new_key = match.group(1)
                dict_component_rm[new_key] = filtered_request_dic[key]
        for key in filtered_mock_dic:
            match = pattern.match(key)
            if match:
                new_key = match.group(1)
                dict_component_rm[new_key] = filtered_mock_dic[key]
        # print("dict_component_rm",dict_component_rm)

        start_count = pre_test_case_counter + 1 + pre_favour
        end_count = start_count + num_g_out

        for k in range(start_count, end_count):

            dict_component_kv = OrderedDict()
            for key in dict_component_rm:
                for sub_key, value in dict_component_rm[key].iteritems():
                    new_key = key + sub_key
                    dict_component_kv[new_key] = value
            # print("dict_component_kv",dict_component_kv)

            # standard_parameter
            standard_dic_kv = OrderedDict()
            for key_a, value_a in dict_component_kv.iteritems():
                type_value = standard_dic.get(key_a, {}).get('type')
                if type_value in ('all', 'range'):
                    datatype_value = standard_dic.get(key_a, {}).get('datatype')
                    if datatype_value == 'list':
                        list_value_dic = standard_dic.get(key_a, {}).get('extend')
                        extend_list_fields = list_value_dic['fields']
                        len_list_fields = len(extend_list_fields)
                        for i in range(1, len_list_fields):
                            key_a_add = '!' + str(i) + '!' + key_a
                            standard_dic_kv[key_a_add] = value_a
                    standard_dic_kv[key_a] = value_a
                elif type_value == 'skip':
                    datatype_value = standard_dic.get(key_a, {}).get('datatype')
                    if datatype_value == 'string' or datatype_value == 'int' or datatype_value == 'long' or datatype_value == 'float' or datatype_value == 'enum':
                        if len(value_a.split('!!')) == 2 or datatype_value == 'enum':
                            dict_component_kv[key_a] = value_a
                        else:
                            dict_component_kv[key_a] = '\'' + value_a + '\''
            # print("standard_dic_kv",standard_dic_kv)

            mutation_time = int(sample_geometric_times(MEAN_MUTATION_COUNT))

            for _ in range(mutation_time):
                # mutation
                standard_dic_kv = dict_Mutation(standard_dic_kv)
                # print("standard_dic_kv",standard_dic_kv)

            for key_a, value_a in standard_dic_kv.iteritems():
                datatype_value = standard_dic.get(key_a, {}).get('datatype')
                if datatype_value == 'string':
                    string_gen = standard_dic_kv[key_a]
                    if string_gen == 'null':
                        standard_dic_kv[key_a] = '{}'.format(string_gen)
                    else:
                        standard_dic_kv[key_a] = '"{}"'.format(string_gen)
                elif datatype_value == 'enum' or datatype_value == 'mixed':
                    string_gen = standard_dic_kv[key_a]
                    if string_gen == 'null':
                        standard_dic_kv[key_a] = '{}'.format(string_gen)
                    else:
                        if string_gen.startswith('!'):
                            spilt_values = string_gen.split(' ', 1)
                            standard_dic_kv[key_a] = spilt_values[0] + ' ' + '\'{}\''.format(spilt_values[1])
                        else:
                            standard_dic_kv[key_a] = '\'{}\''.format(string_gen)
                elif datatype_value == 'int' or datatype_value == 'long' or datatype_value == 'float':
                    string_gen = standard_dic_kv[key_a]
                    if string_gen == 'null':
                        standard_dic_kv[key_a] = '{}'.format(string_gen)
                    else:
                        standard_dic_kv[key_a] = '\'{}\''.format(string_gen)

            # recombine
            for key_a, value_a in standard_dic_kv.iteritems():
                for key_b, value_b in dict_component_kv.iteritems():
                    if key_a == key_b:
                        dict_component_kv[key_a] = value_a
            # print("dict_component_kv",dict_component_kv)
            # print('\n\n')

            new_dic = OrderedDict()

            for key, value in dict_component_kv.items():
                split_keys = key.split('#')
                b_key = split_keys[1].strip()
                b_value_key = split_keys[2] if len(split_keys) > 2 else ''
                if b_key not in new_dic:
                    new_dic[b_key] = OrderedDict()

                new_dic[b_key][b_value_key] = value

            # print("new_dic",new_dic)

            for key, value in new_dic.items():
                output_path = target_directory if 'request_' in key else target_mock_directory
                file_path = os.path.join(output_path, key + '.yaml')
                with open(file_path, 'a') as file:
                    for inner_key, inner_value in value.items():
                        if 'case_' in inner_key:
                            inner_key = 'case_' + str(k)
                            file.write('{}: {}\n'.format(inner_key, inner_value))
                        else:
                            file.write('{}: {}\n'.format(inner_key, inner_value))
                    file.write('\n')

        pre_favour = pre_favour + num_g_out


def dict_Mutation(standard_dic_kv):
    if not standard_dic_kv:
        return standard_dic_kv

    keys = list(standard_dic_kv.keys())

    key_a = secure_random11.choice(keys)

    if key_a.startswith('!'):
        key_a = re.sub(r"^!\d+!", "", key_a)

    datatype_value = standard_dic.get(key_a, {}).get('datatype')

    if datatype_value == 'int' or datatype_value == 'long':
        origin_int = secure_random17.randint(0, 2 ** 31 - 1)
        if standard_dic_kv[key_a] != 'null' and standard_dic_kv[key_a] != '':
            origin_int = int(standard_dic_kv[key_a])
        new_int = modify_bits(origin_int)
        standard_dic_kv[key_a] = new_int
    elif datatype_value == 'boolean':
        if standard_dic_kv[key_a] == 'true':
            standard_dic_kv[key_a] = 'false'
        elif standard_dic_kv[key_a] == 'false':
            standard_dic_kv[key_a] = 'true'
    elif datatype_value == 'string':
        string_gen = mutate_string(standard_dic_kv[key_a])
        standard_dic_kv[key_a] = '{}'.format(string_gen)
    elif datatype_value == 'datetime':
        date = generate_random_datetime_or_null()
        if date == None:
            standard_dic_kv[key_a] = 'null'
        else:
            standard_dic_kv[key_a] = date
    elif datatype_value == 'float':
        origin_float = float(secure_random17.randint(0, 2 ** 31 - 1))
        if standard_dic_kv[key_a] != 'null' and standard_dic_kv[key_a] != '':
            origin_float = float(standard_dic_kv[key_a])
        new_float = modify_bits_float(origin_float)
        standard_dic_kv[key_a] = new_float
    elif datatype_value == 'enum':
        enum_value = standard_dic_kv[key_a]
        enum_value_pre = enum_value.split(' ', 1)
        enum_value_list = standard_dic.get(key_a, {}).get('extend')
        enum_value_back = secure_random13.choice(enum_value_list)
        if enum_value_back == '#random#':
            enum_value_back = mutate_string('null')
        if enum_value_pre[0].startswith('!'):
            enum_value_new = enum_value_pre[0] + ' ' + enum_value_back
        else:
            enum_value_new = enum_value_back
        standard_dic_kv[key_a] = enum_value_new
    elif datatype_value == 'mixed':
        enum_value_dic = standard_dic.get(key_a, {}).get('extend')
        enum_len = enum_value_dic['len']
        enum_value_list = enum_value_dic['mode']
        enum_sep = enum_value_dic['sep'][0]
        enum_value_back = ''
        for i in range(enum_len):
            enum_value_back += secure_random13.choice(enum_value_list[i])
            if i < enum_len - 1:
                enum_value_back += enum_sep
        standard_dic_kv[key_a] = enum_value_back
    elif datatype_value == 'map':
        map_value = standard_dic_kv[key_a]
        if map_value == 'null' or map_value.replace(' ', '') == '{}':
            enum_value_dic = standard_dic.get(key_a, {}).get('extend')
            v2 = ''
            if 'init' is enum_value_dic:
                v2 = enum_value_dic['init']
            else:
                key_sub = mutate_string('null')
                value_sub = mutate_string('null')
                v2 = "{'" + key_sub + "'" + ':' + "'" + value_sub + "'}"
            standard_dic_kv[key_a] = v2
        else:
            map_value = map_value.replace('null', "'null'")
            map_value = map_value.replace('\n', '')
            map_sub_dic = ast.literal_eval(map_value)
            for k, v in map_sub_dic.items():
                random_num = secure_random14.randint(1, 100)
                if 1 <= random_num <= 90:
                    v = mutate_string(v)
                elif 91 <= random_num <= 95:
                    v = '{}'
                else:
                    v = 'null'
            map_sub_dic[k] = v
            str_dic = str(map_sub_dic)
            str_dic = str_dic.replace("'null'", "null")
            standard_dic_kv[key_a] = str_dic
    elif datatype_value == 'list':
        list_value = standard_dic_kv[key_a]
        list_value_dic = standard_dic.get(key_a, {}).get('extend')
        extend_list_init = list_value_dic['init']
        extend_list_fields = list_value_dic['fields']
        if list_value.replace(' ', '') == '[]' or list_value == 'null':
            standard_dic_kv[key_a] = extend_list_init
        else:
            random_num = secure_random16.randint(1, 100)
            if 91 <= random_num <= 95:
                standard_dic_kv[key_a] = '[ ]'
            elif 96 <= random_num <= 100:
                standard_dic_kv[key_a] = 'null'
            else:
                # print("list_value",list_value)
                list_value = list_value.replace("null", "'null'")
                list_sub_list = ast.literal_eval(list_value)
                keys_entend = list(extend_list_fields.keys())
                key = secure_random15.choice(keys_entend)
                # print("list_sub_list",list_sub_list[0])
                # print('list key',key)
                value = eval("list_sub_list[0]" + key)

                type_ = extend_list_fields[key]['type']
                datatype = extend_list_fields[key]['datatype']

                if type_ == 'all' or type_ == 'range':
                    if datatype == 'string':
                        value = mutate_string(value)
                        value = '\'' + value + '\''
                    elif datatype == 'int' or datatype == 'long':
                        # step_int = sample_geometric_int_step(MEAN_INT_STEP)
                        origin_int = secure_random17.randint(0, 2 ** 31 - 1)
                        if value != 'null' and value != '':
                            origin_int = int(value)
                        new_int = modify_bits(origin_int)
                        # new_int = origin_int + step_int
                        # if new_int >= 2 ** 31 - 1:
                        #     new_int = 0
                        # sign_flip = secure_random4.randint(0, 1)
                        # if sign_flip == 1:
                        #     new_int = - new_int
                        value = new_int
                        # value = secure_random11.randint(- 2 ** 31, 2 ** 31 - 1)
                        # print('After randint:', type(value))
                        value = '\'' + str(value) + '\''
                    elif datatype == 'boolean':
                        if value == 'true':
                            value = 'false'
                        elif value == 'false':
                            value = 'true'
                        value = '\'' + value + '\''
                    elif datatype == 'datetime':
                        value = generate_random_datetime_or_null()
                        if value == None:
                            value = 'null'
                        value = '\'' + value + '\''
                    elif datatype == 'float':
                        # step_float = sample_geometric_float_step(MEAN_FLOAT_STEP)
                        origin_float = float(secure_random17.randint(0, 2 ** 31 - 1))
                        if value != 'null' and value != '':
                            origin_float = float(value)
                        new_float = modify_bits_float(origin_float)
                        value = '\'' + str(new_float) + '\''
                    elif datatype == 'enum':
                        enum_value = value
                        enum_value_pre = enum_value.split(' ', 1)
                        enum_value_list = extend_list_fields[key]['extend']
                        enum_value_back = secure_random13.choice(enum_value_list)
                        if enum_value_back == '#random#':
                            enum_value_back = mutate_string('null')
                        if enum_value_pre[0].startswith('!'):
                            enum_value_new = enum_value_pre[0] + ' ' + enum_value_back
                        else:
                            enum_value_new = enum_value_back
                        value = '\'' + enum_value_new + '\''
                    elif datatype == 'mixed':
                        enum_value_dic = extend_list_fields[key]['extend']
                        enum_len = enum_value_dic['len']
                        enum_value_list = enum_value_dic['mode']
                        enum_sep = enum_value_dic['sep'][0]
                        enum_value_back = ''
                        for i in range(enum_len):
                            enum_value_back += secure_random13.choice(enum_value_list[i])
                            if i < enum_len - 1:
                                enum_value_back += enum_sep
                        value = '\'' + enum_value_back + '\''

                    # key_without_list = re.findall(r"\[([^\[\]]+)\]", key)
                    # print("listvalue",value)
                    # print(r"rlistvalue",value)
                    # print('listvaluetype',type(value))
                    eval(compile("list_sub_list[0]" + key + "=" + str(value), '<string>', 'exec'))
                    finial_list = str(list_sub_list)
                    finial_list = finial_list.replace("'null'", "null")
                    standard_dic_kv[key_a] = finial_list

    return standard_dic_kv


def mutate_string(s):
    charpool_type = secure_random7.choice([
        "digit", "letter", "alphanumeric", "constraint"
    ])

    if s == 'null' or s == "":
        a = ''
        char_pool = get_char_pool(charpool_type)
        return add_string(a, char_pool, int(sample_geometric_size_string(MEAN_MUTATION_SIZE_STRING)))
    elif s == None:
        a = ''
        char_pool = get_char_pool(charpool_type)
        return add_string(a, char_pool, int(sample_geometric_size_string(MEAN_MUTATION_SIZE_STRING)))

    char_pool = get_char_pool(charpool_type)

    operation_length = int(sample_geometric_size_string(MEAN_MUTATION_SIZE_STRING))
    operation = secure_random8.choice(["add", "trim", "modify"])
    # print("s", s)
    offset = secure_random9.randint(0, max(1, len(s) - operation_length)) if len(s) > 0 else 0

    if operation == "add":
        return add_string(s, char_pool, operation_length)
    elif operation == "trim":
        return trim_string(s, operation_length, offset)
    elif operation == "modify":
        return modify_string(s, char_pool, operation_length, offset)


def get_char_pool(mutation_type):
    if mutation_type == "digit":
        return string.digits
    elif mutation_type == "letter":
        return string.letters
    elif mutation_type == "alphanumeric":
        return string.digits + string.letters
    elif mutation_type == "constraint":
        punctuation = "!$%*+-./:;<=>@^_|~ "
        return (string.digits + string.letters + punctuation)
    elif mutation_type == "complexfull":
        # probably failed in early yaml parse phase
        return (string.digits + string.letters + string.punctuation + " \b\f\n\r\t\v\0")


def add_string(s, char_pool=None, operation_length=0):
    char_pool = char_pool or get_char_pool("constraint")
    random_chars = ''.join(secure_random10.choice(char_pool) for _ in range(operation_length))
    insert_offset = secure_random9.randint(0, len(s) + 1)
    return s[:insert_offset] + random_chars + s[insert_offset:]


def trim_string(s, operation_length, offset):
    if operation_length >= len(s):
        return 'null' if operation_length > len(s) else ""
    else:
        if offset + operation_length >= len(s):
            return s[:offset]
        else:
            return s[:offset] + s[offset + operation_length:]


def modify_string(s, char_pool, operation_length, offset):
    random_chars = ''.join(secure_random10.choice(char_pool) for _ in range(operation_length))
    return s[:offset] + random_chars + s[offset + operation_length:]


def generate_random_datetime_or_null():
    if secure_random6.random() < 0.1:
        return None

    start_date = datetime.datetime(2000, 1, 1)
    end_date = datetime.datetime.now()

    time_between_dates = (end_date - start_date).days * 24 * 3600 + (end_date - start_date).seconds

    random_seconds = random.uniform(0, time_between_dates)

    random_date = start_date + datetime.timedelta(seconds=random_seconds)

    random_date_str = random_date.strftime('%Y-%m-%dT%H:%M:%SZ')

    return random_date_str


def number_of_combinations(n, r):
    return math.factorial(n) // (math.factorial(r) * math.factorial(n - r))


def crossover_create_request_mock(crossover_seed_pool, crossover_lists_dict, target_directory, target_mock_directory):
    cross_over_count = 0

    crossover_seed_pool = sorted(crossover_seed_pool, key=lambda x: x.values()[0], reverse=True)  # sort ranking
    if len(crossover_seed_pool) > crossover_size:
        crossover_seed_pool = crossover_seed_pool[:crossover_size]
    # print("13123213213",crossover_seed_pool)
    if len(crossover_seed_pool) == 1:
        combinations_count = 2
    else:
        combinations_count = number_of_combinations(len(crossover_seed_pool), 2)
    # delete
    for _ in range(combinations_count):
        if len(crossover_seed_pool) >= 2:
            chosen_pairs = secure_random12.sample(crossover_seed_pool, 2)
        else:
            chosen_pairs = [crossover_seed_pool[0], crossover_seed_pool[0]]

        pattern_1 = re.compile(r'_' + list(chosen_pairs[0].keys())[0] + '$')
        pattern_2 = re.compile(r'_' + list(chosen_pairs[1].keys())[0] + '$')
        list_1 = [key for key in crossover_lists_dict.keys() if pattern_1.search(key)]
        list_2 = [key for key in crossover_lists_dict.keys() if pattern_2.search(key)]

        if list_1 and list_2:
            list_1.sort()
            list_2.sort()
        for key_1, key_2 in zip(list_1, list_2):  # method1 random
            base_key_1 = key_1.rsplit('_', 1)[0]
            base_key_2 = key_2.rsplit('_', 1)[0]

            dict_1 = crossover_lists_dict[key_1]
            dict_2 = crossover_lists_dict[key_2]
            # print("dict_1", dict_1)
            # print("dict_2", dict_2)
            merged_dict = OrderedDict()
            keys = list(set(dict_1.keys()) & set(dict_2.keys()))

            for k in dict_1:
                if k in keys:
                    choice = random.choice([True, False])
                    v1 = dict_1[k]
                    v2 = dict_2[k]
                    v_c = v1 if choice else v2
                    merged_dict[k] = v_c
                elif k not in keys:
                    merged_dict[k] = dict_1[k]
            new_key = '#' + base_key_1 + '#'
            crossover_dict = OrderedDict()
            crossover_dict[new_key] = merged_dict
            dict_component_kv = OrderedDict()
            for key in crossover_dict:
                for sub_key, value in crossover_dict[key].iteritems():
                    new_key = key + sub_key
                    dict_component_kv[new_key] = value

            standard_dic_kv = OrderedDict()
            for key_a, value_a in dict_component_kv.iteritems():
                type_value = standard_dic.get(key_a, {}).get('type')
                if type_value in ('all', 'range'):
                    datatype_value = standard_dic.get(key_a, {}).get('datatype')
                    if datatype_value == 'list':
                        list_value_dic = standard_dic.get(key_a, {}).get('extend')
                        extend_list_fields = list_value_dic['fields']
                        len_list_fields = len(extend_list_fields)
                        for i in range(1, len_list_fields):
                            key_a_add = '!' + str(i) + '!' + key_a
                            standard_dic_kv[key_a_add] = value_a
                    standard_dic_kv[key_a] = value_a
                elif type_value == 'skip':
                    datatype_value = standard_dic.get(key_a, {}).get('datatype')
                    if datatype_value == 'string' or datatype_value == 'int' or datatype_value == 'long' or datatype_value == 'float' or datatype_value == 'enum':
                        if len(value_a.split('!!')) == 2 or datatype_value == 'enum':
                            dict_component_kv[key_a] = value_a
                        else:
                            dict_component_kv[key_a] = '\'' + value_a + '\''

            standard_dic_kv = dict_Mutation(standard_dic_kv)

            for key_a, value_a in standard_dic_kv.iteritems():
                datatype_value = standard_dic.get(key_a, {}).get('datatype')
                if datatype_value == 'string':
                    string_gen = standard_dic_kv[key_a]
                    if string_gen == 'null':
                        standard_dic_kv[key_a] = '{}'.format(string_gen)
                    else:
                        standard_dic_kv[key_a] = '"{}"'.format(string_gen)
                elif datatype_value == 'enum' or datatype_value == 'mixed':
                    string_gen = standard_dic_kv[key_a]
                    if string_gen == 'null':
                        standard_dic_kv[key_a] = '{}'.format(string_gen)
                    else:
                        if string_gen.startswith('!'):
                            spilt_values = string_gen.split(' ', 1)
                            standard_dic_kv[key_a] = spilt_values[0] + ' ' + '\'{}\''.format(spilt_values[1])
                        else:
                            standard_dic_kv[key_a] = '\'{}\''.format(string_gen)
                elif datatype_value == 'int' or datatype_value == 'long' or datatype_value == 'float':
                    string_gen = standard_dic_kv[key_a]
                    if string_gen == 'null':
                        standard_dic_kv[key_a] = '{}'.format(string_gen)
                    else:
                        standard_dic_kv[key_a] = '\'{}\''.format(string_gen)

            for key_a, value_a in standard_dic_kv.iteritems():
                for key_b, value_b in dict_component_kv.iteritems():
                    if key_a == key_b:
                        dict_component_kv[key_a] = value_a

            new_dic = OrderedDict()

            for key, value in dict_component_kv.items():
                split_keys = key.split('#')
                b_key = split_keys[1].strip()
                b_value_key = split_keys[2] if len(split_keys) > 2 else ''
                if b_key not in new_dic:
                    new_dic[b_key] = OrderedDict()

                new_dic[b_key][b_value_key] = value

            # print("new_dic",new_dic)
            a = test_case_counter + cross_over_count + 1
            for key, value in new_dic.items():
                output_path = target_directory if 'request_' in key else target_mock_directory
                file_path = os.path.join(output_path, key + '.yaml')
                with open(file_path, 'a') as file:
                    for inner_key, inner_value in value.items():
                        if 'case_' in inner_key:
                            inner_key = 'case_' + str(a)
                            file.write('{}: {}\n'.format(inner_key, inner_value))
                        else:
                            file.write('{}: {}\n'.format(inner_key, inner_value))
                    file.write('\n')
        cross_over_count += 1
        # print("cross_over_count", cross_over_count)

        for key_1, key_2 in zip(list_1, list_2):  # method2 block
            base_key_1 = key_1.rsplit('_', 1)[0]
            base_key_2 = key_2.rsplit('_', 1)[0]

            dict_1 = crossover_lists_dict[key_1]
            dict_2 = crossover_lists_dict[key_2]

            key_dict_1 = dict_1.keys()
            key_dict_2 = dict_2.keys()

            merged_dict = OrderedDict()
            keys = list(set(dict_1.keys()) & set(dict_2.keys()))
            half_dict_index = len(keys) - 1 // 2
            index_key = 0
            for k in dict_1:
                if k in keys and k in key_dict_1:
                    v1 = dict_1[k]
                    v2 = dict_2[k]
                    if index_key <= half_dict_index:
                        merged_dict[k] = v1
                    else:
                        merged_dict[k] = v2
                elif k in keys and k not in key_dict_1:
                    v2 = dict_2[k]
                    merged_dict[k] = v2
                elif k not in keys:
                    merged_dict[k] = dict_1[k]
                index_key += 1

            new_key = '#' + base_key_1 + '#'
            crossover_dict = OrderedDict()
            crossover_dict[new_key] = merged_dict
            dict_component_kv = OrderedDict()
            for key in crossover_dict:
                for sub_key, value in crossover_dict[key].iteritems():
                    new_key = key + sub_key
                    dict_component_kv[new_key] = value

            standard_dic_kv = OrderedDict()
            for key_a, value_a in dict_component_kv.iteritems():
                type_value = standard_dic.get(key_a, {}).get('type')
                if type_value in ('all', 'range'):
                    datatype_value = standard_dic.get(key_a, {}).get('datatype')
                    if datatype_value == 'list':
                        list_value_dic = standard_dic.get(key_a, {}).get('extend')
                        extend_list_fields = list_value_dic['fields']
                        len_list_fields = len(extend_list_fields)
                        for i in range(1, len_list_fields):
                            key_a_add = '!' + str(i) + '!' + key_a
                            standard_dic_kv[key_a_add] = value_a
                    standard_dic_kv[key_a] = value_a
                elif type_value == 'skip':
                    datatype_value = standard_dic.get(key_a, {}).get('datatype')
                    if datatype_value == 'string' or datatype_value == 'int' or datatype_value == 'long' or datatype_value == 'float' or datatype_value == 'enum':
                        if len(value_a.split('!!')) == 2 or datatype_value == 'enum':
                            dict_component_kv[key_a] = value_a
                        else:
                            dict_component_kv[key_a] = '\'' + value_a + '\''

            standard_dic_kv = dict_Mutation(standard_dic_kv)

            for key_a, value_a in standard_dic_kv.iteritems():
                datatype_value = standard_dic.get(key_a, {}).get('datatype')
                if datatype_value == 'string':
                    string_gen = standard_dic_kv[key_a]
                    if string_gen == 'null':
                        standard_dic_kv[key_a] = '{}'.format(string_gen)
                    else:
                        standard_dic_kv[key_a] = '"{}"'.format(string_gen)
                elif datatype_value == 'enum' or datatype_value == 'mixed':
                    string_gen = standard_dic_kv[key_a]
                    if string_gen == 'null':
                        standard_dic_kv[key_a] = '{}'.format(string_gen)
                    else:
                        if string_gen.startswith('!'):
                            spilt_values = string_gen.split(' ', 1)
                            standard_dic_kv[key_a] = spilt_values[0] + ' ' + '\'{}\''.format(spilt_values[1])
                        else:
                            standard_dic_kv[key_a] = '\'{}\''.format(string_gen)
                elif datatype_value == 'int' or datatype_value == 'long' or datatype_value == 'float':
                    string_gen = standard_dic_kv[key_a]
                    if string_gen == 'null':
                        standard_dic_kv[key_a] = '{}'.format(string_gen)
                    else:
                        standard_dic_kv[key_a] = '\'{}\''.format(string_gen)

            for key_a, value_a in standard_dic_kv.iteritems():
                for key_b, value_b in dict_component_kv.iteritems():
                    if key_a == key_b:
                        dict_component_kv[key_a] = value_a

            new_dic = OrderedDict()

            for key, value in dict_component_kv.items():
                split_keys = key.split('#')
                b_key = split_keys[1].strip()
                b_value_key = split_keys[2] if len(split_keys) > 2 else ''
                if b_key not in new_dic:
                    new_dic[b_key] = OrderedDict()

                new_dic[b_key][b_value_key] = value

            # print("new_dic",new_dic)
            a = test_case_counter + cross_over_count + 1
            for key, value in new_dic.items():
                output_path = target_directory if 'request_' in key else target_mock_directory
                file_path = os.path.join(output_path, key + '.yaml')
                with open(file_path, 'a') as file:
                    for inner_key, inner_value in value.items():
                        if 'case_' in inner_key:
                            inner_key = 'case_' + str(a)
                            file.write('{}: {}\n'.format(inner_key, inner_value))
                        else:
                            file.write('{}: {}\n'.format(inner_key, inner_value))
                    file.write('\n')
        cross_over_count += 1
        # print("cross_over_count", cross_over_count)

    print("cross_over_count_total", cross_over_count)
    return cross_over_count


def cross_over_create_data_block(data_yaml_files, target_directory, cross_over_count):
    global test_case_counter
    print("testcase counter before crossover", test_case_counter)
    for source_file in data_yaml_files:
        file_name = source_file.split('/')[-1]
        target_file_path = os.path.join(target_directory, file_name)
        test_case_blocks = []

        with open(source_file, 'r') as s_file:
            test_case_blocks = s_file.read().split('- !!com.ipay.itest.common.model.TestData')

        with open(target_file_path, 'a') as t_file:
            for block in test_case_blocks[1:]:
                for i in range(cross_over_count):
                    test_case_counter += 1
                    new_block = '- !!com.ipay.itest.common.model.TestData' + block
                    new_block = re.sub(r'(dataId:\s+.+_DATA_)\S+', lambda x: "{}{}".format(x.group(1), test_case_counter), new_block)
                    new_block = re.sub(r'(logicId:\s+.+_LOGIC_)\S+', lambda x: "{}{}".format(x.group(1), test_case_counter), new_block)
                    new_block = re.sub(r'(_index):\s+\S+', lambda x: "{}: case_{}".format(x.group(1), test_case_counter),
                                       new_block)
                    t_file.write(new_block)
                break


def cross_over_create_logic_block(logic_yaml_files, target_directory, cross_over_count):
    global test_case_counter
    for source_file in logic_yaml_files:
        file_name = source_file.split('/')[-1]
        target_file_path = os.path.join(target_directory, file_name)
        test_case_blocks = []

        with open(source_file, 'r') as s_file:
            test_case_blocks = s_file.read().split('- !!com.ipay.itest.common.model.TestLogic')

        with open(target_file_path, 'a') as t_file:
            for block in test_case_blocks[1:]:
                for i in range(cross_over_count):
                    p = test_case_counter + i + 1
                    pattern1 = r"(ccmock:mock_facade\?targetName='[^']+'&index=')\S+(')"
                    pattern2 = r"(ccmock:mock_agent\?targetName='[^']+'&index=')\S+(')"
                    new_block = '- !!com.ipay.itest.common.model.TestLogic' + block
                    new_block = re.sub(r'(logicId:\s+.+_LOGIC_)\S+', lambda x: "{}{}".format(x.group(1), p), new_block)
                    new_block = re.sub(pattern1, r"\g<1>case_{}\g<2>".format(p), new_block)
                    new_block = re.sub(pattern2, r"\g<1>case_{}\g<2>".format(p), new_block)
                    t_file.write(new_block)
                break


def init_files(root_directory, template_yaml_path, root_mock_directory, template_mock_path):
    if not os.path.exists(template_yaml_path):
        os.makedirs(template_yaml_path)
    if not os.path.exists(template_mock_path):
        os.makedirs(template_mock_path)
    data_yaml_files = []
    request_yaml_files = []
    logic_yaml_files = []
    mock_files = []
    for root, dirs, files in os.walk(root_directory):
        for file in files:
            if file.endswith(
                    '.data.yaml') and 'request' not in file and 'response' not in file and 'Request' not in file and 'Response' not in file:
                data_yaml_files.append(os.path.join(root, file))
            if file.endswith('.yaml') and 'request' in file:
                request_yaml_files.append(os.path.join(root, file))
            if file.endswith(
                    '.logic.yaml') and 'request' not in file and 'response' not in file and 'Request' not in file and 'Response' not in file:
                logic_yaml_files.append(os.path.join(root, file))

    # data
    for source_file in data_yaml_files:
        file_name = source_file.split('/')[-1]
        target_file_path = os.path.join(template_yaml_path, file_name)
        test_case_blocks = []

        with open(source_file, 'r') as s_file:
            test_case_blocks = s_file.read().split('- !!com.ipay.itest.common.model.TestData')
        # print("test_case_blocks",test_case_blocks)
        length_data = len(test_case_blocks) - 1
        i = 1
        with open(target_file_path, 'a') as t_file:
            for block in test_case_blocks[1:]:
                new_block = '- !!com.ipay.itest.common.model.TestData' + block
                new_block = re.sub(r'(dataId:\s+.+_DATA_)\S+', lambda x: "{}{}".format(x.group(1), i), new_block)
                new_block = re.sub(r'(logicId:\s+.+_LOGIC_)\S+', lambda x: "{}{}".format(x.group(1), i), new_block)
                new_block = re.sub(r'(_index):\s+\S+', lambda x: "{}: case_{}".format(x.group(1), i), new_block)
                t_file.write(new_block + "\n")
                i += 1

    a = 0
    # request
    for request_file in request_yaml_files:
        file_name = request_file.split('/')[-1]
        target_file_path = os.path.join(template_yaml_path, file_name)
        a = 0
        with open(request_file, 'r') as r_s_file:
            for line in r_s_file:
                retained_fields = []
                pattern = re.compile(r'case_\d+\s*: !!')
                if pattern.search(line):
                    a += 1
                    str_a = str(a)
                    str_case = "case_" + str_a
                    split_idx = line.index(': !!com.')
                    retained_part = line[split_idx:].strip()
                    retained_fields.append(retained_part)
                    with open(target_file_path, 'a') as w_file:
                        w_file.write(str_case + retained_fields[0] + "\n")
                else:
                    with open(target_file_path, 'a') as w_file:
                        w_file.write(line)

    # seed
    template_seed_path = os.path.join(template_yaml_path, 'seeds.txt')
    with open(template_seed_path, 'w') as seed:
        content = '''1\n'''
        seed.write(content)
        for i in range(1, a + 1):
            content = '''{} + 1\n'''.format(i)
            seed.write(content)

    # logic
    for log_file in logic_yaml_files:
        file_name = log_file.split('/')[-1]
        target_file_path = os.path.join(template_yaml_path, file_name)
        test_case_blocks = []
        with open(log_file, 'r') as s_file:
            test_case_blocks = s_file.read().split('- !!com.ipay.itest.common.model.TestLogic')
        i = 1
        with open(target_file_path, 'a') as t_file:
            for block in test_case_blocks[1:]:
                pattern = r"(ccmock:mock_facade\?targetName='[^']+'&index=')\S+(')"
                new_block = '- !!com.ipay.itest.common.model.TestLogic' + block
                new_block = re.sub(r'(logicId:\s+.+_LOGIC_)\S+', lambda x: "{}{}".format(x.group(1), i), new_block)
                new_block = re.sub(pattern, r"\g<1>case_{}\g<2>".format(i), new_block)
                t_file.write(new_block + "\n")
                i += 1

    # mock
    for root, dirs, files in os.walk(root_mock_directory):
        for file in files:
            if file.endswith('.yaml'):
                mock_files.append(os.path.join(root, file))
    for mock_file in mock_files:
        file_name = mock_file.split('/')[-1]
        mock_file_path = os.path.join(template_mock_path, file_name)
        a = 0
        with open(mock_file, 'r') as m_file:
            for line in m_file:
                retained_fields = []
                if '!!com.ipay.itest.common.util.mockito.MockYaml' in line:
                    a += 1
                    str_a = str(a)
                    str_case = "case_" + str_a
                    split_idx = line.index(': !!com.')
                    retained_part = line[split_idx:].strip()
                    retained_fields.append(retained_part)
                    with open(mock_file_path, 'a') as w_file:
                        w_file.write(str_case + retained_fields[0] + "\n")
                else:
                    with open(mock_file_path, 'a') as w_file:
                        w_file.write(line)


def old_seed_mutation(seed_statement_path):
    seed_new = []
    with open(seed_statement_path, 'r') as seed_file:
        lines = seed_file.readlines()
    seed_array = [line.strip().split() for line in lines]
    # print("seed_statement_path",seed_array)
    for row in seed_array:
        if row[1] == "+" or row[1] == "++":
            seed_new.append(row)
    print("seed_new", seed_new)
    return seed_new


def old_seed_request_mock(seed_new):
    old_seed_count = 0
    pre_favour = 0
    num_g = 0
    for row in seed_new:
        index = row[0]
        statement = row[1]
        coverage = int(row[2])

        if statement == '+':
            # print("Request Statement is +")
            num_g = num_generation(coverage, Max_coverage)
        elif statement == '++':
            # print("Request Statement is ++")
            num_g = num_generation_favour(coverage, Max_coverage)
        elif statement == 'SF':
            # print("Data Statement is SF")
            num_g = 0
        elif statement == 'F':
            # print("Data Statement is F")
            num_g = 0
        elif statement == 'F+':
            # print("Data Statement is F+")
            num_g = num_generation(coverage, Max_coverage)
        elif statement == 'F++':
            # print("Data Statement is F++")
            num_g = num_generation_favour(coverage, Max_coverage)
        elif statement == 'N':
            # print("Request Statement is N")
            num_g = 0
        elif statement == 'YF':
            # print("Request Statement is YF")
            num_g = 0
        elif statement == 'IF':
            # print("Request Statement is IF")
            num_g = 0

        start_count = test_case_counter + 1 + pre_favour
        end_count = start_count + num_g

        for k in range(start_count, end_count):
            pattern_1 = re.compile(r'_' + index + '$')
            # print("old_seed_request_mock,pattern_1",pattern_1)
            list_1 = [key for key in crossover_lists_dict.keys() if pattern_1.search(key)]
            # print("old_seed_request_mock,list_1",list_1)
            for key_1 in list_1:  # method1 random
                # print("old_seed_request_mock,key_1",key_1)
                base_key_1 = key_1.rsplit('_', 1)[0]
                dict_1 = crossover_lists_dict[key_1]
                new_key = '#' + base_key_1 + '#'
                crossover_dict = OrderedDict()
                crossover_dict[new_key] = dict_1

                dict_component_kv = OrderedDict()
                for key in crossover_dict:
                    for sub_key, value in crossover_dict[key].iteritems():
                        new_key = key + sub_key
                        dict_component_kv[new_key] = value

                # print("old_seed_request_mock,dict_component_kv",dict_component_kv)

                standard_dic_kv = OrderedDict()
                for key_a, value_a in dict_component_kv.iteritems():
                    type_value = standard_dic.get(key_a, {}).get('type')
                    if type_value in ('all', 'range'):
                        datatype_value = standard_dic.get(key_a, {}).get('datatype')
                        if datatype_value == 'list':
                            list_value_dic = standard_dic.get(key_a, {}).get('extend')
                            extend_list_fields = list_value_dic['fields']
                            len_list_fields = len(extend_list_fields)
                            for i in range(1, len_list_fields):
                                key_a_add = '!' + str(i) + '!' + key_a
                                standard_dic_kv[key_a_add] = value_a
                        standard_dic_kv[key_a] = value_a
                    elif type_value == 'skip':
                        datatype_value = standard_dic.get(key_a, {}).get('datatype')
                        if datatype_value == 'string' or datatype_value == 'int' or datatype_value == 'long' or datatype_value == 'float' or datatype_value == 'enum':
                            if len(value_a.split('!!')) == 2 or datatype_value == 'enum':
                                dict_component_kv[key_a] = value_a
                            else:
                                dict_component_kv[key_a] = '\'' + value_a + '\''

                mutation_time = int(sample_geometric_times(MEAN_MUTATION_COUNT))
                for _ in range(mutation_time):
                    standard_dic_kv = dict_Mutation(standard_dic_kv)

                for key_a, value_a in standard_dic_kv.iteritems():
                    datatype_value = standard_dic.get(key_a, {}).get('datatype')
                    if datatype_value == 'string':
                        string_gen = standard_dic_kv[key_a]
                        if string_gen == 'null':
                            standard_dic_kv[key_a] = '{}'.format(string_gen)
                        else:
                            standard_dic_kv[key_a] = '"{}"'.format(string_gen)
                    elif datatype_value == 'enum' or datatype_value == 'mixed':
                        string_gen = standard_dic_kv[key_a]
                        if string_gen == 'null':
                            standard_dic_kv[key_a] = '{}'.format(string_gen)
                        else:
                            if string_gen.startswith('!'):
                                spilt_values = string_gen.split(' ', 1)
                                standard_dic_kv[key_a] = spilt_values[0] + ' ' + '\'{}\''.format(spilt_values[1])
                            else:
                                standard_dic_kv[key_a] = '\'{}\''.format(string_gen)
                    elif datatype_value == 'int' or datatype_value == 'long' or datatype_value == 'float':
                        string_gen = standard_dic_kv[key_a]
                        if string_gen == 'null':
                            standard_dic_kv[key_a] = '{}'.format(string_gen)
                        else:
                            standard_dic_kv[key_a] = '\'{}\''.format(string_gen)

                for key_a, value_a in standard_dic_kv.iteritems():
                    for key_b, value_b in dict_component_kv.iteritems():
                        if key_a == key_b:
                            dict_component_kv[key_a] = value_a

                new_dic = OrderedDict()

                for key, value in dict_component_kv.items():
                    split_keys = key.split('#')
                    b_key = split_keys[1].strip()
                    b_value_key = split_keys[2] if len(split_keys) > 2 else ''
                    if b_key not in new_dic:
                        new_dic[b_key] = OrderedDict()

                    new_dic[b_key][b_value_key] = value

                # print("new_dic",new_dic)
                for key, value in new_dic.items():
                    output_path = target_directory if 'request_' in key else target_mock_directory
                    file_path = os.path.join(output_path, key + '.yaml')
                    with open(file_path, 'a') as file:
                        for inner_key, inner_value in value.items():
                            if 'case_' in inner_key:
                                inner_key = 'case_' + str(k)
                                file.write('{}: {}\n'.format(inner_key, inner_value))
                            else:
                                file.write('{}: {}\n'.format(inner_key, inner_value))
                        file.write('\n')
            old_seed_count += 1
        pre_favour = pre_favour + num_g
    return old_seed_count


def fault_explore_create_request_mock(fault_explore_root_pool, fault_explore_root_dict, children_from_fault_explore, seed_array,
                                      request_yaml_files, mock_files,
                                      target_directory, target_mock_directory):
    global test_case_counter

    origin_test_case_counter = test_case_counter
    new_test_case_counter = origin_test_case_counter

    YF_flag = False
    fault_cases_from_last_round_index = []
    for row in seed_array:
        index = int(row[0])
        statement = row[1]
        if statement == 'F' or statement == 'F+' or statement == 'F++' or statement == 'SF':
            fault_cases_from_last_round_index.append(index)
        if statement == 'YF':
            YF_flag = True

    request_dict_of_fault_cases = OrderedDict()
    mock_dict_of_fault_cases = OrderedDict()
    for index in fault_cases_from_last_round_index:
        request_dict_of_fault_cases[index] = OrderedDict()
        mock_dict_of_fault_cases[index] = OrderedDict()

    for request_file in request_yaml_files:
        file_name = request_file.split('/')[-1]
        request_blocks = []
        with open(request_file, 'r') as s_file:
            content = s_file.read()
            request_blocks = re.findall(r'(case_\d+:.*?(?=\ncase_\d+:|\Z))', content, re.DOTALL | re.MULTILINE)
        for block in request_blocks:
            match = re.match(r'^case_(\d+):', block.strip())
            if match:
                block_index = int(match.group(1))
                if block_index in fault_cases_from_last_round_index:
                    block_dic = parse_block_to_dict(block)
                    request_file_name = file_name.split('.')[0]
                    request_dict_of_fault_cases[block_index][request_file_name] = block_dic

    for mock_file in mock_files:
        file_name = mock_file.split('/')[-1]
        mock_blocks = []
        with open(mock_file, 'r') as s_file:
            content = s_file.read()
            mock_blocks = re.findall(r'(case_\d+:.*?(?=\ncase_\d+:|\Z))', content, re.DOTALL | re.MULTILINE)
        for block in mock_blocks:
            match = re.match(r'^case_(\d+):', block.strip())
            if match:
                block_index = int(match.group(1))
                if block_index in fault_cases_from_last_round_index:
                    mock_dic = parse_block_to_dict(block)
                    mock_file_name = file_name.split('.')[0]
                    mock_dict_of_fault_cases[block_index][mock_file_name] = mock_dic

    if YF_flag == True:
        for index in children_from_fault_explore:
            # reset update flag
            for case_state_dic in fault_explore_root_pool:
                for explore_state_dic in case_state_dic["explore"]:
                    if index in explore_state_dic["lastChildren"]:
                        children_from_fault_explore.remove(index)
                        explore_state_dic["updated"] = True

    # update states from last fault cases
    for index in fault_cases_from_last_round_index:
        if index in children_from_fault_explore:
            # this is a child case of a root case in the pool, merge the states
            for case_state_dic in fault_explore_root_pool:
                for explore_state_dic in case_state_dic["explore"]:
                    if index in explore_state_dic["lastChildren"]:
                        # get infos
                        key = explore_state_dic["key"]
                        file_name = key.split('#')[1].strip()
                        field_name = key.split('#')[2]
                        block_dic = OrderedDict()
                        if file_name in request_dict_of_fault_cases[index]:
                            block_dic = request_dict_of_fault_cases[index][file_name]
                        if file_name in mock_dict_of_fault_cases[index]:
                            block_dic = mock_dict_of_fault_cases[index][file_name]
                        # update states and merge the boundary
                        explore_state_dic["updated"] = True
                        explore_state_dic["allChildren"].append(index)
                        explore_state_dic["lastChildren"].remove(index)
                        children_from_fault_explore.remove(index)
                        value_str = block_dic[field_name]
                        datatype = explore_state_dic["datatype"]
                        if value_str != 'null' and value_str != '':
                            if datatype == 'int' or datatype == 'long':
                                current_value = int(value_str)
                                if current_value < explore_state_dic["range"][0]:
                                    explore_state_dic["range"][0] = current_value
                                elif current_value > explore_state_dic["range"][1]:
                                    explore_state_dic["range"][1] = current_value
                            elif datatype == 'float':
                                current_value = float(value_str)
                                if current_value < explore_state_dic["range"][0]:
                                    explore_state_dic["range"][0] = current_value
                                elif current_value > explore_state_dic["range"][1]:
                                    explore_state_dic["range"][1] = current_value
                            elif datatype == 'enum':
                                enum_list = standard_dic.get(key, {}).get("extend")
                                filter_value = value_str.split(' ', 1)[-1]
                                current_index = enum_list.index(filter_value)
                                left_index = enum_list.index(explore_state_dic["range"][0])
                                right_index = enum_list.index(explore_state_dic["range"][1])
                                if current_index >= 0 and current_index < left_index:
                                    explore_state_dic["range"][0] = filter_value
                                elif current_index <= len(enum_list) - 1 and current_index > right_index:
                                    explore_state_dic["range"][1] = filter_value
        else:
            # this is a root fault case from normal cases
            fault_explore_root_pool.append(OrderedDict([
                ("root", index), ("explore", [])
            ]))
            fault_explore_root_dict[index] = OrderedDict()
            for filename, blockdic in request_dict_of_fault_cases[index].items():
                fault_explore_root_dict[index][filename] = blockdic
                for field_name, value in blockdic.items():
                    if value == 'null' or value == '':
                        continue
                    key = '#' + filename + '#' + field_name
                    if standard_dic.get(key, {}).get("type") == 'skip':
                        continue
                    if standard_dic.get(key, {}).get("datatype") == 'int' or standard_dic.get(key, {}).get(
                            "datatype") == 'long' or standard_dic.get(key, {}).get("datatype") == 'float' or standard_dic.get(key, {}).get(
                        "datatype") == 'enum':
                        current_value = 0
                        if standard_dic.get(key, {}).get("datatype") == 'int' or standard_dic.get(key, {}).get("datatype") == 'long':
                            current_value = int(value)
                        elif standard_dic.get(key, {}).get("datatype") == 'float':
                            current_value = float(value)
                        elif standard_dic.get(key, {}).get("datatype") == 'enum':
                            current_value = value.split(' ', 1)[-1]
                        for case_state_dic in fault_explore_root_pool:
                            if case_state_dic["root"] == index:
                                case_state_dic["explore"].append(OrderedDict([
                                    ("key", key),
                                    ("name", standard_dic.get(key, {}).get("name")),
                                    ("type", standard_dic.get(key, {}).get("type")),
                                    ("datatype", standard_dic.get(key, {}).get("datatype")),
                                    ("range", [current_value, current_value]),
                                    ("lastChildren", []),
                                    ("allChildren", []),
                                    ("updated", True),
                                    ("exploreFinished", False)
                                ]))
                                break
            for filename, blockdic in mock_dict_of_fault_cases[index].items():
                fault_explore_root_dict[index][filename] = blockdic
                for field_name, value in blockdic.items():
                    if value == 'null' or value == '':
                        continue
                    key = '#' + filename + '#' + field_name
                    if standard_dic.get(key, {}).get("type") == 'skip':
                        continue
                    if standard_dic.get(key, {}).get("datatype") == 'int' or standard_dic.get(key, {}).get(
                            "datatype") == 'long' or standard_dic.get(key, {}).get("datatype") == 'float' or standard_dic.get(key, {}).get(
                        "datatype") == 'enum':
                        current_value = 0
                        if standard_dic.get(key, {}).get("datatype") == 'int' or standard_dic.get(key, {}).get("datatype") == 'long':
                            current_value = int(value)
                        elif standard_dic.get(key, {}).get("datatype") == 'float':
                            current_value = float(value)
                        elif standard_dic.get(key, {}).get("datatype") == 'enum':
                            current_value = value.split(' ', 1)[-1]
                        explore_list = []
                        for case_state_dic in fault_explore_root_pool:
                            if case_state_dic["root"] == index:
                                case_state_dic["explore"].append(OrderedDict([
                                    ("key", key),
                                    ("name", standard_dic.get(key, {}).get("name")),
                                    ("type", standard_dic.get(key, {}).get("type")),
                                    ("datatype", standard_dic.get(key, {}).get("datatype")),
                                    ("range", [current_value, current_value]),
                                    ("lastChildren", []),
                                    ("allChildren", []),
                                    ("updated", True),
                                    ("exploreFinished", False)
                                ]))
                                break

    # fault boundary exploration here
    for case_state_dic in fault_explore_root_pool:
        index = case_state_dic["root"]
        for explore_dic in case_state_dic["explore"]:
            if explore_dic["updated"] == True:
                explore_dic["updated"] = False
                key = explore_dic["key"]
                file_name = key.split('#')[1].strip()
                field_name = key.split('#')[2]
                datatype = explore_dic["datatype"]
                new_left = None
                new_right = None
                if datatype == 'int' or datatype == 'long':
                    last_left = explore_dic["range"][0]
                    last_right = explore_dic["range"][1]
                    if last_left - MEAN_INT_STEP >= - 2 ** 31:
                        new_left = str(last_left - MEAN_INT_STEP)
                    if last_right + MEAN_INT_STEP <= 2 ** 31 - 1:
                        new_right = last_right + MEAN_INT_STEP
                elif datatype == 'float':
                    last_left = explore_dic["range"][0]
                    last_right = explore_dic["range"][1]
                    if last_left - MEAN_FLOAT_STEP >= - 2 ** 31:
                        new_left = last_left - MEAN_FLOAT_STEP
                    if last_right + MEAN_FLOAT_STEP <= 2 ** 31 - 1:
                        new_right = last_right + MEAN_FLOAT_STEP
                elif datatype == 'enum':
                    last_left = explore_dic["range"][0]
                    last_right = explore_dic["range"][1]
                    if last_left != 'null' and last_right != 'null':
                        last_left_index = standard_dic.get(key, {}).get("extend").index(last_left)
                        last_right_index = standard_dic.get(key, {}).get("extend").index(last_right)
                        if last_left_index - 1 >= 0:
                            new_left = standard_dic.get(key, {}).get("extend")[last_left_index - 1]
                        if last_right_index + 1 <= len(standard_dic.get(key, {}).get("extend")) - 1:
                            new_right = standard_dic.get(key, {}).get("extend")[last_right_index + 1]
                if new_left != None:
                    fault_case_dict = copy.deepcopy(fault_explore_root_dict[index])
                    find = False
                    for filename, dicts in fault_case_dict.items():
                        if find == True:
                            break
                        for field_name, value in dicts.items():
                            targetkey = '#' + filename + '#' + field_name
                            if key == targetkey:
                                if dicts[field_name].startswith('!'):
                                    tag = dicts[field_name].split(' ', 1)[0]
                                    dicts[field_name] = tag + ' ' + new_left
                                else:
                                    dicts[field_name] = new_left
                                find = True
                                break
                    new_test_case_counter += 1
                    for filename, dicts in fault_case_dict.items():
                        output_path = target_directory if 'request_' in filename else target_mock_directory
                        file_path = os.path.join(output_path, filename + '.yaml')
                        for field_name, value in dicts.items():
                            targetkey = '#' + filename + '#' + field_name
                            datatype_value = standard_dic.get(targetkey, {}).get('datatype')
                            if datatype_value == 'string':
                                string_gen = dicts[field_name]
                                if string_gen == 'null':
                                    dicts[field_name] = '{}'.format(string_gen)
                                else:
                                    dicts[field_name] = '"{}"'.format(string_gen)
                            elif datatype_value == 'enum' or datatype_value == 'mixed':
                                string_gen = dicts[field_name]
                                if string_gen == 'null':
                                    dicts[field_name] = '{}'.format(string_gen)
                                else:
                                    if string_gen.startswith('!'):
                                        spilt_values = string_gen.split(' ', 1)
                                        dicts[field_name] = spilt_values[0] + ' ' + '\'{}\''.format(spilt_values[1])
                                    else:
                                        dicts[field_name] = '\'{}\''.format(string_gen)
                            elif datatype_value == 'int' or datatype_value == 'long' or datatype_value == 'float':
                                string_gen = dicts[field_name]
                                if string_gen == 'null':
                                    dicts[field_name] = '{}'.format(string_gen)
                                else:
                                    dicts[field_name] = '\'{}\''.format(string_gen)
                        with open(file_path, 'a') as file:
                            for inner_key, inner_value in dicts.items():
                                if 'case_' in inner_key:
                                    inner_key = 'case_' + str(new_test_case_counter)
                                    file.write('{}: {}\n'.format(inner_key, inner_value))
                                else:
                                    file.write('{}: {}\n'.format(inner_key, inner_value))
                            file.write('\n')
                    children_from_fault_explore.append(new_test_case_counter)
                    explore_dic["lastChildren"].append(new_test_case_counter)

                if new_right != None:
                    fault_case_dict = copy.deepcopy(fault_explore_root_dict[index])
                    find = False
                    for filename, dicts in fault_case_dict.items():
                        if find == True:
                            break
                        for field_name, value in dicts.items():
                            targetkey = '#' + filename + '#' + field_name
                            if key == targetkey:
                                if dicts[field_name].startswith('!'):
                                    tag = dicts[field_name].split(' ', 1)[0]
                                    dicts[field_name] = tag + ' ' + new_right
                                else:
                                    dicts[field_name] = new_right
                                find = True
                                break
                    new_test_case_counter += 1
                    for filename, dicts in fault_case_dict.items():
                        output_path = target_directory if 'request_' in filename else target_mock_directory
                        file_path = os.path.join(output_path, filename + '.yaml')
                        for field_name, value in dicts.items():
                            targetkey = '#' + filename + '#' + field_name
                            datatype_value = standard_dic.get(targetkey, {}).get('datatype')
                            if datatype_value == 'string':
                                string_gen = dicts[field_name]
                                if string_gen == 'null':
                                    dicts[field_name] = '{}'.format(string_gen)
                                else:
                                    dicts[field_name] = '"{}"'.format(string_gen)
                            elif datatype_value == 'enum' or datatype_value == 'mixed':
                                string_gen = dicts[field_name]
                                if string_gen == 'null':
                                    dicts[field_name] = '{}'.format(string_gen)
                                else:
                                    if string_gen.startswith('!'):
                                        spilt_values = string_gen.split(' ', 1)
                                        dicts[field_name] = spilt_values[0] + ' ' + '\'{}\''.format(spilt_values[1])
                                    else:
                                        dicts[field_name] = '\'{}\''.format(string_gen)
                            elif datatype_value == 'int' or datatype_value == 'long' or datatype_value == 'float':
                                string_gen = dicts[field_name]
                                if string_gen == 'null':
                                    dicts[field_name] = '{}'.format(string_gen)
                                else:
                                    dicts[field_name] = '\'{}\''.format(string_gen)
                        with open(file_path, 'a') as file:
                            for inner_key, inner_value in dicts.items():
                                if 'case_' in inner_key:
                                    inner_key = 'case_' + str(new_test_case_counter)

                                    file.write('{}: {}\n'.format(inner_key, inner_value))
                                else:
                                    file.write('{}: {}\n'.format(inner_key, inner_value))
                            file.write('\n')
                    children_from_fault_explore.append(new_test_case_counter)
                    explore_dic["lastChildren"].append(new_test_case_counter)
            else:
                explore_dic["exploreFinished"] = True

    with open(fuzzing_result_path + '/fault_root_case_pool.json', 'w') as file:
        json.dump(fault_explore_root_pool, file, indent=4)

    print("fault_cases_from_last_round_index:", fault_cases_from_last_round_index)
    print("children_from_fault_explore:", children_from_fault_explore)
    root_fault_cases = []
    for case_state_dic in fault_explore_root_pool:
        root_fault_cases.append(case_state_dic["root"])
    print("root_fault_cases:", root_fault_cases)

    return new_test_case_counter - origin_test_case_counter


def process_all_rounds(root_directory, fuzzing_result_path, target_directory, num_rounds, template_yaml_path, root_mock_directory,
                       template_mock_path):
    global test_case_counter
    global Pre_Max_coverage
    global Max_coverage
    break_count = 0
    for i in range(num_rounds + 1):
        print("Start round {}".format(i))
        if i == 0:
            command = "python LogFilter.py 0"
            os.system(command)
            print("Finished round {}".format(i))
            source_directory = template_yaml_path
            init_files(root_directory, template_yaml_path, root_mock_directory, template_mock_path)
            print('init successed')
            # continue
        else:
            print("Starting iTest in round {}".format(i))
            command = "(cd .. && " + test_command + " 2>&1)" + " | stdbuf -oL python LogFilter.py {}".format(i)
            process = subprocess.Popen(command, stdout=subprocess.PIPE, universal_newlines=True, shell=True)
            for line in iter(process.stdout.readline, ''):
                print(line, end='')
            process.stdout.close()
            process.wait()
            log_clear1 = "rm -rf {}".format(clear_log_dir1)
            process1 = subprocess.Popen(log_clear1, stdout=subprocess.PIPE, universal_newlines=True, shell=True)
            process1.stdout.close()
            process1.wait()
            log_clear2 = "rm -rf {}".format(clear_log_dir2)
            process2 = subprocess.Popen(log_clear2, stdout=subprocess.PIPE, universal_newlines=True, shell=True)
            process2.stdout.close()
            process2.wait()
            print("Finished round {}".format(i))
            source_directory = os.path.join(fuzzing_result_path, 'round_{}'.format(i))
            back_up = os.path.join(fuzzing_result_path, 'round_{}/template_yaml'.format(i))
            # if os.path.exists(back_up): shutil.rmtree(back_up)
            shutil.copytree(template_yaml_path, back_up)
        # source_directory = os.path.join(fuzzing_result_path, 'round_{}'.format(i))

        if Pre_Max_coverage < Max_coverage:
            Pre_Max_coverage = Max_coverage
            break_count = 1
        elif Pre_Max_coverage >= Max_coverage:
            break_count += 1
            if break_count >= 3:
                print("Coverage is basically stable, no need to continue")
                break

        old_seeds_count = []
        if i != 0 and i != 1:
            seed_new = old_seed_mutation(seed_statement_path)
            old_seeds_count = seed_new

        data_yaml_files, request_yaml_files, seed_array, logic_yaml_files, mock_files = find_yaml_files(seed_statement_path,
                                                                                                        source_directory,
                                                                                                        template_yaml_path,
                                                                                                        template_mock_path)

        print("data_yaml_files", data_yaml_files)
        print("request_yaml_files", request_yaml_files)
        print("logic_yaml_files", logic_yaml_files)

        pre_test_case_counter, new_seeds_count = create_data_block(data_yaml_files, data_seed_directory_path, target_directory, seed_array,
                                                                   Max_coverage)
        print("Data_complete")
        create_logic_block(logic_yaml_files, target_directory, seed_array, pre_test_case_counter, Max_coverage)
        print("Logic_complete")

        create_request_mock_block(request_yaml_files, request_seed_directory_path, target_directory, mock_files, target_mock_directory,
                                  seed_array, pre_test_case_counter, Max_coverage)
        print("Request_Mock_complete")

        children_from_new_seeds = test_case_counter - pre_test_case_counter

        old_seed_count = 0
        if i != 0 and i != 1:
            # print("testcase counter before old seed", test_case_counter)
            old_seed_count = old_seed_request_mock(seed_new)
            # print("logic_yaml_files",logic_yaml_files)
            cross_over_create_logic_block(logic_yaml_files, target_directory, old_seed_count)
            cross_over_create_data_block(data_yaml_files, target_directory, old_seed_count)
            # print("old_seed_count", old_seed_count)

        if i == 0:
            del crossover_seed_pool[:]
            print("crossover_seed_pool++", crossover_seed_pool)
            crossover_lists_dict.clear()
            print("crossover_lists_dict++", crossover_lists_dict)
            if os.path.exists(data_seed_directory_path):
                os.remove(data_seed_directory_path)
            if os.path.exists(request_seed_directory_path):
                os.remove(request_seed_directory_path)
            if os.path.exists(seed_statement_path):
                os.remove(seed_statement_path)
        cross_over_count = 0
        if i != 0:
            # print (crossover_seed_pool)
            cross_over_count = crossover_create_request_mock(crossover_seed_pool, crossover_lists_dict, target_directory,
                                                             target_mock_directory)

            cross_over_create_logic_block(logic_yaml_files, target_directory, cross_over_count)
            cross_over_create_data_block(data_yaml_files, target_directory, cross_over_count)
            print("Crossover_complete")
        fault_explore_count = 0
        #if i != 0:
        #    fault_explore_count = fault_explore_create_request_mock(fault_explore_root_pool, fault_explore_root_dict,
        #                                                            children_from_fault_explore, seed_array,
        #                                                            request_yaml_files, mock_files, target_directory, target_mock_directory)
        #    cross_over_create_logic_block(logic_yaml_files, target_directory, fault_explore_count)
        #    cross_over_create_data_block(data_yaml_files, target_directory, fault_explore_count)

        data_files_to_copy = [os.path.join(target_directory, os.path.basename(file)) for file in data_yaml_files]
        request_files_to_copy = [os.path.join(target_directory, os.path.basename(file)) for file in request_yaml_files]
        logic_files_to_copy = [os.path.join(target_directory, os.path.basename(file)) for file in logic_yaml_files]
        mock_files_to_copy = [os.path.join(target_mock_directory, os.path.basename(file)) for file in mock_files]

        copy_files(data_files_to_copy, template_yaml_path)
        copy_files(request_files_to_copy, template_yaml_path)
        copy_files(logic_files_to_copy, template_yaml_path)
        copy_files(mock_files_to_copy, template_mock_path)

        copy_files(data_files_to_copy, test_path)
        copy_files(request_files_to_copy, test_path)
        copy_files(logic_files_to_copy, test_path)
        copy_files(mock_files_to_copy, test_mock_path)

        # back_up = os.path.join(fuzzing_result_path, 'round_{}/template_yaml'.format(i + 1))
        # shutil.copytree(template_yaml_path, back_up)
        # print("testcount", test_case_counter)
        print("old seeds count:", len(old_seeds_count), "new seeds count:", new_seeds_count)
        print("children from old seeds:", old_seed_count, ", children from new seeds:", children_from_new_seeds,
              ", children from crossover:", cross_over_count, ", children from explore fault boundary:", fault_explore_count, ", total:",
              test_case_counter - pre_test_case_counter)


def copy_fuzzing_results(src_dir, dest_dir_base):
    dest_dir = os.path.join(dest_dir_base, "round_1")

    round_num = 1
    while os.path.exists(dest_dir):
        round_num += 1
        dest_dir = os.path.join(dest_dir_base, "round_{}".format(round_num))

    os.makedirs(dest_dir)

    for item in os.listdir(src_dir):
        src_item = os.path.join(src_dir, item)
        dest_item = os.path.join(dest_dir, item)

        if os.path.isdir(src_item):
            shutil.copytree(src_item, dest_item)
        else:
            shutil.copy2(src_item, dest_item)


def main():
    rounds = fuzzing_rounds  # Set the number of rounds you want
    process_all_rounds(root_directory, fuzzing_result_path, target_directory, rounds, template_yaml_path, root_mock_directory,
                       template_mock_path)

    source_directory = '/home/admin/lq01145628/internal_release/iexpprod/fuzzing/fuzzing_results'
    destination_base_directory = '/home/admin/lq01145628/internal_release/iexpprod/fuzzing/fuzzing_results_new'
    copy_fuzzing_results(source_directory, destination_base_directory)


    print("Total test cases generated: {}".format(test_case_counter))

# seed_new=old_seed_mutation(seed_statement_path)
# print(mutation.request)
# init_files(root_directory, template_yaml_path)


if __name__ == '__main__':
    main()
