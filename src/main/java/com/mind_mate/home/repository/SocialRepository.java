package com.mind_mate.home.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mind_mate.home.entity.Social;

public interface SocialRepository  extends JpaRepository<Social, Long>{
	Optional<Social> findByProviderAndProviderUserId(String provider, String providerUserId);

}
