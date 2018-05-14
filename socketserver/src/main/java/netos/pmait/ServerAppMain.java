package netos.pmait;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import netos.pmait.utils.WebsocketChatServerInitializer;

import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

@SuppressWarnings("all")
public class ServerAppMain {

	public static int port = 6666;

	public static void main(String[] args) {

		ApplicationContext beanFactory = new ClassPathXmlApplicationContext("applicationContext.xml");

		WebsocketChatServerInitializer childHandlers = (WebsocketChatServerInitializer) beanFactory.getBean("serverPipelineFactory");

		java.util.Properties pebean = (java.util.Properties) beanFactory.getBean("propertiesReader");

		try {
			String start_port = (String) pebean.get("start_port");
			port = Integer.parseInt(start_port);
		} catch (Exception e1) {

			e1.printStackTrace();
		}

		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup)
			.channel(NioServerSocketChannel.class)
			.childHandler(childHandlers)
			// .childHandler(new ChannelInitializerImpl())
			// .option(ChannelOption.SO_BACKLOG, 128)
			.childOption(ChannelOption.SO_KEEPALIVE, true);

			System.out
			.println("=============websocket服务器启动成功======================"
					+ port);

			ChannelFuture f = b.bind(port).sync();
			f.channel().closeFuture().sync();

		} catch (Exception e) {

			e.printStackTrace();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();

			System.out.println("=============服务端 关闭了");
		}

	}


	public void printDateTime(){
		LocalDateTime ls = LocalDateTime.now();
		Instant  timestamp = Instant.now();
		DateTimeFormatter dft =DateTimeFormatter.ofPattern("yyyy-MM-dd");
		System.out.println(ls.format(dft));
	}
}
