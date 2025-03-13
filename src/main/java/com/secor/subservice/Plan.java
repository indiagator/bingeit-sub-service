package com.secor.subservice;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "plans")
@Getter @Setter
public class Plan
{
    private String planid;
    private String planname;
    private String frequency;
    private String amount;
}
