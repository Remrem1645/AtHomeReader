package com.rem.reader.Controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rem.reader.DTO.AddAccountRequestDTO;
import com.rem.reader.DTO.AuthAccountDTO;
import com.rem.reader.DTO.UpdateAccountPasswordDTO;
import com.rem.reader.DTO.UpdateAccountUsernameRequestDTO;
import com.rem.reader.Service.AccountService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @PostMapping("login")
    public ResponseEntity<?> login(@RequestBody AuthAccountDTO authAccountDTO, HttpSession session) {
        return accountService.authAccount(authAccountDTO, session);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpSession session) {
        return accountService.logout(session);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createAccount(@RequestBody AddAccountRequestDTO addAccountRequestDTO) {
        return accountService.createAccount(addAccountRequestDTO);
    }

    @PutMapping("/update-username")
    public ResponseEntity<?> updateUsername(
            @RequestBody UpdateAccountUsernameRequestDTO updateAccountUsernameRequestDTO, HttpSession session) {
        return accountService.updatetUsername(updateAccountUsernameRequestDTO, session);
    }

    @PutMapping("/update-password")
    public ResponseEntity<?> updatePassword(@RequestBody UpdateAccountPasswordDTO updateAccountPasswordDTO,
            HttpSession session) {
        return accountService.updatePassword(updateAccountPasswordDTO, session);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteAccount(HttpSession session) {
        return accountService.deleteAccount(session);
    }

    @DeleteMapping("/delete/{uuid}")
    public ResponseEntity<?> deleteAccountByUuid(
            @PathVariable UUID uuid, HttpSession session) {
        return accountService.deleteAccountAdmin(uuid, session);
    }
}
