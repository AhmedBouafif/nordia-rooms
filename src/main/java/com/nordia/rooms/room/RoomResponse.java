package com.nordia.rooms.room;

import java.util.List;

public record RoomResponse(Long id, String name, int capacity, int floor, List<String> equipment) {

    static RoomResponse from(Room room) {
        return new RoomResponse(room.getId(), room.getName(), room.getCapacity(),
                room.getFloor(), List.copyOf(room.getEquipment()));
    }
}