package com.agentflow.managerservice.exception;

public class ManagerCapacityExceededException extends RuntimeException {
    public ManagerCapacityExceededException(String message) {
        super(message);
    }
}
