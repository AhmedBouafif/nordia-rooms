package com.nordia.rooms.room;

import com.nordia.rooms.common.ApiException;
import com.nordia.rooms.common.ErrorCode;
import com.nordia.rooms.common.PageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    private final RoomRepository rooms;

    public RoomService(RoomRepository rooms) {
        this.rooms = rooms;
    }

    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> list(Integer minCapacity, int page, int size) {
        PageRequest request = PageRequest.of(page, size, Sort.by("id"));
        Page<Room> result = minCapacity == null
                ? rooms.findAll(request)
                : rooms.findByCapacityGreaterThanEqual(minCapacity, request);
        return PageResponse.from(result.map(RoomResponse::from));
    }

    public Room require(Long id) {
        return rooms.findById(id).orElseThrow(() ->
                new ApiException(ErrorCode.ROOM_NOT_FOUND, "Room " + id + " does not exist."));
    }
}