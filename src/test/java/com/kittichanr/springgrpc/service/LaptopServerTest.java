package com.kittichanr.springgrpc.service;

import com.kittichanr.pcbook.generated.CreateLaptopRequest;
import com.kittichanr.pcbook.generated.CreateLaptopResponse;
import com.kittichanr.pcbook.generated.Laptop;
import com.kittichanr.pcbook.generated.LaptopServiceGrpc;
import com.kittichanr.springgrpc.sample.Generator;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Random;

class LaptopServerTest {
    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule(); // automatic graceful shutdown channel at the end of test
    private LaptopStore store;
    private LaptopServer server;
    private ManagedChannel channel;

    @BeforeEach
    void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();
        InProcessServerBuilder serverBuilder = InProcessServerBuilder.forName(serverName).directExecutor();

        store = new InMemoryLaptopStore();
        server = new LaptopServer(serverBuilder, 0, store);
        server.start();

        channel = grpcCleanup.register(
                InProcessChannelBuilder.forName(serverName).directExecutor().build()
        );

    }

    @AfterEach
    void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void createLaptopWithValidID() {
        Generator generator = new Generator(new Random());
        Laptop laptop = generator.NewLaptop();
        CreateLaptopRequest request = CreateLaptopRequest.newBuilder().setLaptop(laptop).build();

        LaptopServiceGrpc.LaptopServiceBlockingStub stub = LaptopServiceGrpc.newBlockingStub(channel);
        CreateLaptopResponse response = stub.createLaptop(request);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(laptop.getId(), response.getId());

        Laptop found = store.Find(response.getId());
        Assertions.assertNotNull(found);
    }

    @Test
    public void createLaptopWithEmptyID() {
        Generator generator = new Generator(new Random());
        Laptop laptop = generator.NewLaptop().toBuilder().setId("").build();
        CreateLaptopRequest request = CreateLaptopRequest.newBuilder().setLaptop(laptop).build();

        LaptopServiceGrpc.LaptopServiceBlockingStub stub = LaptopServiceGrpc.newBlockingStub(channel);
        CreateLaptopResponse response = stub.createLaptop(request);

        Assertions.assertNotNull(response);
        Assertions.assertFalse(response.getId().isEmpty());

        Laptop found = store.Find(response.getId());
        Assertions.assertNotNull(found);
    }

    @Test
    public void createLaptopWithInvalidID() {
        StatusRuntimeException throwable = Assertions.assertThrows(StatusRuntimeException.class, () -> {
            Generator generator = new Generator(new Random());
            Laptop laptop = generator.NewLaptop().toBuilder().setId("invalid").build();
            CreateLaptopRequest request = CreateLaptopRequest.newBuilder().setLaptop(laptop).build();

            LaptopServiceGrpc.LaptopServiceBlockingStub stub = LaptopServiceGrpc.newBlockingStub(channel);
            CreateLaptopResponse response = stub.createLaptop(request);

            Assertions.assertNull(response);
            Assertions.assertNull(response.getId());
        });
        Assertions.assertEquals("INVALID_ARGUMENT: Invalid UUID string: invalid", throwable.getMessage());
    }

    @Test
    public void createLaptopWithAnAlreadyExistsID() {
        StatusRuntimeException throwable = Assertions.assertThrows(StatusRuntimeException.class, () -> {
            Generator generator = new Generator(new Random());
            Laptop laptop = generator.NewLaptop();
            store.Save(laptop);
            CreateLaptopRequest request = CreateLaptopRequest.newBuilder().setLaptop(laptop).build();

            LaptopServiceGrpc.LaptopServiceBlockingStub stub = LaptopServiceGrpc.newBlockingStub(channel);
            CreateLaptopResponse response = stub.createLaptop(request);

            Assertions.assertNull(response);
            Assertions.assertNull(response.getId());
        });
        Assertions.assertEquals("ALREADY_EXISTS: laptop ID already exists", throwable.getMessage());
    }
}