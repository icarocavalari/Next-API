package io.github.icarocavalari.nextapi.auth;

import io.github.icarocavalari.nextapi.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens")
@Getter
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Instant expireAt;

    private Instant revokedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    protected RefreshToken() {}

    public RefreshToken(String tokenHash, User user, Instant expireAt) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.expireAt = expireAt;
    }

    public void revoke(Instant now) {
        if (this.revokedAt == null) {
            this.revokedAt = now;
        }
    }

    public boolean isValid(Instant now) {
        return expireAt.isAfter(now) && revokedAt == null;
    }
}
