package com.secor.subservice;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

@Document(collection = "subscriptions")
@Getter @Setter
public class Subscription
{
    private String subid;
    private MultiUserView users;
    private String planid;
    private Date startdate;
    private String status; //unpaid, active, inactive, suspended
}
