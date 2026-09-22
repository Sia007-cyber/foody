package com.foody.communications.dto;
import jakarta.validation.constraints.*;
public final class CommunicationRequests {private CommunicationRequests(){}
 public record CreateTicket(@NotBlank @Size(max=160) String subject,@NotBlank @Size(max=4000) String message){}
 public record Reply(@NotBlank @Size(max=4000) String message){}
 public record DirectMessage(@NotNull Long businessId,@NotBlank @Size(max=160) String subject,@NotBlank @Size(max=4000) String message){}
 public record Broadcast(@NotBlank @Size(max=160) String subject,@NotBlank @Size(max=4000) String message){}
}
