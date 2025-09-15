package com.gateway.config;
import com.game.common.protocol.Opcode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "gateway.routing")
public class ServiceMappingConfig {
    @Getter
    @Setter
    private Map<Short,String > opcodeToService = new HashMap<>();
    //获取操作码对应的服务路径
    public String getServicePath(short opcode){
        return opcodeToService.getOrDefault(opcode,null);
    }
    public boolean requiresRouting(short opcode){
        return opcodeToService.containsKey(opcode);}
    //判断指定操作码是否需要认证
    public boolean requiresAuthentication(short opcode) {
        return opcode != Opcode.LOGIN_OPCODE &&
                opcode != Opcode.REGISTER_OPCODE &&
                opcode != Opcode.TOKEN_REFRESH_REQUEST;
    }

}
