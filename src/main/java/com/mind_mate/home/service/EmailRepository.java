package com.mind_mate.home.service;

import org.springframework.data.repository.CrudRepository;

import com.mind_mate.home.entity.Email;

public interface EmailRepository extends CrudRepository<Email, String>{

}
