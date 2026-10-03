package com.group2.rms.requisition.service;

import com.group2.rms.requisition.entity.JobRequisition;
import com.group2.rms.user.entity.User;
import com.group2.rms.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import java.util.*;
@Component @RequiredArgsConstructor
public class RequisitionAccess {
    private final UserRepository users;
    public User actor() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null) throw new AccessDeniedException("Sign in to continue.");
        var user=users.findByUsernameIgnoreCase(auth.getName()).filter(u->"Active".equals(u.getAccountStatus())).orElseThrow(()->new AccessDeniedException("Active account required."));
        if(!Set.of("Hiring Manager","Director","HR","System Admin").contains(role(user))) throw new AccessDeniedException("Requisitions are not available for this role.");
        return user;
    }
    public String role(User user) { return user.getRole().getRoleName(); }
    public boolean owns(User user,JobRequisition req) { return Objects.equals(user.getUserId(),req.getHiringManager().getUserId()); }
    public boolean canCreate(User user) { return Set.of("Hiring Manager","System Admin").contains(role(user)); }
    public boolean canEdit(User user,JobRequisition req) { return (owns(user,req)||"System Admin".equals(role(user)))&&Set.of("Draft","Rejected").contains(req.getApprovalStatus()); }
    public boolean canDecide(User user,JobRequisition req) { return "Director".equals(role(user))&&!owns(user,req)&&"Pending_Director".equals(req.getApprovalStatus()); }
    public void requireCreate(User user) { if(!canCreate(user)) throw new AccessDeniedException("Only Hiring Managers and administrators can create requests."); }
    public void requireView(User user,JobRequisition req) {
        if("System Admin".equals(role(user))||owns(user,req)) return;
        if(Set.of("Director","HR").contains(role(user))&&!"Draft".equals(req.getApprovalStatus())) return;
        throw new AccessDeniedException("This request is outside your scope.");
    }
    public void requireEdit(User user,JobRequisition req) { requireView(user,req); if(!canEdit(user,req)) throw new AccessDeniedException("Only a draft or rejected request can be edited or deleted."); }
}
