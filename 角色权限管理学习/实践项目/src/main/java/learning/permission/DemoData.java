package learning.permission;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;

/** 仅供本地学习。账号表非空时不会覆盖已有数据。 */
@Component
@ConditionalOnProperty(name="lab.seed",havingValue="true",matchIfMissing=true)
public class DemoData implements CommandLineRunner {
    private final UserRepository users; private final RoleRepository roles;
    private final PermissionRepository permissions; private final ProjectRepository projects;
    private final MemberRepository members; private final ArticleRepository articles;
    private final PasswordEncoder passwords;
    @Value("${lab.demo-password}") private String password;
    DemoData(UserRepository users,RoleRepository roles,PermissionRepository permissions,ProjectRepository projects,
            MemberRepository members,ArticleRepository articles,PasswordEncoder passwords) {
        this.users=users;this.roles=roles;this.permissions=permissions;this.projects=projects;
        this.members=members;this.articles=articles;this.passwords=passwords;
    }
    @Override @Transactional public void run(String... args) {
        if (users.count()!=0) return;
        var codes=List.of("user:manage","role:manage","permission:read","project:manage","audit:read",
            "article:read","article:create","article:update","article:delete","article:review");
        codes.forEach(c -> permissions.save(new AppPermission(c)));
        var author=role("AUTHOR","作者",DataScope.OWN,List.of("article:read","article:create","article:update","article:delete"));
        var editor=role("EDITOR","编辑",DataScope.DEPARTMENT,List.of("article:read","article:create","article:update","article:review"));
        var administrator=role("ADMIN","管理员",DataScope.ALL,codes);
        var admin=user("admin",10L,administrator); var alice=user("alice",10L,author);
        var bob=user("bob",10L,editor); var carol=user("carol",20L,author);
        var p1=projects.save(new Project("共同项目")); var p2=projects.save(new Project("独立项目"));
        for (var u:List.of(admin,alice,bob,carol)) members.save(new ProjectMember(p1,u));
        for (var u:List.of(admin,carol)) members.save(new ProjectMember(p2,u));
        article("Alice 待审核文章",alice,p1); article("Bob 自己的文章",bob,p1);
        article("Carol 跨部门文章",carol,p1); article("Carol 独立项目文章",carol,p2);
    }
    private AppRole role(String code,String name,DataScope scope,List<String> codes) {
        var r=new AppRole();r.code=code;r.name=name;r.scope=scope;r.permissions.addAll(permissions.findAllById(codes));return roles.save(r);
    }
    private UserAccount user(String name,Long dept,AppRole role) {
        var u=new UserAccount();u.username=name;u.passwordHash=passwords.encode(password);u.departmentId=dept;u.roles.add(role);return users.save(u);
    }
    private void article(String title,UserAccount owner,Project project) {
        var a=new Article();a.title=title;a.content="学习示例";a.owner=owner;a.departmentId=owner.departmentId;a.project=project;articles.save(a);
    }
}
