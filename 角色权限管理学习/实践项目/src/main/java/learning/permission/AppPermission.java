package learning.permission;

import jakarta.persistence.*;

@Entity
@Table(name = "app_permission")
public class AppPermission {
    @Id @Column(length = 80)
    String code;
    protected AppPermission() {}
    AppPermission(String code) { this.code = code; }
}
