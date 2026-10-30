package com.accenture.springai_bootcamp_demo.client;

/**
 * Raised when the model server cannot be reached or gives an unusable answer.
 */
public class ModelServerException extends RuntimeException {

    public ModelServerException(String message) {
        super(message);
    }

    public ModelServerException(String message, Throwable cause) {
        super(message, cause);
    }
}
