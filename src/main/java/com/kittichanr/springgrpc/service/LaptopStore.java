package com.kittichanr.springgrpc.service;

import com.kittichanr.pcbook.generated.Filter;
import com.kittichanr.pcbook.generated.Laptop;
import io.grpc.Context;

public interface LaptopStore {
    void Save(Laptop laptop) throws Exception;

    Laptop Find(String id);

    void Search(Context context, Filter filter, LaptopStream stream);
}

