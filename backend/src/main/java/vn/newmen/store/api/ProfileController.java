package vn.newmen.store.api;

import static vn.newmen.store.api.ApiModels.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import vn.newmen.store.domain.Account;
import vn.newmen.store.domain.Address;
import vn.newmen.store.repository.AccountRepository;
import vn.newmen.store.repository.AddressRepository;
import vn.newmen.store.service.CurrentAccount;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {
    private final CurrentAccount currentAccount;
    private final AddressRepository addresses;
    private final AccountRepository accounts;

    public ProfileController(CurrentAccount currentAccount, AddressRepository addresses, AccountRepository accounts) {
        this.currentAccount = currentAccount;
        this.addresses = addresses;
        this.accounts = accounts;
    }

    @GetMapping
    public ProfileResponse get(Authentication authentication) {
        Account account = currentAccount.require(authentication);
        return new ProfileResponse(account.id, account.email, account.fullName, account.phone);
    }

    @PatchMapping
    public ProfileResponse update(@Valid @RequestBody ProfileUpdateRequest request, Authentication authentication) {
        Account account = currentAccount.require(authentication);
        account.fullName = request.fullName().trim();
        account.phone = request.phone();
        accounts.save(account);
        return new ProfileResponse(account.id, account.email, account.fullName, account.phone);
    }

    @GetMapping("/addresses")
    public Map<String, List<AddressResponse>> addresses(Authentication authentication) {
        Account account = currentAccount.require(authentication);
        return Map.of("data", addresses.findByAccountIdOrderByIsDefaultDescIdDesc(account.id).stream().map(this::toResponse).toList());
    }

    @PostMapping("/addresses")
    public ResponseEntity<AddressResponse> createAddress(@Valid @RequestBody AddressWriteRequest request,
                                                          Authentication authentication) {
        Account account = currentAccount.require(authentication);
        if (request.isDefault()) clearDefaults(account.id);
        Address address = fromInput(request.address(), account, request.isDefault());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(addresses.save(address)));
    }

    @PatchMapping("/addresses/{id}")
    public AddressResponse updateAddress(@PathVariable Long id, @Valid @RequestBody AddressWriteRequest request,
                                         Authentication authentication) {
        Account account = currentAccount.require(authentication);
        Address address = addresses.findByIdAndAccountId(id, account.id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy địa chỉ"));
        if (request.isDefault()) clearDefaults(account.id);
        Address updated = fromInput(request.address(), account, request.isDefault());
        updated.id = address.id;
        return toResponse(addresses.save(updated));
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(@PathVariable Long id, Authentication authentication) {
        Account account = currentAccount.require(authentication);
        Address address = addresses.findByIdAndAccountId(id, account.id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy địa chỉ"));
        addresses.delete(address);
        return ResponseEntity.noContent().build();
    }

    private Address fromInput(AddressInput input, Account owner, boolean isDefault) {
        Address address = new Address();
        address.account = owner;
        address.recipientName = input.recipientName().trim();
        address.phone = input.phone().trim();
        address.line1 = input.line1().trim();
        address.ward = input.ward();
        address.district = input.district().trim();
        address.province = input.province().trim();
        address.isDefault = isDefault;
        return address;
    }

    private AddressResponse toResponse(Address address) {
        return new AddressResponse(address.id, address.recipientName, address.phone, address.line1,
            address.ward, address.district, address.province, address.isDefault);
    }

    private void clearDefaults(Long accountId) {
        addresses.findByAccountIdOrderByIsDefaultDescIdDesc(accountId).forEach(address -> {
            if (address.isDefault) { address.isDefault = false; addresses.save(address); }
        });
    }

}
