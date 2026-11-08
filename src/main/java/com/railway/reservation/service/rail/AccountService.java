package com.railway.reservation.service.rail;

import com.railway.reservation.dto.AccountDtos.*;
import com.railway.reservation.exception.ApiException;
import com.railway.reservation.model.SavedTraveller;
import com.railway.reservation.model.User;
import com.railway.reservation.repository.SavedTravellerRepository;
import com.railway.reservation.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** The signed-in passenger's profile, password and saved travellers. */
@Service
@Transactional
public class AccountService {

    private final UserRepository userRepository;
    private final SavedTravellerRepository travellerRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(UserRepository userRepository, SavedTravellerRepository travellerRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.travellerRepository = travellerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private User user(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("USER_NOT_FOUND", "Sign in again to continue."));
    }

    @Transactional(readOnly = true)
    public Profile profile(Long userId) {
        return toProfile(user(userId));
    }

    public Profile update(Long userId, ProfileUpdate update) {
        User u = user(userId);
        u.setName(update.name().trim());
        u.setPhone(update.phone().trim());
        return toProfile(u);
    }

    public void changePassword(Long userId, PasswordChange change) {
        User u = user(userId);
        if (!passwordEncoder.matches(change.currentPassword(), u.getPasswordHash())) {
            throw ApiException.badRequest("WRONG_PASSWORD", "Your current password is not correct.");
        }
        if (change.newPassword().equals(change.currentPassword())) {
            throw ApiException.badRequest("SAME_PASSWORD", "Choose a password you have not used here before.");
        }
        u.setPasswordHash(passwordEncoder.encode(change.newPassword()));
    }

    @Transactional(readOnly = true)
    public List<Traveller> travellers(Long userId) {
        return travellerRepository.findByUserIdOrderByNameAsc(userId).stream().map(AccountService::toDto).toList();
    }

    public Traveller addTraveller(Long userId, TravellerInput in) {
        if (travellerRepository.countByUserId(userId) >= BookingService.MAX_SAVED_TRAVELLERS) {
            throw ApiException.conflict("TOO_MANY_TRAVELLERS", "You can save up to " + BookingService.MAX_SAVED_TRAVELLERS + " travellers.");
        }
        return toDto(travellerRepository.save(new SavedTraveller(user(userId), BookingService.tidyName(in.name()),
                in.age(), in.gender(), in.berthPreference(), in.food())));
    }

    public Traveller updateTraveller(Long userId, Long id, TravellerInput in) {
        SavedTraveller t = owned(userId, id);
        t.update(BookingService.tidyName(in.name()), in.age(), in.gender(), in.berthPreference(), in.food());
        return toDto(t);
    }

    public void deleteTraveller(Long userId, Long id) {
        travellerRepository.delete(owned(userId, id));
    }

    private SavedTraveller owned(Long userId, Long id) {
        return travellerRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> ApiException.notFound("TRAVELLER_NOT_FOUND", "That traveller is not in your list."));
    }

    public static Profile toProfile(User u) {
        return new Profile(u.getId(), u.getName(), u.getEmail(), u.getPhone(),
                u.getRole().name().replace("ROLE_", ""), u.getCreatedAt(), u.getGoogleEmail());
    }

    static Traveller toDto(SavedTraveller t) {
        return new Traveller(t.getId(), t.getName(), t.getAge(), t.getGender().name(), t.getBerthPreference().name(),
                t.getFood() == null ? null : t.getFood().name());
    }
}
