package com.foody.auth.repository;

import com.foody.auth.entity.ImpersonationSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImpersonationSessionRepository extends JpaRepository<ImpersonationSession, String> { }
