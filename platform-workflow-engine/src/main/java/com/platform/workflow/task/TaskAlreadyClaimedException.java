package com.platform.workflow.task;

public class TaskAlreadyClaimedException extends RuntimeException {
    public TaskAlreadyClaimedException(String taskId) {
        super("Task " + taskId + " is already being claimed by another user");
    }
}
