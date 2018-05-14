package netos.pmait.handle;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.DefaultHttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpDataFactory;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentHashMap.KeySetView;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerFactory;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import netos.pmait.service.MyServiceI;

/**
 * 
 * 
 * 
 *  /sendMsgToApp 所有的这些业务逻辑处理 做成命令 对应插件类的方式 ,
 *  用spring 配置类插入进来做 handle处理类处理
 *  可以考虑用设计模式----命令模式（Command）
 *   本websocket支持:
 *       1.点对点发送功能,即客户端需要给某一台终端发送消息
 *       2.群发消息功能
 *       3.针对某一类型客户端发送消息
 *       4.延时发送消息功能
 * 
 * @author cctv
 *
 */
@SuppressWarnings("all")
public class MyRequestHandler extends SimpleChannelInboundHandler<Object> {


	protected Logger logger = Logger.getLogger(this.getClass());

	/**
	 * ws://localhost:6879/yywebsocket/1234
	 * {"mac":"123*1234","msg":"萨顶顶撒"}
	 */
	private static final String HOST = "ws://120.78.163.136:6879";
	//app客户端连接服务器的websocket地址
	private static final String WEBSOCKET_PATH = "/yywebsocket";
	//发送消息到app的请求地址
	//private static final String SEND_MSG_TOAPP="/sendMsgToApp";

	private WebSocketServerHandshaker handshaker;
	private ObjectMapper jsonMapper = new ObjectMapper();

	private FullHttpResponse response;

	public static final HttpDataFactory factory = new DefaultHttpDataFactory(DefaultHttpDataFactory.MINSIZE);

	public static ConcurrentHashMap<String, Channel> clientChannel = new ConcurrentHashMap(5);

	public boolean isWebSocke = false;
	public String  clientMac = "";
	public MyServiceI myServiceI;

	public DateTimeFormatter dft =DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS");


	public MyRequestHandler(MyServiceI serviceI) {

		myServiceI = serviceI;
	}

	private static String getWebSocketLocation(HttpRequest req) {

		return "";
	}

	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		String requestUrl = "";
		if (msg instanceof HttpRequest) {
			HttpRequest mRequest = ((HttpRequest) msg);
			requestUrl = mRequest.getUri();
			logger.debug( LocalDateTime.now().format(dft) + "来自的请求-----"+ requestUrl);
			String socketUrl = null;
			try {
				socketUrl = requestUrl.substring(0, requestUrl.lastIndexOf("/"));
				if (WEBSOCKET_PATH.equals(socketUrl)) {
					isWebSocke = true;
					clientMac = requestUrl.substring(
							requestUrl.lastIndexOf("/") + 1,
							requestUrl.length());
					if (!StringUtils.isEmpty(clientMac)) {
						Object newChannel = clientChannel.get(clientMac);
						if (newChannel == null) {
							Channel incoming = ctx.channel();
							clientChannel.put(clientMac, incoming);
						} else {
							Channel incoming = ctx.channel();
							if(!(newChannel.equals(incoming))){
								clientChannel.put(clientMac, incoming);
							}
						}
					}
				}
				handleHttpRequest(ctx, mRequest);
				if (!StringUtils.isEmpty(clientMac)&& WEBSOCKET_PATH.equals(socketUrl)) {
					/* HisDevice device = new HisDevice();
					 device.setMac(clientMac);
					 device.setState(1);
					 device.setOnlineTime(new Timestamp(System.currentTimeMillis()));
					 myServiceI.updateDeviceInfo(device);*/
					logger.debug("更新数据库设备状态--------"+clientMac);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if (msg instanceof WebSocketFrame) {
			handlerWebSocketFrame(ctx, (WebSocketFrame) msg);
		}
		if (!(isWebSocke) && (msg instanceof HttpContent)) {

			HttpContent content = (HttpContent) msg;
			ByteBuf buf = content.content();
			String result = buf.toString(CharsetUtil.UTF_8);
			logger.debug("接收到的数据------->>"+result);
			try {
				/*if (SEND_MSG_TOAPP.equals(requestUrl)) {
					Map packet = jsonMapper.readValue(result,Map.class);
					logger.debug("---------------"+packet.get("mac"));
					//客户端发送多个对象时,mac地址中间有*号如:00:e0:4c:87:00:00*84:26:bd:20:1b:90
					String allmac = String.valueOf(packet.get("mac"));
					if(allmac!=null){
						String clients[] = allmac.split("\\*");
						for(int j=0;clients!=null&&j<clients.length;j++){
							Channel mChannel = clientChannel.get(clients[j]);
							if(mChannel!=null&&mChannel.isActive()){
								TextWebSocketFrame tws = new TextWebSocketFrame(result);
								mChannel.writeAndFlush(tws);
							}
						}
					}else {
						Channel mChannel = clientChannel.get(allmac);
						if(mChannel!=null&&mChannel.isActive()){
							TextWebSocketFrame tws = new TextWebSocketFrame(result);
							mChannel.writeAndFlush(tws);
						}
					} 
				}*/
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
		ctx.flush();
	}

	private void handlerWebSocketFrame(ChannelHandlerContext ctx,
			WebSocketFrame frame) {
		// 判断是否关闭链路的指令
		if (frame instanceof CloseWebSocketFrame) {
			handshaker.close(ctx.channel(),
					(CloseWebSocketFrame) frame.retain());
		}

		// 判断是否ping消息
		if (frame instanceof PingWebSocketFrame) {
			ctx.channel().write(
					new PongWebSocketFrame(frame.content().retain()));
			return;
		}

		// 支持文本消息，不支持二进制消息
		if (!(frame instanceof TextWebSocketFrame)) {
			throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass().getName()));
		}

		// 返回应答消息
		String request = ((TextWebSocketFrame) frame).text();
		logger.debug( LocalDateTime.now().format(dft) + " 服务端收到------：" + request);
		Map<String, Object> param = new HashMap<String, Object>();
		try {
			param = jsonMapper.readValue(request,Map.class);
			String json = request;
			String []macStr = null;
			Channel incoming = ctx.channel();
			for (String ksv : param.keySet()) {
				macStr = String.valueOf(param.get("mac")).split("\\*");
			}
			for (int i = 0; i < macStr.length; i++) {
				Channel mChannel = clientChannel.get(macStr[i]);
				if(mChannel!=null&&mChannel.isActive()){
					System.out.println(String.valueOf(param.get("msg")));
					TextWebSocketFrame tws = new TextWebSocketFrame(String.valueOf(param.get("msg")));
					mChannel.writeAndFlush(tws);
				}
			}
			/*TextWebSocketFrame tws = new TextWebSocketFrame(String.valueOf(param.get("msg")));
			ctx.channel().writeAndFlush(tws);*/
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		

		// TextWebSocketFrame tws = new TextWebSocketFrame(new
		// Date().toString()+ ctx.channel().id() + "：" + request);

		// group.writeAndFlush(tws);

		// ctx.channel().writeAndFlush(tws);
	}



	/**
	 * /////////////////////////////////////////////
	 * 
	 *  @param ctx
	 *  @param req
	 * 
	 * /////////////////////////////////////////////
	 */
	private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest req) {

		if (!req.getDecoderResult().isSuccess()
				|| (!"websocket".equals(req.headers().get("Upgrade")))) {

			if (req.getMethod().equals(HttpMethod.POST)) {
				logger.debug("这是post请求");
			} else if (req.getMethod().equals(HttpMethod.GET)) {

				logger.debug("这是get请求");
			}

			// 处理管理平台的请求 favicon.ico /sendMsgToApp
			/*if (SEND_MSG_TOAPP.equals(req.getUri())) {

				try { 

					if (req.getMethod().equals(HttpMethod.POST)) {


					}


				} catch (Exception e1) {

					e1.printStackTrace();
				}

				String res = "{code:1,message:null,data:null}";
				try {
					DefaultFullHttpResponse response = new DefaultFullHttpResponse(
							HTTP_1_1, OK, Unpooled.wrappedBuffer(res
									.getBytes("UTF-8")));
					sendHttpResponse(ctx, req, response);

				} catch (Exception e) {
					e.printStackTrace();
				}

				return;
			}*/
			try {

				DefaultFullHttpResponse response = new DefaultFullHttpResponse(
						HTTP_1_1, OK, Unpooled.wrappedBuffer("success"
								.getBytes("UTF-8")));

				sendHttpResponse(ctx, req, response);
			} catch (Exception e) {

				e.printStackTrace();
			}

			return;

		}

		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
				getWebSocketLocation(null), null, false);
		handshaker = wsFactory.newHandshaker(req);

		if (handshaker == null) {
			WebSocketServerHandshakerFactory
			.sendUnsupportedWebSocketVersionResponse(ctx.channel());
		} else {
			handshaker.handshake(ctx.channel(), req);
		}
	}

	private static void sendHttpResponse(ChannelHandlerContext ctx,
			HttpRequest req, DefaultFullHttpResponse res) {
		// 返回应答给客户端
		if (res.getStatus().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(),
					CharsetUtil.UTF_8);
			res.content().writeBytes(buf);
			buf.release();
		}
		// 如果是非Keep-Alive，关闭连接
		ChannelFuture f = ctx.channel().writeAndFlush(res);
		if (!isKeepAlive(req) || res.getStatus().code() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	private static boolean isKeepAlive(HttpRequest req) {
		return false;
	}

	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		Channel incoming = ctx.channel();
		logger.debug("______________:" + incoming.remoteAddress() + "掉线");
		for(String key : clientChannel.keySet()) {  
			if(incoming.equals(clientChannel.get(key))){
				clientChannel.remove(key);
				logger.debug("mac:"+key+"移除成功----ip---->>"+incoming.remoteAddress());
				/* HisDevice device = new HisDevice();
				 device.setMac(key);
				 device.setState(0);
				 device.setOnlineTime(new Timestamp(System.currentTimeMillis()));
				 myServiceI.updateDeviceInfo(device);*/
				break;
			}

		}



	}

	public void channelActive(ChannelHandlerContext ctx) throws Exception {

		Channel incoming = ctx.channel();

		// channels.add(incoming);

		logger.debug("------------客户端在线状态--------------:"+ incoming.remoteAddress() + "在线");
	}

	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
			throws Exception {

		// super.exceptionCaught(ctx, cause);

		logger.debug("客户端异常");
	}



}

// public static ChannelGroup channels = new
// DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
// try {
// decoder.offer(contentData);
// while (decoder.hasNext()) {
// InterfaceHttpData data = decoder.next();
// if (data != null) {
//
// if (data.getHttpDataType() == HttpDataType.Attribute) {
// Attribute attribute = (Attribute) data;
// String value = attribute.getValue();
// System.out.println("得到sendMsgToApp参数---------"+data.getName()+" =="+attribute.getValue());
//
// }
//
//
// }
// }
// } catch (Exception e1) {
//
// }
