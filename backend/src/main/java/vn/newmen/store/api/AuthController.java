package vn.newmen.store.api;

import static vn.newmen.store.api.ApiModels.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.newmen.store.domain.Account;
import vn.newmen.store.repository.AccountRepository;
import vn.newmen.store.service.CurrentAccount;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AccountRepository accounts;
    private final PasswordEncoder passwords;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;
    private final CsrfTokenRepository csrfTokenRepository;
    private final CurrentAccount currentAccount;

    public AuthController(AccountRepository accounts, PasswordEncoder passwords,
                          AuthenticationManager authenticationManager,
                          SecurityContextRepository contextRepository, CsrfTokenRepository csrfTokenRepository,
                          CurrentAccount currentAccount) {
        this.accounts = accounts;
        this.passwords = passwords;
        this.authenticationManager = authenticationManager;
        this.contextRepository = contextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
        this.currentAccount = currentAccount;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) { return Map.of("token", token.getToken()); }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletRequest httpRequest,
                                                 HttpServletResponse httpResponse) {
        String email = request.email().trim().toLowerCase();
        if (accounts.existsByEmailIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã được sử dụng");
        Account account = new Account();
        account.email = email;
        account.fullName = request.fullName().trim();
        account.phone = request.phone();
        account.passwordHash = passwords.encode(request.password());
        account.role = "CUSTOMER";
        account.active = true;
        accounts.save(account);
        return ResponseEntity.status(HttpStatus.CREATED).body(authenticate(email, request.password(), httpRequest, httpResponse));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest,
                             HttpServletResponse httpResponse) {
        return authenticate(request.email().trim().toLowerCase(), request.password(), httpRequest, httpResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        try { session.invalidate(); } catch (IllegalStateException ignored) { }
        csrfTokenRepository.saveToken(null, request, response);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(Authentication authentication) {
        return currentAccount.optional(authentication)
            .map(account -> ResponseEntity.ok(toResponse(account)))
            .orElseGet(() -> ResponseEntity.noContent().build());
    }

    private AuthResponse authenticate(String email, String password, HttpServletRequest request, HttpServletResponse response) {
        Authentication result = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken.unauthenticated(email, password));
        request.getSession(true);
        request.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(result);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, request, response);
        csrfTokenRepository.saveToken(null, request, response);
        Account account = accounts.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"));
        return toResponse(account);
    }

    private AuthResponse toResponse(Account account) {
        return new AuthResponse(account.id, account.email, account.fullName, account.phone, account.role);
    }
}
