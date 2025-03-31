package com.rem.reader.Service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rem.reader.DTO.AddAccountRequestDTO;
import com.rem.reader.DTO.AuthAccountDTO;
import com.rem.reader.DTO.UpdateAccountPasswordDTO;
import com.rem.reader.DTO.UpdateAccountUsernameRequestDTO;
import com.rem.reader.Models.Account;
import com.rem.reader.Repo.AccountRepo;

import jakarta.servlet.http.HttpSession;

@Service
@Transactional
public class AccountService {
    
    @Autowired
    AccountRepo accountRepo;

    private static final String ACCESS_CODE = "REM06";

    public ResponseEntity<?> authAccount(AuthAccountDTO authAccountDTO, HttpSession session) {
        if (authAccountDTO.getUsername() == null || authAccountDTO.getPassword() == null) {
            return ResponseEntity.badRequest().body("Username and password must be provided");
        }

        Account account = accountRepo.findByUsername(authAccountDTO.getUsername());
        if (account == null || !account.checkPassword(authAccountDTO.getPassword())) {
            return ResponseEntity.status(401).body("Invalid username or password");
        }

        session.setAttribute("userUuid", account.getUuid());

        return ResponseEntity.ok("Authenticated successfully" + account.getUuid());
    }

    public ResponseEntity<?> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.ok("Logged out successfully");
    }
    
    
    public ResponseEntity<?> createAccount(AddAccountRequestDTO addAccountRequestDTO) {
        String accessCode = addAccountRequestDTO.getAccessCode();
        String username = addAccountRequestDTO.getUsername();
        String password = addAccountRequestDTO.getPassword();
        
        if (!accessCode.equals(ACCESS_CODE)) {
            return ResponseEntity.badRequest().body("Invalid access code");
        }

        if (username.isBlank() || username == null || password.isBlank() || password == null) {
            return ResponseEntity.badRequest().body("Username and password must be provided");
        }

        if (accountRepo.findByUsername(username) != null) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        Account newAccount = new Account();
        newAccount.setUsername(username);
        newAccount.setPassword(password);
        newAccount.setUuid(UUID.randomUUID());
        newAccount.setAdmin(false); 

        accountRepo.save(newAccount);

        return ResponseEntity.ok("Account created successfully");
    }


    public ResponseEntity<?> updatetUsername(UpdateAccountUsernameRequestDTO newName, HttpSession session ) {
        UUID uuid = (UUID) session.getAttribute("userUuid");
        if (uuid == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Account account = accountRepo.findByUuid(uuid);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }

        Account existingAccount = accountRepo.findByUsername(newName.getNewUsername());
        if (existingAccount != null) {
            return ResponseEntity.badRequest().body("Username already exists");
        }

        account.setUsername(newName.getNewUsername());
        accountRepo.save(account); 

        return ResponseEntity.ok("Username updated to " + newName.getNewUsername());
    }

    public ResponseEntity<?> updatePassword(UpdateAccountPasswordDTO newPassword, HttpSession session) {
        UUID uuid = (UUID) session.getAttribute("userUuid");
        if (uuid == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Account account = accountRepo.findByUuid(uuid);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }

        account.setPassword(newPassword.getNewPassword());
        accountRepo.save(account); 

        return ResponseEntity.ok("Password updated successfully");
    }

    public ResponseEntity<?> deleteAccount(HttpSession session) {
        UUID uuid = (UUID) session.getAttribute("userUuid");
        if (uuid == null) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Account account = accountRepo.findByUuid(uuid);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }

        accountRepo.delete(account); 
        session.invalidate();

        return ResponseEntity.ok("Account deleted successfully");
    }

    public ResponseEntity<?> deleteAccountAdmin(UUID uuidToRemove, HttpSession session) {
        UUID uuid = (UUID) session.getAttribute("userUuid");
        if (uuid == null || !accountRepo.isAdminByUuid(uuid)) {
            return ResponseEntity.status(401).body("Unauthorized");
        }

        Account account = accountRepo.findByUuid(uuidToRemove);
        if (account == null) {
            return ResponseEntity.notFound().build();
        }

        accountRepo.delete(account); 

        return ResponseEntity.ok("Account deleted successfully");
    }
}
