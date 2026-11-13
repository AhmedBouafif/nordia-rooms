package com.nordia.rooms.room;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
public class Room {

    @Id
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private int capacity;

    @Column(nullable = false)
    private int floor;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "room_equipment", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "item", nullable = false)
    private List<String> equipment = new ArrayList<>();

    protected Room() {}

    public Room(Long id, String name, int capacity, int floor, List<String> equipment) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.floor = floor;
        this.equipment = new ArrayList<>(equipment);
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public int getCapacity() { return capacity; }
    public int getFloor() { return floor; }
    public List<String> getEquipment() { return equipment; }
}