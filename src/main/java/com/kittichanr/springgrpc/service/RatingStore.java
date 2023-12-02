package com.kittichanr.springgrpc.service;

public interface RatingStore {
    Rating Add(String laptopID, double score);
}
