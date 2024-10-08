package com.fintech.auth.service.impl;

import com.fintech.auth.dto.AuthDTO;
import com.fintech.auth.dto.RefreshDTO;
import com.fintech.auth.dto.UserSignInDTO;
import com.fintech.auth.dto.UserSignUpDTO;
import com.fintech.auth.model.AuthUser;
import com.fintech.auth.model.Role;
import com.fintech.auth.repository.AuthUserRepository;
import com.fintech.auth.repository.RoleRepository;
import com.fintech.auth.service.AuthService;
import com.fintech.auth.util.JWTUtil;
import com.fintech.auth.util.RolesEnum;
import com.onedlvb.advice.LogLevel;
import com.onedlvb.advice.annotation.AuditLog;
import jakarta.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Set;

/**
 * An implementation of {@link AuthService} interface.
 * @author Matushkin Anton
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService  {

    @NonNull
    private final AuthUserRepository authUserRepository;

    @NonNull
    private final JWTUtil jwtUtil;

    @NonNull
    private final PasswordEncoder passwordEncoder;

    @NonNull
    private final AuthenticationManager authenticationManager;

    @NonNull
    private final RoleRepository roleRepository;

    @Value("${auth.secret.access-expiration-time}")
    private long expirationTime;

    @Override
    @Transactional
    @AuditLog(logLevel = LogLevel.INFO)
    public AuthDTO signup(UserSignUpDTO userDTO) {
        AuthDTO responseAuthDTO = new AuthDTO();
        try {
            AuthUser user = new AuthUser();
            user.setUsername(userDTO.getUsername());
            user.setEmail(userDTO.getEmail());
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

            Set<Role> roles = Set.of(roleRepository.findById(RolesEnum.USER.name()).orElse(
                    new Role("USER",
                            "Может просматривать справочную информацию" +
                                    " (contractor/county/all, deal/deal-status и т.д.)",
                            true)));

            user.setRoles(roles);

            authUserRepository.save(user);

            responseAuthDTO.setMessage("User saved successfully.");
            responseAuthDTO.setStatusCode(200);
        } catch (Exception e) {
            responseAuthDTO.setMessage(e.getMessage());
            responseAuthDTO.setStatusCode(500);
        }
        return responseAuthDTO;
    }

    @Override
    @Transactional
    @AuditLog(logLevel = LogLevel.INFO)
    public AuthDTO signIn(UserSignInDTO requestAuthDTO) {
        AuthDTO responseAuthDTO = new AuthDTO();
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                    requestAuthDTO.getUsername(), requestAuthDTO.getPassword()));
            AuthUser user = authUserRepository.findByUsername(requestAuthDTO.getUsername()).orElseThrow();
            fillAuthDTO(responseAuthDTO, user, "Successfully signed in.");
        } catch (Exception e) {
            responseAuthDTO.setMessage(e.getMessage());
            responseAuthDTO.setStatusCode(500);
        }
        return responseAuthDTO;
    }

    @Override
    @Transactional
    @AuditLog(logLevel = LogLevel.INFO)
    public AuthDTO refreshToken(RefreshDTO refreshTokenRequest) {
        AuthDTO responseAuthDTO = new AuthDTO();
        String username = jwtUtil.getUsernameFromToken(refreshTokenRequest.getRefreshToken());
        AuthUser user = authUserRepository.findByUsername(username).orElseThrow();
        if (jwtUtil.isTokenValid(refreshTokenRequest.getRefreshToken(), user)) {
            fillAuthDTO(responseAuthDTO, user, "Successfully refreshed token.");
        } else {
            responseAuthDTO.setStatusCode(500);
        }
        return responseAuthDTO;

    }

    /**
     * Fills the provided {@link AuthDTO} with the authentication details of the specified user.
     * @param responseAuthDTO The {@link AuthDTO} to be filled with the user's authentication details.
     * @param user            The authenticated user.
     * @param message         The message to be set in the {@link AuthDTO}.
     */
    private void fillAuthDTO(AuthDTO responseAuthDTO, AuthUser user, String message) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(new HashMap<>(), user);
        responseAuthDTO.setStatusCode(200);
        responseAuthDTO.setAccessToken(accessToken);
        responseAuthDTO.setRefreshToken(refreshToken);
        responseAuthDTO.setExpirationTimeMils(String.valueOf(expirationTime));
        responseAuthDTO.setMessage(message);

    }

}
