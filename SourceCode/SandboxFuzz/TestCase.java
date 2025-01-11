package com.alibaba.jvm.sandbox.module.example;
import java.util.Map;

/**
 * TestCase - Represents a test case with its data, logic, request, and mock components.
 */
public class TestCase {

    private Map<String, Object> data;
    private Map<String, Object> logic;
    private Map<String, Object> request;
    private Map<String, Object> mock;

    public TestCase() {
    }

    public TestCase(Map<String, Object> data, Map<String, Object> logic, Map<String, Object> request, Map<String, Object> mock) {
        this.data = data;
        this.logic = logic;
        this.request = request;
        this.mock = mock;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> getLogic() {
        return logic;
    }

    public void setLogic(Map<String, Object> logic) {
        this.logic = logic;
    }

    public Map<String, Object> getRequest() {
        return request;
    }

    public void setRequest(Map<String, Object> request) {
        this.request = request;
    }

    public Map<String, Object> getMock() {
        return mock;
    }

    public void setMock(Map<String, Object> mock) {
        this.mock = mock;
    }
}
