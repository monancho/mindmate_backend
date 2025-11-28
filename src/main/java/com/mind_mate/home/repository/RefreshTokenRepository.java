package com.mind_mate.home.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import com.mind_mate.home.entity.RefreshToken;


public interface RefreshTokenRepository extends CrudRepository<RefreshToken, Long>{
	Optional<RefreshToken> findByAccessToken(String accessToken);
}
