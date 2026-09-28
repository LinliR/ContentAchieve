package learning.permission;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.web.csrf.CsrfToken;
import static learning.permission.ApiTypes.*;
import java.util.*;

@RestController
@RequestMapping("/api")
@Validated
public class ApiController {
    private final AdminService admin;
    private final ArticleService articles;
    ApiController(AdminService admin, ArticleService articles) { this.admin=admin; this.articles=articles; }
    @GetMapping("/auth/csrf") public Map<String,String> csrf(CsrfToken token) {
        return Map.of("token",token.getToken(),"headerName",token.getHeaderName());
    }
    @GetMapping("/auth/me") public MeView me() { return admin.me(); }
    @GetMapping("/users") public List<UserView> users() { return admin.users(); }
    @PostMapping("/users") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public UserView createUser(@Valid @RequestBody UserCreate input) { return admin.createUser(input); }
    @PutMapping("/users/{id}") public UserView updateUser(@PathVariable Long id,@Valid @RequestBody UserUpdate input) { return admin.updateUser(id,input); }
    @PutMapping("/users/{id}/roles") public UserView userRoles(@PathVariable Long id,@Valid @RequestBody RoleIds input) { return admin.assignRoles(id,input); }
    @GetMapping("/roles") public List<RoleView> roles() { return admin.roles(); }
    @PostMapping("/roles") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public RoleView createRole(@Valid @RequestBody RoleCreate input) { return admin.createRole(input); }
    @PutMapping("/roles/{id}") public RoleView updateRole(@PathVariable Long id,@Valid @RequestBody RoleUpdate input) { return admin.updateRole(id,input); }
    @PutMapping("/roles/{id}/permissions") public RoleView rolePermissions(@PathVariable Long id,@Valid @RequestBody PermissionCodes input) { return admin.assignPermissions(id,input); }
    @DeleteMapping("/roles/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void deleteRole(@PathVariable Long id) { admin.deleteRole(id); }
    @GetMapping("/permissions") public List<String> permissions() { return admin.permissions(); }
    @GetMapping("/projects") public List<ProjectView> projects() { return admin.projects(); }
    @PostMapping("/projects") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ProjectView createProject(@Valid @RequestBody ProjectCreate input) { return admin.createProject(input); }
    @PutMapping("/projects/{projectId}/members/{userId}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void addMember(@PathVariable Long projectId,@PathVariable Long userId) { admin.addMember(projectId,userId); }
    @DeleteMapping("/projects/{projectId}/members/{userId}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long projectId,@PathVariable Long userId) { admin.removeMember(projectId,userId); }
    @GetMapping("/audits") public PageView<AuditView> audits(@RequestParam(defaultValue="0") @Min(0) int page,
        @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) { return admin.audits(page,size); }
    @GetMapping("/articles") public PageView<ArticleView> list(@RequestParam(defaultValue="0") @Min(0) int page,
        @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) { return articles.list(page,size); }
    @GetMapping("/articles/{id}") public ArticleView get(@PathVariable Long id) { return articles.get(id); }
    @PostMapping("/articles") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ArticleView create(@Valid @RequestBody ArticleCreate input) { return articles.create(input); }
    @PutMapping("/articles/{id}") public ArticleView update(@PathVariable Long id,@Valid @RequestBody ArticleUpdate input) { return articles.update(id,input); }
    @DeleteMapping("/articles/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { articles.delete(id); }
    @PostMapping("/articles/{id}/review") public ArticleView review(@PathVariable Long id) { return articles.review(id); }
}
