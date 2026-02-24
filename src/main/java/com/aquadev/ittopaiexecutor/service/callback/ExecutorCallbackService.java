package com.aquadev.ittopaiexecutor.service.callback;

public interface ExecutorCallbackService {

    void notifyCompleted(String executionId, String s3Key);
}
