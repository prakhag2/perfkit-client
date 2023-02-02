package com.testing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.testing.model.User;

@Repository
public interface UserRepository extends JpaRepository<User,String> {}
