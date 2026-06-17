package com.example.facility.identity.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.facility.shared.apiresponse.ApiResponse;

@RestController
@RequestMapping("/api/v1/identity")
public class IdentityController {
    
    @GetMapping("/hello")
    @ResponseStatus
    public ApiResponse<String> hello(@RequestParam String param) {
        return new ApiResponse<>("Hello " + param + ", this is a protected endpoint.");
    }
}

