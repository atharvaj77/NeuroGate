package com.neurogate.agent;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
public class AgentLoopException extends RuntimeException {
    public AgentLoopException(String message) {
        super(message);
    }
}
