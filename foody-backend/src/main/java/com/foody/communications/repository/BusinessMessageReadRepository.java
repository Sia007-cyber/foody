package com.foody.communications.repository;
import com.foody.communications.entity.*; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface BusinessMessageReadRepository extends JpaRepository<BusinessMessageRead,BusinessMessageReadId>{Set<BusinessMessageRead> findByBusinessIdAndMessageIdIn(Long businessId,Collection<Long> messageIds);}
