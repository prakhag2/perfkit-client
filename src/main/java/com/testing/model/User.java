package com.testing.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "user")
public class User implements Serializable {

  private static final long serialVersionUID = -171319613961359443L;

  @Id
  @Column(name = "user_id")
  private String userId;

  @OneToMany(mappedBy = "user")
  private Set<Job> jobs = new HashSet<>();

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }
  
  public Set<Job> getJobs() {
	  return jobs;
  }
  
  public void setJobs(Set<Job> jobs) {
	  this.jobs = jobs;
  }
  
  public User() {
    super();
  }
}