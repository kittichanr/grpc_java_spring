package com.kittichanr.springgrpc.service;

public class AlreadyExistsException extends RuntimeException {
    public AlreadyExistsException(String messsage) {
        super(messsage);
    }
}
