package vn.newmen.store.service;

import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import vn.newmen.store.domain.Account;
import vn.newmen.store.repository.AccountRepository;

@Component
public class CurrentAccount {
    private final AccountRepository accounts;

    public CurrentAccount(AccountRepository accounts) { this.accounts = accounts; }

    public Optional<Account> optional(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return Optional.empty();
        }
        return accounts.findByEmailIgnoreCase(authentication.getName()).filter(account -> account.active);
    }

    public Account require(Authentication authentication) {
        return optional(authentication).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Đăng nhập để tiếp tục"));
    }

    public Account requireRole(Authentication authentication, String role) {
        Account account = require(authentication);
        if (!role.equals(account.role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này");
        return account;
    }
}
