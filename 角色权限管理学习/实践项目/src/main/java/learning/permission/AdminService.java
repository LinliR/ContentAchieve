package learning.permission;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.data.domain.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
import static learning.permission.ApiTypes.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class AdminService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PermissionRepository permissions;
    private final ProjectRepository projects;
    private final MemberRepository members;
    private final AuditRepository audits;
    private final IdentityService identity;
    private final PasswordEncoder passwords;
    AdminService(UserRepository users, RoleRepository roles, PermissionRepository permissions,
            ProjectRepository projects, MemberRepository members, AuditRepository audits,
            IdentityService identity, PasswordEncoder passwords) {
        this.users=users; this.roles=roles; this.permissions=permissions; this.projects=projects;
        this.members=members; this.audits=audits; this.identity=identity; this.passwords=passwords;
    }
    static UserView userView(UserAccount u) {
        return new UserView(u.id,u.username,u.departmentId,u.enabled,
            u.roles.stream().map(r -> r.id).collect(Collectors.toCollection(TreeSet::new)));
    }
    static RoleView roleView(AppRole r) {
        return new RoleView(r.id,r.code,r.name,r.enabled,r.scope,
            r.permissions.stream().map(p -> p.code).collect(Collectors.toCollection(TreeSet::new)));
    }
    void audit(String action, String target, String detail) {
        var actor = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        audits.save(new AuditEvent(actor,action,target,detail));
    }
    UserAccount user(Long id) { return users.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND,"用户不存在")); }
    AppRole role(Long id) { return roles.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND,"角色不存在")); }
    Project project(Long id) { return projects.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND,"项目不存在")); }

    @PreAuthorize("hasAuthority('user:manage')")
    public List<UserView> users() { return users.findAll(Sort.by("id")).stream().map(AdminService::userView).toList(); }
    @PreAuthorize("hasAuthority('user:manage')")
    public UserView createUser(UserCreate input) {
        if (users.existsByUsername(input.username())) throw new ResponseStatusException(CONFLICT,"用户名已存在");
        // BCrypt 接收最多 72 字节，而字符数验证不能覆盖 UTF-8 多字节情况。
        if (input.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(BAD_REQUEST,"密码不能超过 72 个 UTF-8 字节");
        var u=new UserAccount(); u.username=input.username(); u.passwordHash=passwords.encode(input.password());
        u.departmentId=input.departmentId(); users.save(u);
        audit("USER_CREATE","user:"+u.id,"username="+u.username);
        return userView(u);
    }
    @PreAuthorize("hasAuthority('user:manage')")
    public UserView updateUser(Long id, UserUpdate input) {
        var u=user(id); var before="department="+u.departmentId+",enabled="+u.enabled;
        u.departmentId=input.departmentId(); u.enabled=input.enabled();
        audit("USER_UPDATE","user:"+id,before+" -> department="+u.departmentId+",enabled="+u.enabled);
        return userView(u);
    }
    @PreAuthorize("hasAuthority('user:manage')")
    public UserView assignRoles(Long id, RoleIds input) {
        var u=user(id); var selected=new LinkedHashSet<>(roles.findAllById(input.roleIds()));
        if (selected.size()!=input.roleIds().size()) throw new ResponseStatusException(BAD_REQUEST,"包含不存在的角色");
        var before=userView(u).roleIds(); u.roles.clear(); u.roles.addAll(selected);
        audit("USER_ROLES","user:"+id,before+" -> "+input.roleIds());
        return userView(u);
    }
    @PreAuthorize("hasAuthority('role:manage')")
    public List<RoleView> roles() { return roles.findAll(Sort.by("id")).stream().map(AdminService::roleView).toList(); }
    @PreAuthorize("hasAuthority('role:manage')")
    public RoleView createRole(RoleCreate input) {
        if (roles.existsByCode(input.code())) throw new ResponseStatusException(CONFLICT,"角色编码已存在");
        var r=new AppRole(); r.code=input.code(); r.name=input.name(); r.scope=input.scope(); roles.save(r);
        audit("ROLE_CREATE","role:"+r.id,"code="+r.code+",scope="+r.scope);
        return roleView(r);
    }
    @PreAuthorize("hasAuthority('role:manage')")
    public RoleView updateRole(Long id, RoleUpdate input) {
        var r=role(id); var before=roleView(r).toString();
        r.name=input.name(); r.enabled=input.enabled(); r.scope=input.scope();
        audit("ROLE_UPDATE","role:"+id,before+" -> "+roleView(r));
        return roleView(r);
    }
    @PreAuthorize("hasAuthority('role:manage')")
    public RoleView assignPermissions(Long id, PermissionCodes input) {
        var r=role(id); var selected=new LinkedHashSet<>(permissions.findAllById(input.permissionCodes()));
        if (selected.size()!=input.permissionCodes().size()) throw new ResponseStatusException(BAD_REQUEST,"包含未定义的权限编码");
        var before=roleView(r).permissionCodes(); r.permissions.clear(); r.permissions.addAll(selected);
        audit("ROLE_PERMISSIONS","role:"+id,before+" -> "+input.permissionCodes());
        return roleView(r);
    }
    @PreAuthorize("hasAuthority('role:manage')")
    public void deleteRole(Long id) {
        var r=role(id);
        if (users.existsByRoles_Id(id)) throw new ResponseStatusException(CONFLICT,"角色仍被用户引用，请先解除分配");
        audit("ROLE_DELETE","role:"+id,"code="+r.code); roles.delete(r);
    }
    @PreAuthorize("hasAuthority('permission:read')")
    public List<String> permissions() { return permissions.findAll(Sort.by("code")).stream().map(p -> p.code).toList(); }
    @PreAuthorize("hasAuthority('project:manage')")
    public List<ProjectView> projects() { return projects.findAll(Sort.by("id")).stream().map(p -> new ProjectView(p.id,p.name)).toList(); }
    @PreAuthorize("hasAuthority('project:manage')")
    public ProjectView createProject(ProjectCreate input) {
        var p=projects.save(new Project(input.name())); audit("PROJECT_CREATE","project:"+p.id,p.name);
        return new ProjectView(p.id,p.name);
    }
    @PreAuthorize("hasAuthority('project:manage')")
    public void addMember(Long projectId, Long userId) {
        var p=project(projectId); var u=user(userId);
        if (!members.existsByProject_IdAndUser_Id(projectId,userId)) {
            members.save(new ProjectMember(p,u)); audit("MEMBER_ADD","project:"+projectId,"user="+userId);
        }
    }
    @PreAuthorize("hasAuthority('project:manage')")
    public void removeMember(Long projectId, Long userId) {
        project(projectId); user(userId);
        members.deleteByProject_IdAndUser_Id(projectId,userId);
        audit("MEMBER_REMOVE","project:"+projectId,"user="+userId);
    }
    @PreAuthorize("hasAuthority('audit:read')")
    public PageView<AuditView> audits(int page, int size) {
        var result=audits.findAllByOrderByIdDesc(PageRequest.of(page,size));
        return new PageView<>(result.map(a -> new AuditView(a.id,a.occurredAt,a.actor,a.action,a.target,a.detail)).getContent(),
            result.getTotalElements(),page,size);
    }
    public MeView me() { var u=identity.current(); return new MeView(userView(u),IdentityService.authorities(u)); }
}
