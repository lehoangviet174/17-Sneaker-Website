package com.vuhien.application.model.dto;

import lombok.Data;

@Data
public class PaymentRestDTO {
    private String status;
    private String message;
    private String URL;
}