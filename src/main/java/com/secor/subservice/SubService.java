package com.secor.subservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import java.util.Random;

@Service
public class SubService
{
    private static final Logger log = LoggerFactory.getLogger(MainRestController.class);

    @Autowired
    @Qualifier("plain-old-web-client")
    WebClient webClient;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Autowired
    RedisTemplate<String, Object> redisTemplate;

    @Autowired
    Producer producer;

        public String createSubscription(String planid, MultiUserView multiUserView, String token) throws JsonProcessingException {
            log.info("Creating subscription for plan: {}", planid);
            Subscription subscription = new Subscription();
            subscription.setPlanid(planid);
            subscription.setUsers(multiUserView);
            subscription.setSubid(String.valueOf(new Random().nextInt(1000)));
            subscription.setStatus("unpaid");
            subscription.setStartdate(new java.util.Date());
            subscriptionRepository.save(subscription);
            producer.publishSubDatum(subscription.getSubid(), "subscription created payment creation in progress", "CREATE", "PROCESSING");
            log.info("Saving subscription: {}", subscription);

            Mono<String> paymentServiceResponse = webClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/create/payment/{subid}")
                            .build(subscription.getSubid()))
                    .header("Authorization", token)
                    .retrieve()
                    .bodyToMono(String.class); // This is an Async Request

            String responseKey = String.valueOf(new Random().nextInt(1000)); // this is the key that we will return from this method
            log.info("Response Key: {}", responseKey);
            redisTemplate.opsForValue().set(responseKey,"stage2 complete");

            /// SETUP A HANDLER FOR THE EVENTUAL RESPONSE
            paymentServiceResponse.subscribe(
                    (response) ->
                    {
                        log.info(response+" from the payment service");
                        // MENU CREATION LOGIC TO BE IMPLEMENTED HERE
                        // AND PUT THE RESPONSE IN REDIS
                        try {
                            producer.publishSubDatum(subscription.getSubid(), "subscription created payment created", "UPDATE", "UNPAID");
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }

                        redisTemplate.opsForValue().set(responseKey,"payresponse "+response);
                    },
                    error ->
                    {
                        log.info("error processing the response "+error.getMessage());
                        redisTemplate.opsForValue().set(responseKey,"error "+error.getMessage());
                    });
            /// END OF HANDLER

            return responseKey; // Interim Response

        }
}
