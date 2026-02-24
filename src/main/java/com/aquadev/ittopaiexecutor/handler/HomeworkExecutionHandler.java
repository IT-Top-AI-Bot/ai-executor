package com.aquadev.ittopaiexecutor.handler;

import com.aquadev.ittopaiexecutor.dto.kafka.HomeworkExecutionEvent;

public interface HomeworkExecutionHandler {

    void handle(HomeworkExecutionEvent event);
}
