package com.aquadev.ittopaiexecutor.handler;

import com.aquadev.commonlibs.HomeworkExecutionEvent;

public interface HomeworkExecutionHandler {

    void handle(HomeworkExecutionEvent event);
}
