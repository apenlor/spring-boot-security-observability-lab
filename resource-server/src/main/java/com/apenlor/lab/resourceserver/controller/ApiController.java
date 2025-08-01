package com.apenlor.lab.resourceserver.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ApiController {

    @GetMapping("/public/info")
    public ResponseEntity<String> getPublicInfo() {
        String body = "This is PUBLIC information. Anyone can see this.";
        return ResponseEntity.ok(body);
    }

    @GetMapping("/secure/data")
    public ResponseEntity<String> getSecureData(Authentication authentication) {
        String username = authentication.getName();
        String body = String.format("This is SECURE data for user: %s. You should only see this if you are authenticated.", username);
        return ResponseEntity.ok(body);
    }
}