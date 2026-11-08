package com.railway.reservation.service.google;

import com.railway.reservation.dto.AccountDtos.Profile;
import com.railway.reservation.dto.LoginResponseDto;
import com.railway.reservation.exception.ApiException;
import com.railway.reservation.model.User;
import com.railway.reservation.repository.UserRepository;
import com.railway.reservation.service.JwtService;
import com.railway.reservation.service.rail.AccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Google as a second way into an existing account. A passenger links Google from the account
 * page while signed in; afterwards that Google account can sign in. Google never creates an
 * account here, so an unlinked Google account is turned away.
 */
@Service
public class GoogleSignInService {

    private static final Logger log = LoggerFactory.getLogger(GoogleSignInService.class);

    private final GoogleTokenVerifier verifier;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public GoogleSignInService(GoogleTokenVerifier verifier, UserRepository userRepository, JwtService jwtService) {
        this.verifier = verifier;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponseDto signIn(String credential) {
        GoogleIdentity google = verifier.verify(credential);
        User user = userRepository.findByGoogleSubject(google.subject()).orElseThrow(() -> new ApiException(
                HttpStatus.UNAUTHORIZED, "GOOGLE_NOT_LINKED",
                "This Google account is not linked to an account here. Sign in with your email and password, "
                        + "then link Google from your account page."));
        if (user.isAccountLocked()) {
            // Repeated wrong passwords lock every way in, not only the password form
            throw new ApiException(HttpStatus.UNAUTHORIZED, "ACCOUNT_LOCKED",
                    "This account is locked for a few minutes after repeated wrong passwords. Try again later.");
        }
        log.info("Signed in with Google userId={}", user.getId());
        return new LoginResponseDto(jwtService.generateToken(user));
    }

    @Transactional
    public Profile link(Long userId, String credential) {
        GoogleIdentity google = verifier.verify(credential);
        User user = user(userId);
        if (google.subject().equals(user.getGoogleSubject())) {
            return AccountService.toProfile(user);
        }
        if (user.getGoogleSubject() != null) {
            throw ApiException.conflict("GOOGLE_ALREADY_LINKED",
                    "Another Google account is already linked. Unlink it first.");
        }
        userRepository.findByGoogleSubject(google.subject()).ifPresent(other -> {
            throw ApiException.conflict("GOOGLE_IN_USE", "This Google account is already linked to a different account here.");
        });
        user.linkGoogle(google.subject(), google.email(), LocalDateTime.now());
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            // Two accounts racing to link the same Google account: the unique column decides
            throw ApiException.conflict("GOOGLE_IN_USE", "This Google account is already linked to a different account here.");
        }
        log.info("Linked Google account userId={}", userId);
        return AccountService.toProfile(user);
    }

    @Transactional
    public Profile unlink(Long userId) {
        User user = user(userId);
        if (user.getGoogleSubject() != null) {
            user.unlinkGoogle();
            log.info("Unlinked Google account userId={}", userId);
        }
        return AccountService.toProfile(user);
    }

    private User user(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "Sign in again to continue."));
    }
}
