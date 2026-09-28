package learning.permission;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class IdentityService implements UserDetailsService {
    private final UserRepository users;
    IdentityService(UserRepository users) { this.users = users; }

    @Override public UserDetails loadUserByUsername(String username) {
        var account = users.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("账号不存在"));
        return User.withUsername(account.username).password(account.passwordHash)
            .disabled(!account.enabled).authorities(authorities(account).toArray(String[]::new)).build();
    }

    UserAccount current() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken)
            throw new ResponseStatusException(UNAUTHORIZED, "请先登录");
        var account = users.findByUsername(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "账号不存在"));
        if (!account.enabled) throw new ResponseStatusException(UNAUTHORIZED, "账号已停用");
        return account;
    }

    static Set<String> authorities(UserAccount account) {
        return account.roles.stream().filter(r -> r.enabled).flatMap(r -> r.permissions.stream())
            .map(p -> p.code).collect(Collectors.toCollection(TreeSet::new));
    }

    static Set<DataScope> scopes(UserAccount account, String permission) {
        // 只合并授予当前操作的角色范围，防止“全量可读 + 本人可改”变成“全量可改”。
        return account.roles.stream().filter(r -> r.enabled)
            .filter(r -> r.permissions.stream().anyMatch(p -> p.code.equals(permission)))
            .map(r -> r.scope).collect(Collectors.toSet());
    }
}
