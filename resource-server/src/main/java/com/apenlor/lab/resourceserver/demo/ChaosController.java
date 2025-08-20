package com.apenlor.lab.resourceserver.demo;

import com.apenlor.lab.resourceserver.dto.ApiResponse;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * A demonstration controller that is only active when the "chaos" Spring profile is enabled.
 * <p>
 * Architectural Decision:
 * This controller is strictly for demonstration and debugging. By isolating it under a
 * Spring profile, we ensure that this non-production code is never active in a
 * production environment. This is a clean and safe pattern for including test harnesses,
 * demo endpoints, or experimental features in an application codebase.
 */
@RestController
@RequestMapping("/demo")
@Profile("chaos")
@Slf4j
@RequiredArgsConstructor
public class ChaosController {

    private final RandomnessProvider randomnessProvider;

    /**
     * An endpoint that simulates unreliable behavior.
     * It introduces a random processing delay and has a chance to fail by throwing an exception.
     * This is designed to generate interesting and variable data for our observability dashboards.
     *
     * @return A standard API response on success.
     * @throws InterruptedException if the thread is interrupted during sleep.
     */
    @GetMapping("/flaky-request")
    @Timed(value = "http.requests.demo", description = "Time taken to process a flaky demo request")
    public ResponseEntity<ApiResponse> getFlakyRequest() throws InterruptedException {
        // Introduce a random delay between 50 and 350 milliseconds.
        int delay = 50 + randomnessProvider.nextInt(300);
        TimeUnit.MILLISECONDS.sleep(delay);
        log.info("Simulating a delay of {}ms", delay);

        // There is a 20% chance that this request will fail.
        if (randomnessProvider.nextInt(5) == 0) {
            log.error("Simulating a random failure!");
            // This exception will be caught by our GlobalExceptionHandler
            // and result in a 500 Internal Server Error.
            throw new RuntimeException("Simulated internal server error!");
        }

        return ResponseEntity.ok(new ApiResponse("Successful but flaky response after " + delay + "ms."));
    }
}