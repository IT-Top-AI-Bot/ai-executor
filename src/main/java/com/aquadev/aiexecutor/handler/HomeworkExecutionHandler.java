package com.aquadev.aiexecutor.handler;

import com.aquadev.commonlibs.HomeworkExecutionEvent;

public interface HomeworkExecutionHandler {

    void handle(HomeworkExecutionEvent event);
}
