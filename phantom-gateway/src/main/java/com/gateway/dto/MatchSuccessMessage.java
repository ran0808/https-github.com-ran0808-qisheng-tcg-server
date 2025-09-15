package com.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MatchSuccessMessage {
    private String playerId1;
    private String PlayerId2;
    private String sessionId;

}
