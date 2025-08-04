package com.mm.user.dto;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

@Data
public class SignUpRequest {
    @CsvBindByName
    private String username;
    @CsvBindByName
    private String name;
    @CsvBindByName
    private String email;
    @CsvBindByName
    private String password;
}