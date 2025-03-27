package com.secor.subservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1")
public class MainRestController {

    private static final Logger log = LoggerFactory.getLogger(MainRestController.class);

    @Autowired
    PlanRepository planRepository;

    @Autowired
    AuthService authService;

    @Autowired
    SubService subService;
    @Autowired
    private SubscriptionRepository subscriptionRepository;
    @Autowired
    private Producer producer;

    @PostMapping("create/plan")
    public ResponseEntity<?> createPlan(@RequestBody Plan plan)
    {
        log.info("Received request to create plan: {}", plan);
        planRepository.save(plan);
        return ResponseEntity.ok("Plan Created");
    }


    @PostMapping("create/subs/{planid}")
    public ResponseEntity<?> createSub(@PathVariable("planid") String planid,
                                       @RequestHeader("Authorization") String token,
                                       @RequestBody MultiUserView multiUserView,
                                       HttpServletRequest request,
                                       HttpServletResponse response) throws JsonProcessingException, InterruptedException {

        log.info("Received request to create subscription: {}", multiUserView);
       // Thread.sleep(5000);

        if(authService.validateToken(token))
        {
            log.info("Token is valid: {}", token);
           // String responseKey = subService.createSub(multiUserView, planid, token);
            //return ResponseEntity.ok(responseKey);
            String responseKey = subService.createSubscription(planid, multiUserView, token);

            log.info("Setting up the Cookie for the Front-end");
            Cookie cookieStage1 = new Cookie("sub-service-stage-2", responseKey);
            cookieStage1.setMaxAge(300);
            log.info("Cookie set up successfully");

            response.addCookie(cookieStage1);

            log.info("Cookie added to the responsebody as strings {} {}",cookieStage1.getName(),cookieStage1.getValue());
            return ResponseEntity.ok("subresponse "+cookieStage1.getName()+" "+cookieStage1.getValue());
        }
        else
        {
            log.info("Token is invalid: {}", token);
            return ResponseEntity.status(401).build();
        }

    }

    @GetMapping("/get/subs/active")
    public ResponseEntity<?> getSubsActive()
    {
        return subscriptionRepository.findByStatusContainsIgnoreCase("active").size() > 0 ? ResponseEntity.ok(subscriptionRepository.findByStatusContainsIgnoreCase("active")) : ResponseEntity.ok("ZERO");
    }

    @PostMapping("/update/subs/{subid}")
    public ResponseEntity<?> updateSub(@PathVariable("subid") String subid) throws JsonProcessingException {
        log.info("Received request to update subscription: {}", subid);
        Subscription subscription = subscriptionRepository.findById(subid).get();
        subscription.setStatus("active");
        subscriptionRepository.save(subscription);
        producer.publishSubDatum(subid, "subscription status updated to active", "UPDATE", "ACTIVE");
        log.info("Subscription updated: {}", subscription);
        return ResponseEntity.ok("Subscription updated");
    }



}
