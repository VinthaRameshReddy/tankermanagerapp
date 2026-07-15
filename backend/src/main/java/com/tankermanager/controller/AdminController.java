package com.tankermanager.controller;

import com.tankermanager.dto.ApiDtos.*;
import com.tankermanager.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PostMapping("/operators")
    @ResponseStatus(HttpStatus.CREATED)
    public OperatorResponse createOperator(@Valid @RequestBody CreateOperatorRequest request) {
        return adminService.createOperator(request);
    }

    @GetMapping("/operators")
    public List<OperatorResponse> listOperators() {
        return adminService.listOperators();
    }

    @PatchMapping("/operators/{id}/active")
    public OperatorResponse setActive(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        Boolean active = body.get("active");
        return adminService.setActive(id, active != null && active);
    }
}
