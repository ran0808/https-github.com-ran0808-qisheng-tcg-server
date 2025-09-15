package com.match.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class MatchRemoveMessage implements Serializable {
    private String playerId;
    private long timestamp;
}