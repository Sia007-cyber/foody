package com.foody.communications.repository;
import com.foody.communications.entity.BusinessMessage; import java.time.Instant; import java.util.*; import org.springframework.data.domain.*; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param;
public interface BusinessMessageRepository extends JpaRepository<BusinessMessage,Long>{
 @Query("SELECT m FROM BusinessMessage m WHERE (m.messageType=com.foody.communications.entity.BusinessMessageType.BROADCAST AND :businessId<=m.audienceBusinessIdCutoff) OR m.targetBusinessId=:businessId ORDER BY m.createdAt DESC,m.id DESC") Slice<BusinessMessage> inbox(@Param("businessId")Long businessId,Pageable pageable);
 @Query("SELECT m FROM BusinessMessage m WHERE m.id=:id AND ((m.messageType=com.foody.communications.entity.BusinessMessageType.BROADCAST AND :businessId<=m.audienceBusinessIdCutoff) OR m.targetBusinessId=:businessId)") Optional<BusinessMessage> findVisible(@Param("id")Long id,@Param("businessId")Long businessId);
 Slice<BusinessMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);
 @Query("SELECT COUNT(m) FROM BusinessMessage m WHERE ((m.messageType=com.foody.communications.entity.BusinessMessageType.BROADCAST AND :businessId<=m.audienceBusinessIdCutoff) OR m.targetBusinessId=:businessId) AND NOT EXISTS(SELECT r.messageId FROM BusinessMessageRead r WHERE r.messageId=m.id AND r.businessId=:businessId)") long countUnread(@Param("businessId")Long businessId);
}
