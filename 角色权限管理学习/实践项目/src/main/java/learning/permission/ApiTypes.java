package learning.permission;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;

/** 所有响应均为 DTO，避免序列化实体时暴露密码或懒加载关联。 */
public final class ApiTypes {
    private ApiTypes() {}
    public record UserCreate(@NotBlank @Size(max=60) @Pattern(regexp="[a-zA-Z0-9_-]+") String username,
        @NotBlank @Size(min=12, max=64) String password, @NotNull @Positive Long departmentId) {}
    public record UserUpdate(@NotNull @Positive Long departmentId, @NotNull Boolean enabled) {}
    public record RoleCreate(@NotBlank @Size(max=60) @Pattern(regexp="[A-Z0-9_]+") String code,
        @NotBlank @Size(max=100) String name, @NotNull DataScope scope) {}
    public record RoleUpdate(@NotBlank @Size(max=100) String name, @NotNull Boolean enabled,
        @NotNull DataScope scope) {}
    public record RoleIds(@NotNull Set<@NotNull @Positive Long> roleIds) {}
    public record PermissionCodes(@NotNull Set<@NotBlank String> permissionCodes) {}
    public record ProjectCreate(@NotBlank @Size(max=100) String name) {}
    public record ArticleCreate(@NotBlank @Size(max=200) String title,
        @NotBlank @Size(max=10000) String content, @NotNull @Positive Long projectId) {}
    public record ArticleUpdate(@NotBlank @Size(max=200) String title,
        @NotBlank @Size(max=10000) String content) {}
    public record RoleView(Long id, String code, String name, boolean enabled, DataScope scope,
        Set<String> permissionCodes) {}
    public record UserView(Long id, String username, Long departmentId, boolean enabled, Set<Long> roleIds) {}
    public record MeView(UserView user, Set<String> authorities) {}
    public record ProjectView(Long id, String name) {}
    public record ArticleView(Long id, String title, String content, Long ownerId, Long departmentId,
        Long projectId, Article.Status status, Long version) {}
    public record AuditView(Long id, Instant occurredAt, String actor, String action, String target, String detail) {}
    public record PageView<T>(List<T> items, long total, int page, int size) {}
    public record ErrorView(String message) {}
}
