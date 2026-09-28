package learning.permission;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "app_user")
public class UserAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, unique = true, length = 60)
    String username;
    @Column(nullable = false)
    String passwordHash;
    @Column(nullable = false)
    Long departmentId;
    boolean enabled = true;
    @ManyToMany
    @JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "role_id"}))
    Set<AppRole> roles = new LinkedHashSet<>();
    @Version Long version;
    protected UserAccount() {}
}
