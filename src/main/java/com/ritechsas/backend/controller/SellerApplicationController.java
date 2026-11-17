package com.ritechsas.backend.controller;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import com.ritechsas.backend.entity.*;
import com.ritechsas.backend.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

@RestController
@RequestMapping("/api/seller-applications")
public class SellerApplicationController {
    public record ApplicationRequest(@NotBlank @Size(max=180) String businessName, @NotBlank @Size(max=150) String contactName,
        @NotBlank @Email @Size(max=150) String email, @Size(max=30) String phone, @NotBlank @Size(max=100) String country,
        @NotBlank String productType, @Size(max=4000) String businessDescription) {}
    public record ReviewRequest(@NotBlank String decision, @Size(max=500) String rejectionReason) {}
    public record ApplicationView(Long id, Long userId, String username, String businessName, String contactName, String email,
        String phone, String country, String productType, String businessDescription, String status, String rejectionReason,
        Long producerId, java.time.LocalDateTime createdAt) {}
    private final SellerApplicationRepository applications; private final UserRepository users; private final RoleRepository roles;
    private final ProducerRepository producers; private final AuditLogRepository audit;
    public SellerApplicationController(SellerApplicationRepository a, UserRepository u, RoleRepository r, ProducerRepository p, AuditLogRepository audit){this.applications=a;this.users=u;this.roles=r;this.producers=p;this.audit=audit;}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @Transactional
    public ApplicationView apply(Authentication auth, @Valid @RequestBody ApplicationRequest request){
        User user=current(auth); if(!user.hasRole("COMPRADOR")) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo los compradores pueden solicitar convertirse en vendedores");
        if(user.hasRole("VENDEDOR")) throw new ResponseStatusException(HttpStatus.CONFLICT,"Tu cuenta ya tiene el rol VENDEDOR");
        if(applications.existsByUserIdAndStatus(user.getId(),SellerApplication.Status.PENDING)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Ya tienes una solicitud pendiente");
        String type=request.productType().trim().toUpperCase(); if(!List.of("COFFEE","COCOA","BOTH").contains(type)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tipo de producto inválido");
        SellerApplication a=new SellerApplication(); a.setUserId(user.getId()); a.setBusinessName(request.businessName().trim()); a.setContactName(request.contactName().trim()); a.setEmail(request.email().trim().toLowerCase()); a.setPhone(request.phone()); a.setCountry(request.country().trim()); a.setProductType(type); a.setBusinessDescription(request.businessDescription());
        return view(applications.save(a));
    }
    @GetMapping("/mine") public List<ApplicationView> mine(Authentication auth){User u=current(auth); return applications.findByUserIdOrderByCreatedAtDesc(u.getId()).stream().map(this::view).toList();}
    @GetMapping("/admin") public List<ApplicationView> all(Authentication auth){requireAdmin(current(auth)); return applications.findAllByOrderByCreatedAtDesc().stream().map(this::view).toList();}
    @PatchMapping("/admin/{id}/review") @Transactional
    public ApplicationView review(Authentication auth,@PathVariable Long id,@Valid @RequestBody ReviewRequest request){
        User admin=current(auth); requireAdmin(admin); SellerApplication a=applications.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Solicitud no encontrada"));
        if(a.getStatus()!=SellerApplication.Status.PENDING) throw new ResponseStatusException(HttpStatus.CONFLICT,"La solicitud ya fue revisada");
        String decision=request.decision().trim().toUpperCase(); User applicant=users.findById(a.getUserId()).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Usuario solicitante no encontrado"));
        a.setReviewedBy(admin.getId()); a.setReviewedAt(LocalDateTime.now());
        if("APPROVE".equals(decision)){
            Role seller=roles.findByName("VENDEDOR").orElseThrow(()->new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"No existe el rol VENDEDOR"));
            Producer producer=producers.findByUserId(applicant.getId()).orElseGet(Producer::new); producer.setName(a.getBusinessName()); producer.setContact(a.getContactName()); producer.setEmail(a.getEmail()); producer.setPhone(a.getPhone()); producer.setProductType(a.getProductType()); producer.setUserId(applicant.getId()); producer=producers.save(producer);
            applicant.getRoles().add(seller); applicant.setRole(seller); users.save(applicant); a.setProducerId(producer.getId()); a.setStatus(SellerApplication.Status.APPROVED); a.setRejectionReason(null);
        } else if("REJECT".equals(decision)){if(request.rejectionReason()==null||request.rejectionReason().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Indica el motivo del rechazo"); a.setStatus(SellerApplication.Status.REJECTED); a.setRejectionReason(request.rejectionReason().trim());}
        else throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La decisión debe ser APPROVE o REJECT");
        SellerApplication saved=applications.save(a); AuditLog log=new AuditLog(); log.setActorUserId(admin.getId()); log.setAction("SELLER_APPLICATION_"+saved.getStatus().name()); log.setEntityType("seller_applications"); log.setEntityId(saved.getId()); audit.save(log); return view(saved);
    }
    private User current(Authentication auth){if(auth==null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);return users.findByUsername(auth.getName()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));}
    private void requireAdmin(User u){if(!u.hasRole("ADMINISTRADOR"))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo el administrador puede revisar solicitudes");}
    private ApplicationView view(SellerApplication a){String username=users.findById(a.getUserId()).map(User::getUsername).orElse("");return new ApplicationView(a.getId(),a.getUserId(),username,a.getBusinessName(),a.getContactName(),a.getEmail(),a.getPhone(),a.getCountry(),a.getProductType(),a.getBusinessDescription(),a.getStatus().name(),a.getRejectionReason(),a.getProducerId(),a.getCreatedAt());}
}
