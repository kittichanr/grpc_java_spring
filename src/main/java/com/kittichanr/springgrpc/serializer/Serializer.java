package com.kittichanr.springgrpc.serializer;

import com.google.protobuf.util.JsonFormat;
import com.kittichanr.pcbook.generated.Laptop;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Serializer {
    public void WriteBinaryFile(Laptop laptop, String filename) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(filename);
        laptop.writeTo(outputStream);
        outputStream.close();
    }

    public Laptop ReadBinaryFile(String filename) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(filename);
        Laptop laptop = Laptop.parseFrom(fileInputStream);
        fileInputStream.close();
        return laptop;
    }

    public void writeJSONFile(Laptop laptop, String filename) throws IOException {
        JsonFormat.Printer printer = JsonFormat.printer()
                .includingDefaultValueFields()
                .preservingProtoFieldNames();

        String jsonString = printer.print(laptop);

        FileOutputStream fileOutputStream = new FileOutputStream(filename);
        fileOutputStream.write(jsonString.getBytes());
        fileOutputStream.close();
    }

    public static void main(String[] args) throws IOException {
        Serializer serializer = new Serializer();
        Laptop laptop = serializer.ReadBinaryFile("laptop.bin");
        serializer.writeJSONFile(laptop, "laptop.json");
    }
}
