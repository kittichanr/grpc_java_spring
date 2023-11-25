package com.kittichanr.springgrpc.service;

import com.kittichanr.pcbook.generated.Laptop;

public interface LaptopStream {
    void Send(Laptop laptop);
}
