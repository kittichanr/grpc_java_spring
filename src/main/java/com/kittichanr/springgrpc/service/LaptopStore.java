package com.kittichanr.springgrpc.service;

import com.kittichanr.pcbook.generated.Laptop;

public interface LaptopStore {
    void Save(Laptop laptop) throws Exception;

    Laptop Find(String id);
}
