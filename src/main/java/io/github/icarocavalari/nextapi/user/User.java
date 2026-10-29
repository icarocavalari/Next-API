package io.github.icarocavalari.nextapi.user;

import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "users_roles", joinColumns = @JoinColumn(name = "user_id"),
                inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles = new HashSet<>();

    private String email;

    private String password;

    private String nickname;

    private Integer age;

    private boolean isFake;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    protected User() {
    }

    public User(String email, String password, String nickname, Integer age, Role role) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.age = age;
        this.roles.add(role);
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }
}
