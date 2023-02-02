package com.testing.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.testing.model.Job;

@Repository
public interface JobRepository extends JpaRepository<Job,Integer> {}
