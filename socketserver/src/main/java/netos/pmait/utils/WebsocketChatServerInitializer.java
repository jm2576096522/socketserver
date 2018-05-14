package netos.pmait.utils;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import netos.pmait.handle.MyRequestHandler;
import netos.pmait.service.MyServiceI;

/**
 * 
 * 
 * 
 * @author cctv 2017-12-20
 *
 */
public class WebsocketChatServerInitializer extends ChannelInitializer<SocketChannel> {

	public MyServiceI myServiceI;

	public MyServiceI getMyServiceI() {
		return myServiceI;
	}

	public void setMyServiceI(MyServiceI myServiceI) {
		this.myServiceI = myServiceI;
	}

	static final EventExecutorGroup executorGroup = new DefaultEventExecutorGroup(
			20);

	protected void initChannel(SocketChannel ch) throws Exception {

		ChannelPipeline pipeline = ch.pipeline();

		// pipeline.addLast(new HttpResponseEncoder());
		// // server端接收到的是httpRequest，所以要使用HttpRequestDecoder进行解码
		// pipeline.addLast(new HttpRequestDecoder());
		// pipeline.addLast(new HttpRequestHandler());
		// pipeline.addLast("decoder", new StringDecoder(CharsetUtil.UTF_8));
		// pipeline.addLast("encoder", new StringEncoder(CharsetUtil.UTF_8));
		// pipeline.addLast(new NewHttpRequestHandler());
		// pipeline.addLast("decoder", new StringDecoder(CharsetUtil.UTF_8));
		// pipeline.addLast("encoder", new StringEncoder(CharsetUtil.UTF_8));
		pipeline.addLast(new HttpServerCodec());
		pipeline.addLast(new StringDecoder());

		pipeline.addLast(new HttpObjectAggregator(64 * 1024));
		pipeline.addLast(new ChunkedWriteHandler());
		pipeline.addLast("deflater", new HttpContentCompressor());
	 

		pipeline.addLast(executorGroup,new ChannelHandler[] { new MyRequestHandler(myServiceI) });
		// 检测客户端是否在线

		//pipeline.addLast(new HeartbeatServerHandler());

	}

}
