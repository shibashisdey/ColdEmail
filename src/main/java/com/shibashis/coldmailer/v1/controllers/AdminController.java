package com.shibashis.coldmailer.v1.controllers;

import com.shibashis.coldmailer.v1.dto.admin.AdminFailedEmailView;
import com.shibashis.coldmailer.v1.dto.admin.AdminHrContactView;
import com.shibashis.coldmailer.v1.dto.admin.AdminEmailAccountStatusRequest;
import com.shibashis.coldmailer.v1.dto.admin.AdminEmailAccountView;
import com.shibashis.coldmailer.v1.dto.admin.AdminPlatformStatsView;
import com.shibashis.coldmailer.v1.dto.admin.AdminSettingUpdateRequest;
import com.shibashis.coldmailer.v1.dto.admin.AdminUserStatusRequest;
import com.shibashis.coldmailer.v1.dto.admin.AdminUserView;
import com.shibashis.coldmailer.v1.dto.admin.AdminWorkerHealthView;
import com.shibashis.coldmailer.v1.services.AdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.DeleteMapping;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/hr-contacts")
    public ResponseEntity<List<AdminHrContactView>> listHrContacts(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(adminService.listHrContacts(q));
    }

    @GetMapping("/failed-emails")
    public ResponseEntity<List<AdminFailedEmailView>> listFailedEmails() {
        return ResponseEntity.ok(adminService.listFailedEmails());
    }

    @GetMapping("/users")
    public ResponseEntity<List<AdminUserView>> listUsers() {
        return ResponseEntity.ok(adminService.listUsers());
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<AdminUserView> updateUserStatus(@PathVariable Long id,
                                                          @Valid @RequestBody AdminUserStatusRequest request) {
        return ResponseEntity.ok(adminService.updateUserStatus(id, request));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<AdminUserView> removeUser(@PathVariable Long id) {
        return ResponseEntity.ok(adminService.removeUser(id));
    }

    @GetMapping("/email-accounts")
    public ResponseEntity<List<AdminEmailAccountView>> listEmailAccounts() {
        return ResponseEntity.ok(adminService.listEmailAccounts());
    }

    @PatchMapping("/email-accounts/{id}/status")
    public ResponseEntity<AdminEmailAccountView> updateEmailAccountStatus(@PathVariable Long id,
                                                                           @Valid @RequestBody AdminEmailAccountStatusRequest request) {
        return ResponseEntity.ok(adminService.updateEmailAccountStatus(id, request));
    }

    @GetMapping("/stats")
    public ResponseEntity<AdminPlatformStatsView> platformStats() {
        return ResponseEntity.ok(adminService.platformStats());
    }

    @GetMapping("/worker-health")
    public ResponseEntity<AdminWorkerHealthView> workerHealth() {
        return ResponseEntity.ok(adminService.workerHealth());
    }

    @GetMapping("/settings")
    public ResponseEntity<Map<String, String>> settings() {
        return ResponseEntity.ok(adminService.listSettings());
    }

    @PatchMapping("/settings")
    public ResponseEntity<Map<String, String>> updateSetting(@Valid @RequestBody AdminSettingUpdateRequest request) {
        return ResponseEntity.ok(adminService.upsertSetting(request));
    }
}
