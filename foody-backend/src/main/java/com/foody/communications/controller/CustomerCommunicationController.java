package com.foody.communications.controller;
import com.foody.auth.security.FoodyUserPrincipal; import com.foody.communications.dto.*; import com.foody.communications.service.CommunicationService; import jakarta.validation.Valid; import org.springframework.http.HttpStatus; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/customer/communications") @PreAuthorize("hasRole('CUSTOMER')") public class CustomerCommunicationController {
 private final CommunicationService service; public CustomerCommunicationController(CommunicationService s){service=s;}
 @PostMapping("/tickets") @ResponseStatus(HttpStatus.CREATED) public CommunicationResponses.TicketDetail create(@AuthenticationPrincipal FoodyUserPrincipal p,@Valid @RequestBody CommunicationRequests.CreateTicket r){return service.createCustomerTicket(p.getUserId(),r);}
 @GetMapping("/tickets") public CommunicationResponses.Page<CommunicationResponses.TicketSummary> tickets(@AuthenticationPrincipal FoodyUserPrincipal p,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="20")int limit){return service.customerTickets(p.getUserId(),page,limit);}
 @GetMapping("/tickets/{id}") public CommunicationResponses.TicketDetail ticket(@AuthenticationPrincipal FoodyUserPrincipal p,@PathVariable Long id){return service.customerTicket(p.getUserId(),id);}
 @PostMapping("/tickets/{id}/replies") @ResponseStatus(HttpStatus.CREATED) public CommunicationResponses.TicketDetail reply(@AuthenticationPrincipal FoodyUserPrincipal p,@PathVariable Long id,@Valid @RequestBody CommunicationRequests.Reply r){return service.customerReply(p.getUserId(),id,r);}
 @GetMapping("/unread-count") public CommunicationResponses.UnreadCount unread(@AuthenticationPrincipal FoodyUserPrincipal p){return service.customerUnread(p.getUserId());}
}
