package com.nordia.rooms.room;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Component
class RoomSeeder implements ApplicationRunner {

    private final RoomRepository rooms;
    private final ObjectMapper mapper;
    private final Resource source;

    RoomSeeder(RoomRepository rooms, ObjectMapper mapper,
               @Value("classpath:rooms.json") Resource source) {
        this.rooms = rooms;
        this.mapper = mapper;
        this.source = source;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (rooms.count() > 0) return;
        try (InputStream in = source.getInputStream()) {
            List<RoomSeed> seeds = mapper.readValue(in, new TypeReference<>() {});
            rooms.saveAll(seeds.stream().map(RoomSeed::toEntity).toList());
        }
    }

    private record RoomSeed(Long id, String name, int capacity, int floor, List<String> equipment) {
        Room toEntity() {
            return new Room(id, name, capacity, floor, equipment);
        }
    }
}