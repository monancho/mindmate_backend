package com.mind_mate.home.service;


import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.mind_mate.home.entity.RefreshToken;
import com.mind_mate.home.repository.RefreshTokenRepository;


import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private final RefreshTokenRepository refreshTokenRepository;
	
	@Transactional
	public void saveTokenInfo(Long userId, String refreshToken, String accessToken) {
		try {
			
			refreshTokenRepository.save(new RefreshToken(userId, refreshToken, accessToken));
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
	}
	
	@Transactional
	public void saveTokenEntity(RefreshToken tokenEntity) {
		 try {
	            refreshTokenRepository.save(tokenEntity);
	        } catch (DataAccessException e) {
	            e.printStackTrace();
	        }
	}
	
	
	
	@Transactional
	public void removeRefreshToken(String accessToken) {
		try {
			
			refreshTokenRepository.findByAccessToken(accessToken)
			.ifPresent(refreshToken -> refreshTokenRepository.delete(refreshToken));
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
	}
	
	public Optional<RefreshToken> findByAccessToken(String accessToken) {
		if (accessToken == null || accessToken.isBlank()) {
	        return Optional.empty();
	    }
		
		try {
			
			return refreshTokenRepository.findByAccessToken(accessToken);
		} catch (DataAccessException e) {
			e.printStackTrace();
			return Optional.empty();
			// TODO: handle exception
		}
	}

	
	
}
