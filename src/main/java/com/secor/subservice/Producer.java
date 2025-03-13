package com.secor.subservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class Producer
{
    private static final Logger logger = LoggerFactory.getLogger(Producer.class);
    private static final String TOPIC = "sub-events";

    @Autowired //DEPENDENCY INJECTION PROMISE FULFILLED AT RUNTIME
    private KafkaTemplate<String, String> kafkaTemplate ;

//    @Autowired
//    SocialEvent1 socialEvent1;

    public void publishSubDatum(String subid,
                                String description,
                                String type,
                                String status
                                ) throws JsonProcessingException {

        SubDatum subDatum = new SubDatum();
        subDatum.setSubid(subid);
        subDatum.setType(type);
        subDatum.setDescription(description);
        subDatum.setServicename("sub-service");
        subDatum.setStatus(status);
//
        // convert to JSON
        ObjectMapper objectMapper = new ObjectMapper();
        String datum =  objectMapper.writeValueAsString(subDatum);
        logger.info("converted the subDatum object to JSON: "+datum);
//
        logger.info(String.format("#### -> Producing message -> %s", datum));
        this.kafkaTemplate.send(TOPIC, datum);
    }

}
