package learning.permission;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean SecurityFilterChain security(HttpSecurity http, IdentityService identities) throws Exception {
        http.authorizeHttpRequests(r -> r
                .requestMatchers("/api/auth/csrf", "/api/auth/login").permitAll()
                .requestMatchers("/api/**").authenticated().anyRequest().denyAll())
            .requestCache(c -> c.disable())
            .formLogin(f -> f.loginProcessingUrl("/api/auth/login")
                .successHandler((req,res,auth) -> json(res,200,"登录成功，请重新获取 CSRF token"))
                .failureHandler((req,res,ex) -> json(res,401,"账号或密码错误，或账号已停用")))
            .logout(l -> l.logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((req,res,auth) -> json(res,200,"已退出")))
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req,res,ex) -> json(res,401,"请先登录"))
                .accessDeniedHandler((req,res,ex) -> json(res,403,"权限不足或 CSRF 校验失败")))
            // 保留默认 CSRF。Session 保存身份，但后续请求的权限从数据库重新读取。
            .addFilterAfter(new RefreshIdentityFilter(identities), SecurityContextHolderFilter.class);
        return http.build();
    }

    static void json(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }

    static class RefreshIdentityFilter extends OncePerRequestFilter {
        private final IdentityService identities;
        RefreshIdentityFilter(IdentityService identities) { this.identities = identities; }
        @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                FilterChain chain) throws ServletException, IOException {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
                try {
                    UserDetails fresh = identities.loadUserByUsername(auth.getName());
                    if (!fresh.isEnabled()) throw new DisabledException("disabled");
                    var context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                        fresh.getUsername(), null, fresh.getAuthorities()));
                    // 新建上下文，不原地修改 Session 中多个并发请求可能共享的对象。
                    SecurityContextHolder.setContext(context);
                } catch (UsernameNotFoundException | DisabledException ex) {
                    SecurityContextHolder.clearContext();
                    var session = request.getSession(false);
                    if (session != null) session.invalidate();
                    json(response,401,"账号不存在或已停用");
                    return;
                }
            }
            chain.doFilter(request,response);
        }
    }
}
