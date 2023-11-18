package com.kittichanr.springgrpc.serializer;

import com.kittichanr.pcbook.generated.Laptop;
import com.kittichanr.springgrpc.sample.Generator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Random;

class SerializerTest {

    @Test
    void writeAndReadBinaryFile() throws IOException {
        String binaryFile = "laptop.bin";
        Laptop laptop1 = new Generator(new Random()).NewLaptop();

        Serializer serializer = new Serializer();
        serializer.WriteBinaryFile(laptop1, binaryFile);

        Laptop laptop2 = serializer.ReadBinaryFile(binaryFile);
        Assertions.assertEquals(laptop1, laptop2);

    }

}