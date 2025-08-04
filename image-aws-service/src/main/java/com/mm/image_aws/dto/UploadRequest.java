package com.mm.image_aws.dto;


import lombok.Data;

import java.util.List;

@Data
public class UploadRequest {
    private List<String> urls;
}


