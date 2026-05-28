package pt.up.fe.t06g10.server.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "room_members", indexes = {@Index(name = "idx_room_members_room_user", columnList = "room_id,user_id", unique = true)})
public class RoomMemberEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt;

    protected RoomMemberEntity() {
    }

    public RoomMemberEntity(RoomEntity room, UserEntity user) {
        this.room = room;
        this.user = user;
    }

    @PrePersist
    private void onCreate() {
        if (joinedAt == null) {
            joinedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public RoomEntity getRoom() {
        return room;
    }

    public UserEntity getUser() {
        return user;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}
