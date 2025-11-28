package com.mind_mate.home.service;

import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.mind_mate.home.entity.Account;
import com.mind_mate.home.repository.AccountRepository;
@Service
public class JwtAccountService implements UserDetailsService{
	@Autowired
	private AccountRepository accountRepository;
	@Override
	// jwt null 문제 예외처리
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		// TODO Auto-generated method stub
		Account account = accountRepository.findByUsername(username)
				.orElseThrow(()-> new UsernameNotFoundException("등록된 사용자 없음"));
		
		return new org.springframework.security.core.userdetails.User(
				account.getUsername(),
				account.getPassword(),
				new ArrayList<>()
				);
	}
}
