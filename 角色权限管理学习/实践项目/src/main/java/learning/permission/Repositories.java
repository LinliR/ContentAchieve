package learning.permission;

import org.springframework.data.jpa.repository.*;
import org.springframework.data.domain.*;
import java.util.*;

interface UserRepository extends JpaRepository<UserAccount, Long> {
    @EntityGraph(attributePaths = {"roles", "roles.permissions"})
    Optional<UserAccount> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByRoles_Id(Long roleId);
}
interface RoleRepository extends JpaRepository<AppRole, Long> {
    boolean existsByCode(String code);
}
interface PermissionRepository extends JpaRepository<AppPermission, String> {}
interface ProjectRepository extends JpaRepository<Project, Long> {}
interface MemberRepository extends JpaRepository<ProjectMember, Long> {
    boolean existsByProject_IdAndUser_Id(Long projectId, Long userId);
    void deleteByProject_IdAndUser_Id(Long projectId, Long userId);
}
interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {}
interface AuditRepository extends JpaRepository<AuditEvent, Long> {
    Page<AuditEvent> findAllByOrderByIdDesc(Pageable pageable);
}
