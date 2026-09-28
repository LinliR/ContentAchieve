package learning.permission;

import jakarta.persistence.*;

@Entity
@Table(name = "project_member", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "user_id"}))
public class ProjectMember {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "project_id") Project project;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id") UserAccount user;
    protected ProjectMember() {}
    ProjectMember(Project project, UserAccount user) { this.project = project; this.user = user; }
}
