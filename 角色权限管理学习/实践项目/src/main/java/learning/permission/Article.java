package learning.permission;

import jakarta.persistence.*;

@Entity
@Table(name = "article")
public class Article {
    public enum Status { PENDING, APPROVED }
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(nullable = false, length = 200) String title;
    @Column(nullable = false, length = 10000) String content;
    @ManyToOne(optional = false) @JoinColumn(name = "owner_id") UserAccount owner;
    @Column(nullable = false) Long departmentId;
    @ManyToOne(optional = false) @JoinColumn(name = "project_id") Project project;
    @Enumerated(EnumType.STRING) @Column(nullable = false) Status status = Status.PENDING;
    @Version Long version;
    protected Article() {}
}
