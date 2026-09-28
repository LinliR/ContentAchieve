package learning.permission;

import jakarta.persistence.*;

@Entity
@Table(name = "app_project")
public class Project {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false) String name;
    protected Project() {}
    Project(String name) { this.name = name; }
}
