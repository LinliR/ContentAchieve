package learning.permission;

import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;
import java.util.*;

/** 使用真实表单登录、Session、CSRF token 与数据库，不用 mockUser 跳过认证链。 */
@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:permission-tests;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "lab.demo-password=Learn-RBAC-2026!"
})
@AutoConfigureMockMvc
class AuthorizationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired PermissionRepository permissions;
    @Autowired ProjectRepository projects;
    @Autowired MemberRepository members;
    @Autowired ArticleRepository articles;
    @Autowired AuditRepository audits;
    @Autowired DemoData seed;
    @Autowired PlatformTransactionManager manager;

    @BeforeEach void resetDatabase() {
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            audits.deleteAll(); audits.flush(); articles.deleteAll(); articles.flush();
            members.deleteAll(); members.flush(); users.deleteAll(); users.flush();
            roles.deleteAll(); roles.flush(); permissions.deleteAll(); permissions.flush();
            projects.deleteAll(); projects.flush();
        });
        seed.run();
    }
    record Client(MockHttpSession session) {}
    JsonNode body(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsString()); }
    long userId(String name) { return users.findByUsername(name).orElseThrow().id; }
    long roleId(String code) { return roles.findAll().stream().filter(r -> r.code.equals(code)).findFirst().orElseThrow().id; }
    long projectId(String name) { return projects.findAll().stream().filter(p -> p.name.equals(name)).findFirst().orElseThrow().id; }
    long articleId(String prefix) { return articles.findAll().stream().filter(a -> a.title.startsWith(prefix)).findFirst().orElseThrow().id; }

    Client login(String username) throws Exception {
        var initial=mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        var session=(MockHttpSession)initial.getRequest().getSession(false);
        var token=body(initial);
        var logged=mvc.perform(post("/api/auth/login").session(session)
            .header(token.get("headerName").asText(),token.get("token").asText())
            .param("username",username).param("password","Learn-RBAC-2026!"))
            .andExpect(status().isOk()).andReturn();
        return new Client((MockHttpSession)logged.getRequest().getSession(false));
    }
    ResultActions read(Client client,String uri) throws Exception { return mvc.perform(get(uri).session(client.session())); }
    ResultActions write(Client client,MockHttpServletRequestBuilder request,Object value) throws Exception {
        var token=body(mvc.perform(get("/api/auth/csrf").session(client.session())).andExpect(status().isOk()).andReturn());
        request.session(client.session()).header(token.get("headerName").asText(),token.get("token").asText());
        if (value!=null) request.contentType("application/json").content(json.writeValueAsString(value));
        return mvc.perform(request);
    }
    List<Long> listed(Client client) throws Exception {
        var data=body(read(client,"/api/articles?size=100").andExpect(status().isOk()).andReturn());
        List<Long> ids=new ArrayList<>();data.get("items").forEach(a -> ids.add(a.get("id").asLong()));return ids;
    }
    void grant(Client admin,long role,Set<String> codes) throws Exception {
        write(admin,put("/api/roles/"+role+"/permissions"),Map.of("permissionCodes",codes)).andExpect(status().isOk());
    }

    @Test void anonymousCannotReadOrWriteAndUnknownRoutesAreDenied() throws Exception {
        mvc.perform(get("/api/articles")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/roles").contentType("application/json").content("{}"))
            .andExpect(status().isForbidden()); // 未携带 CSRF token 时，在认证检查前已被拒绝。
        read(login("admin"),"/unmapped-route").andExpect(status().isForbidden());
    }

    @Test void loginRejectsBadCredentialsAndLogoutInvalidatesSession() throws Exception {
        var initial=mvc.perform(get("/api/auth/csrf")).andReturn();
        var session=(MockHttpSession)initial.getRequest().getSession(false);var token=body(initial);
        mvc.perform(post("/api/auth/login").session(session)
            .header(token.get("headerName").asText(),token.get("token").asText())
            .param("username","alice").param("password","wrong")).andExpect(status().isUnauthorized());
        var alice=login("alice");read(alice,"/api/auth/me").andExpect(status().isOk());
        write(alice,post("/api/auth/logout"),null).andExpect(status().isOk());
        assertThat(alice.session().isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test void csrfRequiredEvenForAdministratorAndLogin() throws Exception {
        mvc.perform(post("/api/auth/login").param("username","admin").param("password","Learn-RBAC-2026!"))
            .andExpect(status().isForbidden());
        var admin=login("admin");
        mvc.perform(post("/api/roles").session(admin.session()).contentType("application/json")
            .content("{\"code\":\"NEW\",\"name\":\"test\",\"scope\":\"OWN\"}"))
            .andExpect(status().isForbidden());
    }

    @Test void authorCannotGrantRolesToSelfOrReadAdministration() throws Exception {
        var alice=login("alice");
        read(alice,"/api/roles").andExpect(status().isForbidden());
        read(alice,"/api/users").andExpect(status().isForbidden());
        read(alice,"/api/permissions").andExpect(status().isForbidden());
        read(alice,"/api/audits").andExpect(status().isForbidden());
        write(alice,put("/api/users/"+userId("alice")+"/roles"),Map.of("roleIds",Set.of(roleId("ADMIN"))))
            .andExpect(status().isForbidden());
    }

    @Test void authorSeesOwnArticlesAndCannotChangeOtherAuthors() throws Exception {
        var alice=login("alice");var own=articleId("Alice");var other=articleId("Bob");
        assertThat(listed(alice)).containsExactly(own);
        read(alice,"/api/articles/"+other).andExpect(status().isNotFound());
        write(alice,put("/api/articles/"+other),Map.of("title","hijack","content","x")).andExpect(status().isNotFound());
        write(alice,delete("/api/articles/"+other),null).andExpect(status().isNotFound());
        write(alice,put("/api/articles/"+own),Map.of("title","updated","content","x")).andExpect(status().isOk());
    }

    @Test void editorSeesOnlyDepartmentAndPaginationTotalMatchesScope() throws Exception {
        var bob=login("bob");
        assertThat(listed(bob)).containsExactlyInAnyOrder(articleId("Alice"),articleId("Bob"));
        read(bob,"/api/articles?size=1").andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(2)).andExpect(jsonPath("$.items.length()").value(1));
        var other=articleId("Carol 跨");
        read(bob,"/api/articles/"+other).andExpect(status().isNotFound());
        write(bob,put("/api/articles/"+other),Map.of("title","x","content","x")).andExpect(status().isNotFound());
        write(bob,post("/api/articles/"+other+"/review"),null).andExpect(status().isNotFound());
    }

    @Test void reviewRequiresPermissionNonOwnerAndPendingState() throws Exception {
        var bob=login("bob");var alice=login("alice");var own=articleId("Alice");
        write(alice,post("/api/articles/"+own+"/review"),null).andExpect(status().isForbidden());
        write(bob,post("/api/articles/"+articleId("Bob")+"/review"),null).andExpect(status().isForbidden());
        write(bob,post("/api/articles/"+own+"/review"),null).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));
        write(bob,post("/api/articles/"+own+"/review"),null).andExpect(status().isConflict());
        write(alice,put("/api/articles/"+own),Map.of("title","x","content","x")).andExpect(status().isConflict());
        write(alice,delete("/api/articles/"+own),null).andExpect(status().isConflict());
    }

    @Test void membershipRemovalTakesEffectWithoutReloginIncludingForAllScope() throws Exception {
        var admin=login("admin");var bob=login("bob");var p=projectId("共同项目");var article=articleId("Alice");
        write(admin,delete("/api/projects/"+p+"/members/"+userId("bob")),null).andExpect(status().isNoContent());
        assertThat(listed(bob)).isEmpty();
        read(bob,"/api/articles/"+article).andExpect(status().isNotFound());
        write(bob,post("/api/articles/"+article+"/review"),null).andExpect(status().isNotFound());
        write(bob,post("/api/articles"),Map.of("title","x","content","x","projectId",p)).andExpect(status().isForbidden());
        write(admin,delete("/api/projects/"+p+"/members/"+userId("admin")),null).andExpect(status().isNoContent());
        read(admin,"/api/articles/"+article).andExpect(status().isNotFound());
    }

    @Test void permissionRevocationAffectsExistingSession() throws Exception {
        var admin=login("admin");var bob=login("bob");
        grant(admin,roleId("EDITOR"),Set.of("article:read"));
        write(bob,post("/api/articles/"+articleId("Alice")+"/review"),null).andExpect(status().isForbidden());
        read(bob,"/api/articles").andExpect(status().isOk());
        read(bob,"/api/auth/me").andExpect(jsonPath("$.authorities.length()").value(1));
    }

    @Test void roleDisableAndReenableRefreshExistingSession() throws Exception {
        var admin=login("admin");var bob=login("bob");var role=roleId("EDITOR");
        write(admin,put("/api/roles/"+role),Map.of("name","编辑","scope","DEPARTMENT","enabled",false))
            .andExpect(status().isOk());
        read(bob,"/api/articles").andExpect(status().isForbidden());
        write(admin,put("/api/roles/"+role),Map.of("name","编辑","scope","DEPARTMENT","enabled",true))
            .andExpect(status().isOk());
        read(bob,"/api/articles").andExpect(status().isOk());
    }

    @Test void disabledAccountCannotUseOldSessionOrLoginAgain() throws Exception {
        var admin=login("admin");var alice=login("alice");
        write(admin,put("/api/users/"+userId("alice")),Map.of("departmentId",10,"enabled",false)).andExpect(status().isOk());
        read(alice,"/api/auth/me").andExpect(status().isUnauthorized());
        assertThat(alice.session().isInvalid()).isTrue();
        var initial=mvc.perform(get("/api/auth/csrf")).andReturn();var token=body(initial);
        mvc.perform(post("/api/auth/login").session((MockHttpSession)initial.getRequest().getSession(false))
            .header(token.get("headerName").asText(),token.get("token").asText())
            .param("username","alice").param("password","Learn-RBAC-2026!"))
            .andExpect(status().isUnauthorized());
    }

    @Test void multiRoleUnionDoesNotCombineUnrelatedOperationScopes() throws Exception {
        var admin=login("admin");var alice=login("alice");
        var created=body(write(admin,post("/api/roles"),Map.of("code","GLOBAL_READER","name","全量阅读","scope","ALL"))
            .andExpect(status().isCreated()).andReturn());var reader=created.get("id").asLong();
        grant(admin,reader,Set.of("article:read"));
        write(admin,put("/api/users/"+userId("alice")+"/roles"),Map.of("roleIds",Set.of(roleId("AUTHOR"),reader)))
            .andExpect(status().isOk());
        read(alice,"/api/articles/"+articleId("Carol 跨")).andExpect(status().isOk());
        write(alice,put("/api/articles/"+articleId("Bob")),Map.of("title","x","content","x"))
            .andExpect(status().isNotFound());
        // 移除作者角色仍保留读权限，但修改资格消失。
        write(admin,put("/api/users/"+userId("alice")+"/roles"),Map.of("roleIds",Set.of(reader))).andExpect(status().isOk());
        read(alice,"/api/articles").andExpect(status().isOk());
        write(alice,put("/api/articles/"+articleId("Alice")),Map.of("title","x","content","x"))
            .andExpect(status().isForbidden());
    }

    @Test void roleLifecycleRejectsReferencesUnknownPermissionsAndDuplicateCodes() throws Exception {
        var admin=login("admin");
        var r=body(write(admin,post("/api/roles"),Map.of("code","READER","name","阅读","scope","OWN"))
            .andExpect(status().isCreated()).andReturn()).get("id").asLong();
        write(admin,post("/api/roles"),Map.of("code","READER","name","重复","scope","OWN")).andExpect(status().isConflict());
        write(admin,put("/api/roles/"+r+"/permissions"),Map.of("permissionCodes",Set.of("unknown")))
            .andExpect(status().isBadRequest());
        grant(admin,r,Set.of("article:read"));
        write(admin,put("/api/users/"+userId("alice")+"/roles"),Map.of("roleIds",Set.of(r))).andExpect(status().isOk());
        write(admin,delete("/api/roles/"+r),null).andExpect(status().isConflict());
        write(admin,put("/api/users/"+userId("alice")+"/roles"),Map.of("roleIds",Set.of())).andExpect(status().isOk());
        write(admin,delete("/api/roles/"+r),null).andExpect(status().isNoContent());
        assertThat(roles.existsById(r)).isFalse();
        read(admin,"/api/audits").andExpect(status().isOk()).andExpect(jsonPath("$.items[0].action").value("ROLE_DELETE"));
    }

    @Test void createUserAndProjectThenAuthorCanCreateAndDeleteOwnArticle() throws Exception {
        var admin=login("admin");
        var created=body(write(admin,post("/api/users"),Map.of("username","dave","password","Learning-2026!","departmentId",30))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.passwordHash").doesNotExist()).andReturn());
        var id=created.get("id").asLong();
        write(admin,put("/api/users/"+id+"/roles"),Map.of("roleIds",Set.of(roleId("AUTHOR")))).andExpect(status().isOk());
        var p=body(write(admin,post("/api/projects"),Map.of("name","新项目")).andExpect(status().isCreated()).andReturn()).get("id").asLong();
        var alice=login("alice");
        write(admin,put("/api/projects/"+p+"/members/"+userId("alice")),null).andExpect(status().isNoContent());
        var article=body(write(alice,post("/api/articles"),Map.of("title","new","content","text","projectId",p))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.ownerId").value(userId("alice")))
            .andExpect(jsonPath("$.departmentId").value(10)).andReturn()).get("id").asLong();
        write(alice,delete("/api/articles/"+article),null).andExpect(status().isNoContent());
        read(alice,"/api/articles/"+article).andExpect(status().isNotFound());
    }

    @Test void invalidOrSpoofedInputsFailWithoutWriting() throws Exception {
        var admin=login("admin");var alice=login("alice");
        write(admin,post("/api/roles"),Map.of("code","","name","x","scope","OWN")).andExpect(status().isBadRequest());
        write(admin,post("/api/users"),Map.of("username","dave","password","short","departmentId",10)).andExpect(status().isBadRequest());
        write(admin,put("/api/users/"+userId("alice")+"/roles"),Map.of("roleIds",Set.of(999999L))).andExpect(status().isBadRequest());
        write(alice,post("/api/articles"),Map.of("title","x","content","x","projectId",projectId("共同项目"),"ownerId",userId("admin")))
            .andExpect(status().isBadRequest());
        read(alice,"/api/articles?size=101").andExpect(status().isBadRequest());
        read(alice,"/api/articles?page=-1").andExpect(status().isBadRequest());
    }

    @Test void departmentChangeImmediatelyChangesDataScope() throws Exception {
        var admin=login("admin");var bob=login("bob");
        write(admin,put("/api/users/"+userId("bob")),Map.of("departmentId",20,"enabled",true)).andExpect(status().isOk());
        assertThat(listed(bob)).containsExactly(articleId("Carol 跨"));
        read(bob,"/api/articles/"+articleId("Alice")).andExpect(status().isNotFound());
    }
}
