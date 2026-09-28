package learning.permission;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.criteria.Predicate;
import static org.springframework.http.HttpStatus.*;
import static learning.permission.ApiTypes.*;
import java.util.*;

@Service
@Transactional
public class ArticleService {
    private final ArticleRepository articles;
    private final ProjectRepository projects;
    private final MemberRepository members;
    private final IdentityService identity;
    private final AdminService admin;
    ArticleService(ArticleRepository articles, ProjectRepository projects, MemberRepository members,
                   IdentityService identity, AdminService admin) {
        this.articles=articles; this.projects=projects; this.members=members; this.identity=identity; this.admin=admin;
    }
    static ArticleView view(Article a) {
        return new ArticleView(a.id,a.title,a.content,a.owner.id,a.departmentId,a.project.id,a.status,a.version);
    }
    Specification<Article> visible(UserAccount actor, String operation) {
        var scopes=IdentityService.scopes(actor,operation);
        return (root,query,cb) -> {
            var membership=query.subquery(Long.class);
            var member=membership.from(ProjectMember.class);
            membership.select(member.get("id")).where(
                cb.equal(member.get("user").get("id"),actor.id),
                cb.equal(member.get("project").get("id"),root.get("project").get("id")));
            List<Predicate> ranges=new ArrayList<>();
            if (scopes.contains(DataScope.ALL)) ranges.add(cb.conjunction());
            if (scopes.contains(DataScope.OWN)) ranges.add(cb.equal(root.get("owner").get("id"),actor.id));
            if (scopes.contains(DataScope.DEPARTMENT)) ranges.add(cb.equal(root.get("departmentId"),actor.departmentId));
            return cb.and(cb.exists(membership),cb.or(ranges.toArray(Predicate[]::new)));
        };
    }
    Article accessible(Long id, UserAccount actor, String operation) {
        return articles.findOne(visible(actor,operation).and((r,q,cb) -> cb.equal(r.get("id"),id)))
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND,"文章不存在或不在可访问范围"));
    }
    void pending(Article a) {
        if (a.status!=Article.Status.PENDING) throw new ResponseStatusException(CONFLICT,"只有待审核文章可以执行此操作");
    }
    @PreAuthorize("hasAuthority('article:read')")
    public PageView<ArticleView> list(int page, int size) {
        var result=articles.findAll(visible(identity.current(),"article:read"),PageRequest.of(page,size,Sort.by("id")));
        return new PageView<>(result.map(ArticleService::view).getContent(),result.getTotalElements(),page,size);
    }
    @PreAuthorize("hasAuthority('article:read')")
    public ArticleView get(Long id) { return view(accessible(id,identity.current(),"article:read")); }
    @PreAuthorize("hasAuthority('article:create')")
    public ArticleView create(ArticleCreate input) {
        var actor=identity.current();
        if (!members.existsByProject_IdAndUser_Id(input.projectId(),actor.id))
            throw new ResponseStatusException(FORBIDDEN,"必须是项目成员才能创建文章");
        var p=projects.findById(input.projectId()).orElseThrow(() -> new ResponseStatusException(NOT_FOUND,"项目不存在"));
        var a=new Article(); a.title=input.title(); a.content=input.content(); a.owner=actor;
        a.departmentId=actor.departmentId; a.project=p; articles.saveAndFlush(a);
        admin.audit("ARTICLE_CREATE","article:"+a.id,"project="+p.id);
        return view(a);
    }
    @PreAuthorize("hasAuthority('article:update')")
    public ArticleView update(Long id, ArticleUpdate input) {
        var a=accessible(id,identity.current(),"article:update"); pending(a);
        a.title=input.title(); a.content=input.content(); articles.flush();
        admin.audit("ARTICLE_UPDATE","article:"+id,"updated"); return view(a);
    }
    @PreAuthorize("hasAuthority('article:delete')")
    public void delete(Long id) {
        var a=accessible(id,identity.current(),"article:delete"); pending(a);
        articles.delete(a); admin.audit("ARTICLE_DELETE","article:"+id,"deleted");
    }
    @PreAuthorize("hasAuthority('article:review')")
    public ArticleView review(Long id) {
        var actor=identity.current(); var a=accessible(id,actor,"article:review");
        if (a.owner.id.equals(actor.id)) throw new ResponseStatusException(FORBIDDEN,"不能审核自己提交的文章");
        pending(a); a.status=Article.Status.APPROVED; articles.flush();
        admin.audit("ARTICLE_REVIEW","article:"+id,"PENDING -> APPROVED"); return view(a);
    }
}
