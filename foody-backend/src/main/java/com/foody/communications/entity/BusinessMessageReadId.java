package com.foody.communications.entity;
import java.io.Serializable; import java.util.Objects;
public class BusinessMessageReadId implements Serializable { public Long messageId; public Long businessId; public BusinessMessageReadId(){} public BusinessMessageReadId(Long m,Long b){messageId=m;businessId=b;} public boolean equals(Object o){return o instanceof BusinessMessageReadId x&&Objects.equals(messageId,x.messageId)&&Objects.equals(businessId,x.businessId);} public int hashCode(){return Objects.hash(messageId,businessId);} }
