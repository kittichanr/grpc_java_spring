package com.kittichanr.springgrpc.service;

import com.kittichanr.pcbook.generated.*;
import com.kittichanr.springgrpc.sample.Generator;
import io.grpc.ManagedChannel;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

class LaptopServerTest {
    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule(); // automatic graceful shutdown channel at the end of test
    private LaptopStore laptopStore;
    private ImageStore imageStore;
    private RatingStore ratingStore;
    private LaptopServer server;
    private ManagedChannel channel;

    @BeforeEach
    void setUp() throws Exception {
        String serverName = InProcessServerBuilder.generateName();
        InProcessServerBuilder serverBuilder = InProcessServerBuilder.forName(serverName).directExecutor();

        laptopStore = new InMemoryLaptopStore();
        imageStore = new DiskImageStore("img");
        ratingStore = new InMemoryRatingStore();
        server = new LaptopServer(serverBuilder, 0, laptopStore, imageStore, ratingStore);
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

        Laptop found = laptopStore.Find(response.getId());
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

        Laptop found = laptopStore.Find(response.getId());
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
            laptopStore.Save(laptop);
            CreateLaptopRequest request = CreateLaptopRequest.newBuilder().setLaptop(laptop).build();

            LaptopServiceGrpc.LaptopServiceBlockingStub stub = LaptopServiceGrpc.newBlockingStub(channel);
            CreateLaptopResponse response = stub.createLaptop(request);

            Assertions.assertNull(response);
            Assertions.assertNull(response.getId());
        });
        Assertions.assertEquals("ALREADY_EXISTS: laptop ID already exists", throwable.getMessage());
    }

    @Test
    public void searchLaptop() throws Exception {
        for (int i = 0; i < 6; i++) {
            Generator generator = new Generator(new Random());
            Laptop laptop = generator.NewLaptop();

            switch (i) {
                case 0:
                    laptop.toBuilder().setPriceUsd(2500).build();
                case 1:
                    CPU cpu = CPU.newBuilder().setNumberCores(2).build();
                    laptop.toBuilder().setCpu(cpu).build();
                case 2:
                    CPU cpu2 = CPU.newBuilder().setMinGhz(2).build();
                    laptop.toBuilder().setCpu(cpu2).build();
                case 3:
                    Memory memory = Memory.newBuilder().setValue(4096).setUnit(Memory.Unit.MEGABYTE).build();
                    laptop.toBuilder().setRam(memory).build();
                case 4:
                    CPU cpu3 = CPU.newBuilder().setNumberCores(4).setMaxGhz(4).setMinGhz(2).build();
                    Memory memory2 = Memory.newBuilder().setValue(16).setUnit(Memory.Unit.GIGABYTE).build();

                    laptop.toBuilder().setCpu(cpu3).setRam(memory2).build();
                case 5:
                    CPU cpu4 = CPU.newBuilder().setNumberCores(4).setMaxGhz(4).setMinGhz(2).build();
                    Memory memory3 = Memory.newBuilder().setValue(16).setUnit(Memory.Unit.GIGABYTE).build();
                    laptop.toBuilder().setCpu(cpu4).setRam(memory3).build();
            }
            laptopStore.Save(laptop);
        }

        Memory memory = Memory.newBuilder()
                .setValue(8)
                .setUnit(Memory.Unit.GIGABYTE)
                .build();

        Filter filter = Filter.newBuilder()
                .setMaxPriceUsd(3200)
                .setMinCpuCores(4)
                .setMinCpuGhz(2.5)
                .setMinRam(memory)
                .build();

        SearchLaptopRequest request = SearchLaptopRequest.newBuilder().setFilter(filter).build();

        LaptopServiceGrpc.LaptopServiceBlockingStub stub = LaptopServiceGrpc.newBlockingStub(channel);
        Iterator<SearchLaptopResponse> responseIterator = stub.searchLaptop(request);

        while (responseIterator.hasNext()) {
            SearchLaptopResponse response = responseIterator.next();
            Laptop laptop = response.getLaptop();
            Assertions.assertNotNull(laptop);
        }
    }

    @Test
    public void rateLaptop() throws Exception {
        Generator generator = new Generator(new Random());
        Laptop laptop = generator.NewLaptop();
        laptopStore.Save(laptop);

        LaptopServiceGrpc.LaptopServiceStub stub = LaptopServiceGrpc.newStub(channel);
        RateLaptopResponseStreamObserver responseObserver = new RateLaptopResponseStreamObserver();
        StreamObserver<RateLaptopRequest> requestObserver = stub.rateLaptop(responseObserver);

        double[] scores = {8, 7.5, 10};
        double[] averages = {8, 7.75, 8.5};
        int n = scores.length;

        for (int i = 0; i < n; i++) {
            RateLaptopRequest request = RateLaptopRequest.newBuilder()
                    .setLaptopId(laptop.getId())
                    .setScore(scores[i])
                    .build();
            requestObserver.onNext(request);
        }

        requestObserver.onCompleted();
        Assertions.assertNull(responseObserver.err);
        Assertions.assertTrue(responseObserver.completed);
        Assertions.assertEquals(n, responseObserver.responses.size());

        int idx = 0;
        for (RateLaptopResponse response : responseObserver.responses) {
            Assertions.assertEquals(laptop.getId(), response.getLaptopId());
            Assertions.assertEquals(idx + 1, response.getRatedCount());
            Assertions.assertEquals(averages[idx], response.getAverageScore(), 1e-9);
            idx++;
        }
    }

    private class RateLaptopResponseStreamObserver implements StreamObserver<RateLaptopResponse> {
        public List<RateLaptopResponse> responses;
        public Throwable err;
        public boolean completed;

        public RateLaptopResponseStreamObserver() {
            responses = new LinkedList<>();
        }

        @Override
        public void onNext(RateLaptopResponse response) {
            responses.add(response);
        }

        @Override
        public void onError(Throwable t) {
            err = t;
        }

        @Override
        public void onCompleted() {
            completed = true;
        }
    }
}