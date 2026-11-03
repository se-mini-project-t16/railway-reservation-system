package com.railway.reservation.controller;

import com.railway.reservation.dto.AccountDtos.*;
import com.railway.reservation.service.rail.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/me")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public Profile me(Authentication auth) {
        return accountService.profile(CurrentUser.id(auth));
    }

    @PutMapping
    public Profile update(@Valid @RequestBody ProfileUpdate update, Authentication auth) {
        return accountService.update(CurrentUser.id(auth), update);
    }

    @PostMapping("/password")
    public ResponseEntity<Void> password(@Valid @RequestBody PasswordChange change, Authentication auth) {
        accountService.changePassword(CurrentUser.id(auth), change);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/travellers")
    public List<Traveller> travellers(Authentication auth) {
        return accountService.travellers(CurrentUser.id(auth));
    }

    @PostMapping("/travellers")
    public ResponseEntity<Traveller> add(@Valid @RequestBody TravellerInput in, Authentication auth) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.addTraveller(CurrentUser.id(auth), in));
    }

    @PutMapping("/travellers/{id}")
    public Traveller update(@PathVariable Long id, @Valid @RequestBody TravellerInput in, Authentication auth) {
        return accountService.updateTraveller(CurrentUser.id(auth), id, in);
    }

    @DeleteMapping("/travellers/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication auth) {
        accountService.deleteTraveller(CurrentUser.id(auth), id);
        return ResponseEntity.noContent().build();
    }
}
