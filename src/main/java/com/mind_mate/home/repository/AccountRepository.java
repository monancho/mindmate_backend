package com.mind_mate.home.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mind_mate.home.entity.Account;

public interface AccountRepository extends JpaRepository<Account, Long>{
	public Optional<Account> findByUsername(String username);
	

}
