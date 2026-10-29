package com.accenture.springai_bootcamp_demo.voyage.service;

public class VoyageNotFoundException extends RuntimeException {

    public VoyageNotFoundException(String voyageId) {
        super("Voyage not found: " + voyageId);
    }
}
