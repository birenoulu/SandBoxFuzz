# -*- coding: utf-8 -*-
from __future__ import print_function

import datetime
import json
import os
import re
import shutil
import sys


class LogFilter():
    def __init__(self, round):
        self.logics = [
            "COD_AUTHCODE_AND_SIGN_CONTRACT"
        ]
        self.round = round
        self.seedID = {logic: [] for logic in self.logics}
        self.profile_columns = ["{:>5}".format(s) for s in ["round", "id", "mth", "tmth", "cov", "uni", "tcov", "tuni", "tag"]]

        # coverage info
        self.total_coverage = {logic: [0] * 65536 for logic in self.logics}
        self.max_coverage = {logic: 0 for logic in self.logics}
        self.real_methods_unique = {logic: set() for logic in self.logics}
        self.real_blocks_unique = {logic: set() for logic in self.logics}
        self.HOB_cache = [0] * 1024
        for i in range(len(self.HOB_cache)):
            self.HOB_cache[i] = self.computeHob(i)

        # plot info
        self.plot_data = []

        # boot log
        self.boot_log = []

        # case state
        self.current_logic = None
        self.current_caseID = None
        self.current_failed = False
        self.current_systemFailed = False
        self.yaml_parse_failed = False
        self.current_failed_reason = None
        self.run_log = []
        self.run_coverage = [0] * 65536
        self.run_time = 0.0
        self.itest_prepare = 0.0
        self.itest_yaml_process = 0.0
        self.itest_execute = 0.0
        self.itest_execute_ccbean = 0.0
        self.itest_check = 0.0
        self.itest_clear = 0.0
        self.real_methods_unique_traversal = set()
        self.real_blocks_unique_traversal = set()

        # case start flags
        self.sofa_boot_start = False
        self.sofa_boot_done = False
        self.itest_test_case_start_flag = False
        self.itest_test_case_exec_start_flag = False

        # control flags
        self.LPS_in_log = False
        self.save_logs_label = ['F', 'F+', 'F++', 'YF', 'SF']
        self.save_logs_period = 10000

        # profile stats
        self.boot_start_time = None
        self.boot_up_time = 0
        self.cases_in_this_round = {logic: 0 for logic in self.logics}
        self.valid_case_time_num = {logic: 0 for logic in self.logics}
        self.valid_case_avg_time = {logic: 0.0 for logic in self.logics}
        self.itest_prepare_avg_time = {logic: 0.0 for logic in self.logics}
        self.itest_yaml_process_avg_time = {logic: 0.0 for logic in self.logics}
        self.itest_execute_avg_time = {logic: 0.0 for logic in self.logics}
        self.itest_execute_ccbean_avg_time = {logic: 0.0 for logic in self.logics}
        self.itest_check_avg_time = {logic: 0.0 for logic in self.logics}
        self.itest_clear_avg_time = {logic: 0.0 for logic in self.logics}
        # +: coverage bit update
        # ++: new coverage (favored)
        # SF: system exception
        # F: component exception with no change to coverage
        # F+: component exception with coverage bit update
        # F++: component exception with new coverage (favored)
        # N: no change to coverage
        # YF: yaml failed
        # IF: exception occured but considered ignored
        self.cases_compose = {logic: {"+": 0, "++": 0, "N": 0, "SF": 0, "F": 0, "F+": 0, "F++": 0, "YF": 0, "IF": 0} for logic in
                              self.logics}

        # re patterns
        # value in these patterns should be replaced with actual output of the test framework
        self.sofa_boot_start_pattern = re.compile(r'^(\d{2}:\d{2}:\d{2})[:,.]\d{3}.*SOFABOOT_START')
        self.sofa_boot_done_pattern = re.compile(r'^(\d{2}:\d{2}:\d{2})[:,.]\d{3}.*SOFABOOT_DONE')
        self.itest_test_case_start_pattern = re.compile(r'\[NM_H_(\w+)_DATA_(\d+)\].*\[test begin\]')
        self.itest_test_case_end_pattern = re.compile(r'\[NM_H_(\w+)_DATA_(\d+)\].*\[upload log\]')
        self.itest_test_case_exec_start_pattern = re.compile(r'\[NM_H_(\w+)_DATA_(\d+)\].*result=ccbean')
        self.itest_test_case_exec_end_pattern = re.compile(r'\[NM_H_(\w+)_DATA_(\d+)\].*\[(check|clear) phase\]')
        self.itest_case_time_pattern = re.compile(r'\[.*?\]\[.*?\].*?\[(\d+(?:,\d+)?)ms')
        self.itest_prepare_time_pattern = re.compile(r'.*?\[prepare phase\].*?\[(\d+(?:,\d+)?)ms')
        self.itest_yaml_process_time_pattern = re.compile(r'.*?\[YAML\].*?\[(\d+(?:,\d+)?)ms')
        self.itest_execute_time_pattern = re.compile(r'.*?\[execute phase\].*?\[(\d+(?:,\d+)?)ms')
        self.itest_execute_ccbean_time_pattern = re.compile(r'.*?\[ccil\] result=ccbean.*?\[(\d+(?:,\d+)?)ms')
        self.itest_check_time_pattern = re.compile(r'.*?\[check phase\].*?\[(\d+(?:,\d+)?)ms')
        self.itest_clear_time_pattern = re.compile(r'.*?\[clear phase\].*?\[(\d+(?:,\d+)?)ms')
        self.LPS_pattern = re.compile(r'LPS: <(.*?)>, hashCode: (\d+), block: (\d+)')
        self.iTest_failed_pattern = re.compile(r'\[fail\]')
        self.component_exception_pattern = re.compile(r'^([^ :\s]+)(?::)?')

        # exception patterns
        self.yaml_parse_exception_pattern = re.compile(r'yaml parse exception')
        self.runtime_exception_list = [
            "java.lang.RuntimeException",
            "java.lang.ArithmeticException",
            "java.lang.ArrayIndexOutOfBoundsException",
            "java.lang.ArrayStoreException",
            "java.lang.ClassCastException",
            "java.lang.IllegalArgumentException",
            "java.lang.IllegalMonitorStateException",
            "java.lang.IllegalStateException",
            "java.lang.IllegalThreadStateException",
            "java.lang.IndexOutOfBoundsException",
            "java.lang.NegativeArraySizeException",
            "java.lang.NullPointerException",
            "java.lang.NumberFormatException",
            "java.lang.SecurityException",
            "java.lang.StringIndexOutOfBoundsException",
            "java.lang.TypeNotPresentException",
            "java.lang.UnsupportedOperationException"
        ]
        self.error_list = [
            "java.lang.Error",
            "java.lang.AssertionError",
            "java.lang.ExceptionInInitializerError",
            "java.lang.InstantiationError",
            "java.lang.InternalError",
            "java.lang.IOError",
            "java.lang.LinkageError",
            "java.lang.OutOfMemoryError",
            "java.lang.StackOverflowError",
            "java.lang.UnknownError",
            "java.lang.VirtualMachineError"
        ]
        # exceptions and errors to ignore
        self.ues_to_ignore = [
        ]

    def computeHob(self, num):
        if num == 0:
            return 0
        ret = 1
        num >>= 1
        while num != 0:
            ret <<= 1
            num >>= 1
        return ret

    def hob(self, num):
        if num < len(self.HOB_cache):
            return self.HOB_cache[num]
        else:
            return self.computeHob(num)

    def updateBits(self, old, new):
        changed = False
        if (any(num > 0 for num in new)):
            for i in range(len(old)):
                before = old[i]
                after = before | self.hob(new[i])
                if before != after:
                    changed = True
                    old[i] = after
        return changed

    def get_non_zero_count(self, coverage):
        return sum(1 for num in coverage if num > 0)

    def fuzzing_setup(self):
        # round 0
        # create dirs fuzzing_results
        if os.path.exists('fuzzing_results'):
            shutil.rmtree('fuzzing_results')
        os.mkdir('fuzzing_results')
        os.mkdir('fuzzing_results/bootLog')

        for logic in self.logics:
            # create dirs for each logic, and create files for coverage and failed
            os.mkdir('fuzzing_results/' + logic)
            with open('fuzzing_results/' + logic + '/coverage.txt', 'w') as file:
                pass
            with open('fuzzing_results/' + logic + '/uniqueMethods.txt', 'w') as file:
                json.dump(list(set([])), file)
            with open('fuzzing_results/' + logic + '/uniqueBlocks.txt', 'w') as file:
                json.dump(list(set([])), file)
            with open('fuzzing_results/' + logic + '/failed.txt', 'w') as file:
                pass
            with open('fuzzing_results/' + logic + '/plot.txt', 'w') as file:
                file.write(' '.join(self.profile_columns) + '\n')
                pass
            with open('fuzzing_results/' + logic + '/profile.txt', 'w') as file:
                pass

    def fuzzing_prepare(self):
        # round 1 and more
        for logic in self.logics:
            # read in total coverage
            with open('fuzzing_results/' + logic + '/coverage.txt', 'r') as file:
                for line in file:
                    index, value = line.strip().split()
                    index = int(index)
                    value = int(value)
                    self.total_coverage[logic][index] = value
            # read in total unique methods
            with open('fuzzing_results/' + logic + '/uniqueMethods.txt', 'r') as file:
                self.real_methods_unique[logic] = set(json.load(file))
            # read in total unique blocks
            with open('fuzzing_results/' + logic + '/uniqueBlocks.txt', 'r') as file:
                self.real_blocks_unique[logic] = set(json.load(file))
            # create dirs for new round
            if os.path.exists('fuzzing_results/' + logic + '/round_' + str(self.round)):
                shutil.rmtree('fuzzing_results/' + logic + '/round_' + str(self.round))
            os.mkdir('fuzzing_results/' + logic + '/round_' + str(self.round))
            # read in max coverage
            if self.round == 1:
                self.max_coverage[logic] = 0
            else:
                with open('fuzzing_results/' + logic + '/round_' + str(self.round - 1) + '/seeds.txt', 'r') as file:
                    max_cover_so_far = file.readline().strip()
                    self.max_coverage[logic] = int(max_cover_so_far)
        os.mkdir('fuzzing_results/bootLog/round_' + str(self.round))
        return

    def fuzzing_done(self):
        for logic in self.logics:
            with open('fuzzing_results/' + logic + '/coverage.txt', 'w') as file:
                for i in range(len(self.total_coverage[logic])):
                    if self.total_coverage[logic][i] > 0:
                        file.write(str(i) + ' ' + str(self.total_coverage[logic][i]) + '\n')
            with open('fuzzing_results/' + logic + '/uniqueMethods.txt', 'w') as file:
                json.dump(list(self.real_methods_unique[logic]), file)
            with open('fuzzing_results/' + logic + '/uniqueBlocks.txt', 'w') as file:
                json.dump(list(self.real_blocks_unique[logic]), file)
            with open('fuzzing_results/' + logic + '/round_' + str(self.round) + '/seeds.txt', 'w') as file:
                file.write(str(self.max_coverage[logic]) + '\n')
                for seed_and_reason in self.seedID[logic]:
                    file.write(' '.join(str(item) for item in seed_and_reason) + '\n')
            with open('fuzzing_results/' + logic + '/profile.txt', 'a') as file:
                file.write("round " + str(self.round) + ":\n")
                file.write("total cases: " + str(self.cases_in_this_round[logic]) + ", total coverage: " + str(
                    self.max_coverage[logic]) + ", case all time: " + str(
                    self.valid_case_avg_time[logic] * self.valid_case_time_num[logic]) + ", case avg time: " + str(
                    self.valid_case_avg_time[logic]) + ", boot up time: " + str(self.boot_up_time) + ", total time:" + str(
                    self.valid_case_avg_time[logic] * self.valid_case_time_num[logic] + self.boot_up_time) + "\n")
                # itest additional info here
                print(self.itest_prepare_avg_time[logic])
                file.write("prepare: " + str(self.itest_prepare_avg_time[logic]) + ", "
                           + "yaml process: " + str(self.itest_yaml_process_avg_time[logic]) + ", "
                           + "execute: " + str(self.itest_execute_avg_time[logic]) + ", "
                           + "execute ccbean" + str(self.itest_execute_ccbean_avg_time[logic]) + ", "
                           + "check: " + str(self.itest_check_avg_time[logic]) + ", "
                           + "clear: " + str(self.itest_clear_avg_time[logic]) + "\n")
                file.write("+: " + str(self.cases_compose[logic]['+']) + ", ++: " + str(self.cases_compose[logic]['++']) + ", N: " + str(
                    self.cases_compose[logic]['N']) + ", SF: " + str(self.cases_compose[logic]['SF']) + ", F: " + str(
                    self.cases_compose[logic]['F']) + ", F+: " + str(self.cases_compose[logic]['F+']) + ", F++: " + str(
                    self.cases_compose[logic]['F++']) + ", YF: " + str(self.cases_compose[logic]['YF']) + ", IF: " + str(
                    self.cases_compose[logic]['IF']) + "\n\n")
                with open('fuzzing_results/bootLog/round_' + str(self.round) + '/boot.log', 'a') as file:
                    for i in range(len(self.boot_log)):
                        file.write(self.boot_log[i])
        return

    def is_test_case_start(self):
        return self.itest_test_case_start_flag == True

    def is_test_case_exec_start(self):
        return self.itest_test_case_exec_start_flag == True

    def test_case_start(self, logic, caseID):
        self.itest_test_case_start_flag = True
        self.current_logic = logic
        self.current_caseID = caseID
        self.current_failed = False
        self.current_systemFailed = False
        self.yaml_parse_failed = False
        self.current_failed_reason = None
        self.run_log = []
        self.run_coverage = [0] * 65536
        self.run_time = 0.0
        self.itest_prepare = 0.0
        self.itest_yaml_process = 0.0
        self.itest_execute = 0.0
        self.itest_execute_ccbean = 0.0
        self.itest_check = 0.0
        self.itest_clear = 0.0
        self.real_methods_unique_traversal = set()
        self.real_blocks_unique_traversal = set()
        self.cases_in_this_round[self.current_logic] += 1

        print("[" + datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f") + "] round " + str(
            round) + ": new case started, " + "logic: " + logic + ", caseID: " + caseID)
        return

    def test_case_end(self):
        coverage_update = False
        reason = []

        totalnonzerobefore = self.get_non_zero_count(self.total_coverage[self.current_logic])
        currentnonzero = self.get_non_zero_count(self.run_coverage)
        bitsupdate = self.updateBits(self.total_coverage[self.current_logic], self.run_coverage)
        totalnonzeroafter = self.get_non_zero_count(self.total_coverage[self.current_logic])
        currentrealuniquemethods = len(self.real_methods_unique_traversal)
        currentrealuniqueblocks = len(self.real_blocks_unique_traversal)
        totalrealuniquemethods = len(self.real_methods_unique[self.current_logic])
        totaltrealuniqueblocks = len(self.real_blocks_unique[self.current_logic])

        if self.current_systemFailed == False:
            if self.yaml_parse_failed == True:
                reason.append("YF")
                with open('fuzzing_results/' + self.current_logic + '/failed.txt', 'a') as file:
                    file.write("ID: " + self.current_caseID + ", Round: " + str(self.round) + ", Reason: Yaml Parse Exception" + '\n')
            elif self.current_failed == True:
                if self.current_failed_reason in self.ues_to_ignore:
                    reason.append("IF")
                    # check if coverage updates
                    if totalnonzeroafter > self.max_coverage[self.current_logic]:
                        self.max_coverage[self.current_logic] = totalnonzeroafter
                else:
                    # check if coverage updates
                    if totalnonzeroafter > self.max_coverage[self.current_logic]:
                        self.max_coverage[self.current_logic] = totalnonzeroafter
                    if bitsupdate:
                        tag = 'F+'
                        if totalnonzeroafter > totalnonzerobefore:
                            tag = tag + '+'
                        reason.append(tag)
                        reason.append(currentnonzero)
                    else:
                        tag = 'F'
                        reason.append(tag)
                        reason.append(currentnonzero)
                    with open('fuzzing_results/' + self.current_logic + '/failed.txt', 'a') as file:
                        file.write(
                            "ID: " + self.current_caseID + ", Round: " + str(self.round) + ", Reason: " + self.current_failed_reason + '\n')
            else:
                # check if coverage updates
                if totalnonzeroafter > self.max_coverage[self.current_logic]:
                    self.max_coverage[self.current_logic] = totalnonzeroafter
                if bitsupdate:
                    tag = '+'
                    if totalnonzeroafter > totalnonzerobefore:
                        tag = tag + '+'
                    reason.append(tag)
                    reason.append(currentnonzero)
                else:
                    tag = 'N'
                    reason.append(tag)
                    reason.append(currentnonzero)
        else:
            reason.append("SF")
            # check if coverage updates
            if totalnonzeroafter > self.max_coverage[self.current_logic]:
                self.max_coverage[self.current_logic] = totalnonzeroafter
            with open('fuzzing_results/' + self.current_logic + '/failed.txt', 'a') as file:
                file.write("ID: " + self.current_caseID + ", Round: " + str(self.round) + ", Reason: " + self.current_failed_reason + '\n')

        # update plot data
        with open('fuzzing_results/' + self.current_logic + '/plot.txt', 'a') as file:
            strings = [str(self.round), str(self.current_caseID), str(currentrealuniquemethods), str(totalrealuniquemethods),
                       str(currentnonzero), str(currentrealuniqueblocks), str(totalnonzeroafter), str(totaltrealuniqueblocks),
                       reason[0]]
            fstrings = ["{:>5}".format(s) for s in strings]
            file.write(' '.join(fstrings) + '\n')

        # update time profile
        if self.valid_case_avg_time[self.current_logic] == 0.0:
            self.valid_case_avg_time[self.current_logic] = self.run_time
        else:
            self.valid_case_avg_time[self.current_logic] = (self.valid_case_avg_time[self.current_logic] * (
                    self.valid_case_time_num[self.current_logic] - 1) + self.run_time) / self.valid_case_time_num[
                                                               self.current_logic]
        # update itest time profile
        # update itest prepare time
        if self.itest_prepare_avg_time[self.current_logic] == 0.0:
            self.itest_prepare_avg_time[self.current_logic] = self.itest_prepare
        else:
            self.itest_prepare_avg_time[self.current_logic] = (self.itest_prepare_avg_time[self.current_logic] * (
                    self.valid_case_time_num[self.current_logic] - 1) + self.itest_prepare) / self.valid_case_time_num[
                                                               self.current_logic]
        # update itest yaml process time
        if self.itest_yaml_process_avg_time[self.current_logic] == 0.0:
            self.itest_yaml_process_avg_time[self.current_logic] = self.itest_yaml_process
        else:
            self.itest_yaml_process_avg_time[self.current_logic] = (self.itest_yaml_process_avg_time[self.current_logic] * (
                    self.valid_case_time_num[self.current_logic] - 1) + self.itest_yaml_process) / self.valid_case_time_num[
                                                               self.current_logic]
        # update itest execute time
        if self.itest_execute_avg_time[self.current_logic] == 0.0:
            self.itest_execute_avg_time[self.current_logic] = self.itest_execute
        else:
            self.itest_execute_avg_time[self.current_logic] = (self.itest_execute_avg_time[self.current_logic] * (
                    self.valid_case_time_num[self.current_logic] - 1) + self.itest_execute) / self.valid_case_time_num[
                                                               self.current_logic]
        # update itest execute ccbean time
        if self.itest_execute_ccbean_avg_time[self.current_logic] == 0.0:
            self.itest_execute_ccbean_avg_time[self.current_logic] = self.itest_execute_ccbean
        else:
            self.itest_execute_ccbean_avg_time[self.current_logic] = (self.itest_execute_ccbean_avg_time[self.current_logic] * (
                    self.valid_case_time_num[self.current_logic] - 1) + self.itest_execute_ccbean) / self.valid_case_time_num[
                                                               self.current_logic]
        # update itest check time
        if self.itest_check_avg_time[self.current_logic] == 0.0:
            self.itest_check_avg_time[self.current_logic] = self.itest_check
        else:
            self.itest_check_avg_time[self.current_logic] = (self.itest_check_avg_time[self.current_logic] * (
                    self.valid_case_time_num[self.current_logic] - 1) + self.itest_check) / self.valid_case_time_num[
                                                               self.current_logic]
        # update itest clear time
        if self.itest_clear_avg_time[self.current_logic] == 0.0:
            self.itest_clear_avg_time[self.current_logic] = self.itest_clear
        else:
            self.itest_clear_avg_time[self.current_logic] = (self.itest_clear_avg_time[self.current_logic] * (
                    self.valid_case_time_num[self.current_logic] - 1) + self.itest_clear) / self.valid_case_time_num[
                                                               self.current_logic]

        # write log
        if reason[0] in self.save_logs_label or (int(self.current_caseID) % self.save_logs_period) == 0:
            with open('fuzzing_results/' + self.current_logic + '/round_' + str(self.round) + '/' + self.current_caseID + '.log',
                      'a') as file:
                for i in range(len(self.run_log)):
                    file.write(self.run_log[i])

        # update case profile
        if reason[0] == "+":
            self.cases_compose[self.current_logic]["+"] += 1
        elif reason[0] == "++":
            self.cases_compose[self.current_logic]["++"] += 1
        elif reason[0] == "N":
            self.cases_compose[self.current_logic]["N"] += 1
        elif reason[0] == "SF":
            self.cases_compose[self.current_logic]["SF"] += 1
        elif reason[0] == "F+":
            self.cases_compose[self.current_logic]["F+"] += 1
        elif reason[0] == "F++":
            self.cases_compose[self.current_logic]["F++"] += 1
        elif reason[0] == "F":
            self.cases_compose[self.current_logic]["F"] += 1
        elif reason[0] == "YF":
            self.cases_compose[self.current_logic]["YF"] += 1
        elif reason[0] == "IF":
            self.cases_compose[self.current_logic]["IF"] += 1

        if len(reason) > 0:
            seed_and_reason = [self.current_caseID]
            seed_and_reason.extend(reason)
            self.seedID[self.current_logic].append(seed_and_reason)

        print("[" + datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f") + "] round " + str(
            round) + ": case ended, " + "logic: " + self.current_logic + ", caseID: " + self.current_caseID)

        self.itest_test_case_start_flag = False
        self.current_logic = None
        self.current_caseID = None
        self.current_failed = False
        self.current_systemFailed = False
        self.yaml_parse_failed = False
        self.current_failed_reason = None
        self.run_log = []
        self.run_coverage = [0] * 65536
        self.run_time = 0.0
        self.real_methods_unique_traversal = set()
        self.real_blocks_unique_traversal = set()
        return

    def test_case_exec_start(self):
        self.itest_test_case_exec_start_flag = True
        return

    def test_case_exec_end(self):
        self.itest_test_case_exec_start_flag = False
        return

    def log_dump(self, log, logic, caseID):
        if logic == None or caseID == None:
            print("logic and caseID are none!")
            sys.exit(1)
        self.run_log.append(log)
        return

    def boot_log_dump(self, log):
        self.boot_log.append(log)
        return

    def update_real_unique_blocks_traversal(self, classmethod, blocknum):
        self.real_methods_unique_traversal.add("<" + classmethod + ">")
        self.real_blocks_unique_traversal.add("<" + classmethod + ">" + "#" + blocknum)
        self.real_methods_unique[self.current_logic].add("<" + classmethod + ">")
        self.real_blocks_unique[self.current_logic].add("<" + classmethod + ">" + "#" + blocknum)
        return

    def update_coverage(self, hashcode):
        self.run_coverage[hashcode] += 1
        return

    def line_filter(self, log_line):
        if "[SandBoxFuzz_error]" in log_line.lower() and "[TestController]" in log_line.lower():
            print(log_line)
        if self.sofa_boot_start == False and self.sofa_boot_done == False:
            # sofa boot not started
            bootstartmatch = self.sofa_boot_start_pattern.search(log_line)
            if bootstartmatch:
                self.sofa_boot_start = True
                self.boot_log_dump(log_line)
                self.boot_start_time = datetime.datetime.strptime(bootstartmatch.group(1), '%H:%M:%S')
                print("sofa boot started!")
        elif self.sofa_boot_start == True and self.sofa_boot_done == False:
            # sofa boot started
            bootdonematch = self.sofa_boot_done_pattern.search(log_line)
            if bootdonematch:
                self.sofa_boot_done = True
                self.boot_log_dump(log_line)
                boot_end_time = datetime.datetime.strptime(bootdonematch.group(1), '%H:%M:%S')
                self.boot_up_time = float((boot_end_time - self.boot_start_time).total_seconds())
                print("sofa boot done! itest started!")
            else:
                LPSmatch = self.LPS_pattern.match(log_line)
                if not LPSmatch:
                    self.boot_log_dump(log_line)
        elif self.sofa_boot_start == True and self.sofa_boot_done == True:
            if self.is_test_case_start() == False and self.is_test_case_exec_start() == False:
                # test case not started
                casestartmatch = self.itest_test_case_start_pattern.search(log_line)
                if casestartmatch:
                    logic, caseID = casestartmatch.groups(0)[0], casestartmatch.groups(0)[1]
                    if logic in self.logics:
                        self.test_case_start(logic, caseID)
                        self.log_dump(log_line, logic, caseID)
            elif self.is_test_case_start() == True and self.is_test_case_exec_start() == False:
                # test case started, wait for exec start or wait for case end
                execstartmatch = self.itest_test_case_exec_start_pattern.search(log_line)
                caseendmatch = self.itest_test_case_end_pattern.search(log_line)
                itestfailmatch = self.iTest_failed_pattern.search(log_line)
                if execstartmatch:
                    # exec start
                    self.test_case_exec_start()
                    self.log_dump(log_line, self.current_logic, self.current_caseID)
                elif caseendmatch:
                    # case end
                    self.log_dump(log_line, self.current_logic, self.current_caseID)
                    self.test_case_end()
                elif itestfailmatch:
                    # iTest case failed
                    self.current_systemFailed = True
                    self.current_failed_reason = "system exception found! check log for detail"
                    self.log_dump(log_line, self.current_logic, self.current_caseID)
                else:
                    # test case start but exec not start
                    LPSmatch = self.LPS_pattern.match(log_line)
                    if not LPSmatch:
                        yamlmatch = self.yaml_parse_exception_pattern.search(log_line)
                        if yamlmatch:
                            self.yaml_parse_failed = True
                        casetimematch = self.itest_case_time_pattern.search(log_line)
                        if casetimematch:
                            # get case time
                            self.valid_case_time_num[self.current_logic] += 1
                            number = int(casetimematch.group(1).replace(",", ""))
                            self.run_time = float(number) / 1000.0

                        # get itest time profile here
                        itest_prepare_match = self.itest_prepare_time_pattern.search(log_line)
                        if itest_prepare_match:
                            # get itest prepare time
                            number = int(itest_prepare_match.group(1).replace(",", ""))
                            self.itest_prepare = float(number) / 1000.0

                        itest_yaml_process_match = self.itest_yaml_process_time_pattern.search(log_line)
                        if itest_yaml_process_match:
                            # get itest yaml process time
                            number = int(itest_yaml_process_match.group(1).replace(",", ""))
                            self.itest_yaml_process = float(number) / 1000.0

                        itest_execute_match = self.itest_execute_time_pattern.search(log_line)
                        if itest_execute_match:
                            # get itest execute time
                            number = int(itest_execute_match.group(1).replace(",", ""))
                            self.itest_execute = float(number) / 1000.0

                        itest_execute_ccbean_match = self.itest_execute_ccbean_time_pattern.search(log_line)
                        if itest_execute_ccbean_match:
                            # get itest execute ccbean time
                            number = int(itest_execute_ccbean_match.group(1).replace(",", ""))
                            self.itest_execute_ccbean = float(number) / 1000.0

                        itest_check_match = self.itest_check_time_pattern.search(log_line)
                        if itest_check_match:
                            # get itest check time
                            number = int(itest_check_match.group(1).replace(",", ""))
                            self.itest_check = float(number) / 1000.0

                        itest_clear_match = self.itest_clear_time_pattern.search(log_line)
                        if itest_clear_match:
                            # get itest clear time
                            number = int(itest_clear_match.group(1).replace(",", ""))
                            self.itest_clear = float(number) / 1000.0

                        self.log_dump(log_line, self.current_logic, self.current_caseID)
            elif self.is_test_case_start() == True and self.is_test_case_exec_start() == True:
                LPSmatch = self.LPS_pattern.match(log_line)
                if LPSmatch:
                    self.update_real_unique_blocks_traversal(LPSmatch.group(1), LPSmatch.group(3))
                    self.update_coverage(int(LPSmatch.group(2)))
                    if self.LPS_in_log:
                        self.log_dump(log_line, self.current_logic, self.current_caseID)
                else:
                    execendmatch = self.itest_test_case_exec_end_pattern.search(log_line)
                    if execendmatch:
                        self.test_case_exec_end()
                    self.log_dump(log_line, self.current_logic, self.current_caseID)
                    # check if component exception or error occurs
                    if self.current_failed == False:
                        componentexceptionmatch = self.component_exception_pattern.match(log_line)
                        if componentexceptionmatch:
                            component_exception = componentexceptionmatch.group(1)
                            if component_exception in self.runtime_exception_list or component_exception in self.error_list:
                                self.current_failed = True
                                self.current_failed_reason = component_exception


if __name__ == "__main__":
    if len(sys.argv) > 1:
        print("logPy start")
        round = int(sys.argv[1])
        print(round)
        logFilter = LogFilter(round)
        if round == 0:
            logFilter.fuzzing_setup()
        else:
            print("[" + datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f") + "] round " + str(round) + ": start preparing...")
            logFilter.fuzzing_prepare()
            print("[" + datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f") + "] round " + str(round) + ": start filtering logs...")
            # read logs from stdin
            for line in sys.stdin:
                # filter every line
                logFilter.line_filter(line)
            logFilter.fuzzing_done()
    else:
        print("please specify the fuzzing round (start from 1)")
