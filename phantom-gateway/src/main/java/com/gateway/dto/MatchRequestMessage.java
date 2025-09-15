package com.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// MatchRequestMessage.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MatchRequestMessage implements Serializable {
    private String token;
    private Long timestamp;
    private String playerId;
}