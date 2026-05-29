package pt.up.fe.t06g10.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "rooms", indexes = {@Index(name = "idx_rooms_name", columnList = "name", unique = true)})
public class RoomEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    protected RoomEntity() {
    }

    public RoomEntity(String name) {
        this.name = name;
    }

    public RoomEntity(String name, String prompt) {
        this.name = name;
        this.prompt = prompt;
    }

    @PrePersist
    private void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getPrompt() {
        return prompt;
    }
}
