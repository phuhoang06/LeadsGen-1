package com.mm.image_aws.dto;

import lombok.Data;

@Data
public class SignUpRequest {
    private String username;
    private String name;
    private String email;
    private String password;
}