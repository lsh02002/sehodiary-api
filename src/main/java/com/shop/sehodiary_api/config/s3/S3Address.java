package com.shop.sehodiary_api.config.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class S3Address {
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    @Value("${cloud.aws.region.static}")
    private String region;

    public String siteAddress() {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com";
    }
}
