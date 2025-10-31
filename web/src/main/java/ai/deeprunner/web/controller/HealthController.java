package ai.deeprunner.web.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for document search operations
 */
@RestController
@RequestMapping
public class HealthController {


    /**
     * Health check endpoint
     */
    @GetMapping("/public/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Document Search Service is running");
    }
}

