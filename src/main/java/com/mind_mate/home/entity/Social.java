package com.mind_mate.home.entity;

import java.time.LocalDateTime;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(uniqueConstraints = {
		@UniqueConstraint(columnNames = {"provider", "providerUserId"})
})
@Getter
@Setter
public class Social {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false)
	private String providerUserId;
	
	@Column(nullable = false)
    private String provider;
	
	@OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;
}
