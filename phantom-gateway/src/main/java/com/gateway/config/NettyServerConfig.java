package com.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import com.gateway.network.handler.GameProtocolDecoder;
import com.gateway.network.handler.GameProtocolEncoder;
import com.gateway.network.handler.LoginHandler;
import com.gateway.network.handler.ProtocolRouterHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
@Slf4j
@Component
@RequiredArgsConstructor
public class NettyServerConfig {
    @Value("${netty.port}")
    private int port;

    private final LoginHandler loginHandler;
    private final ProtocolRouterHandler protocolRouterHandler;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @PostConstruct
    private void start() throws Exception{
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // 配置处理器链
                            ch.pipeline()
                                    .addLast(new GameProtocolDecoder())
                                    .addLast(new GameProtocolEncoder())
                                    .addLast(loginHandler)           // 先处理登录相关协议
                                    .addLast(protocolRouterHandler); // 再处理路由转发
                        }
                    });
            bootstrap.bind(port).sync();
            log.info("Netty服务器启动成功，端口: {}", port);
        } catch (Exception e) {
            log.error("Netty服务器启动失败", e);
            throw e;
        }
    }
    @PreDestroy
    public void stop() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        log.info("Netty服务器已关闭");
    }
}
