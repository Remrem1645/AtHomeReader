package com.rem.reader.Repo;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.rem.reader.Models.Account;

public interface AccountRepo extends JpaRepository<Account, Long> {

    /**
     * Find an account by its username.
     * @param username The username of the account to find.
     * @return The Account entity if found, otherwise null.
     */
    Account findByUsername(String username);


    /**
     * Find an account by its UUID.
     * @param uuid The UUID of the account to find.
     * @return The Account entity if found, otherwise null.
     */
    Account findByUuid(UUID uuid);


    /**
     * Check if user is admin by UUID
     * @param uuid The UUID of the account to check.
     * @return true if the account is admin, otherwise false.
     */
    @Query(value = "SELECT admin FROM account WHERE UUID = ?1", nativeQuery = true)
    boolean isAdminByUuid(UUID uuid);
}
