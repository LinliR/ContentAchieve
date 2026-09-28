package learning.permission;

import jakarta.persistence.*;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "app_role")
public class AppRole {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @Column(nullable = false, unique = true, length = 60)
    String code;
    @Column(nullable = false, length = 100)
    String name;
    boolean enabled = true;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    DataScope scope;
    @ManyToMany
    @JoinTable(name = "role_permission", joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_code"),
        uniqueConstraints = @UniqueConstraint(columnNames = {"role_id", "permission_code"}))
    Set<AppPermission> permissions = new LinkedHashSet<>();
    @Version Long version;
    protected AppRole() {}
}
