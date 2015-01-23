package org.ddpush.im.v1.node.udpconnector;

import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;

import org.ddpush.im.util.PropertyUtil;
import org.ddpush.im.v1.node.ClientMessage;
import org.ddpush.im.v1.node.ServerMessage;

/**
 * UDP服务端
 */
public class UdpConnector {

	/** UDP通道 */
	protected DatagramChannel antenna;// 天线
	/** 接收器 */
	protected Receiver receiver;
	/** 发送器 */
	protected Sender sender;
	/** 接收线程 */
	protected Thread receiverThread;
	/** 发送线程 */
	protected Thread senderThread;

	// boolean started = false;
	// boolean stoped = false;

	/** 从配置文件读取端口号 */
	protected int port = PropertyUtil.getPropertyInt("CLIENT_UDP_PORT");

	/**
	 * 设定号
	 * 
	 * @param port
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * 取得端口号
	 * 
	 * @return
	 */
	public int getPort() {
		return this.port;
	}

	public void init() {

	}

	/**
	 * 服务开始，初始化
	 * 
	 * @throws Exception
	 */
	public void start() throws Exception {
		if (antenna != null) {
			throw new Exception("antenna is not null, may have run before");
		}
		antenna = DatagramChannel.open();
		antenna.socket().bind(new InetSocketAddress(port));
		System.out.println("udp connector port:" + port);
		// non-blocking
		antenna.configureBlocking(false);
		antenna.socket().setReceiveBufferSize(1024 * 1024 * PropertyUtil.getPropertyInt("CLIENT_UDP_BUFFER_RECEIVE"));
		antenna.socket().setSendBufferSize(1024 * 1024 * PropertyUtil.getPropertyInt("CLIENT_UDP_BUFFER_SEND"));
		System.out.println("udp connector recv buffer size:" + antenna.socket().getReceiveBufferSize());
		System.out.println("udp connector send buffer size:" + antenna.socket().getSendBufferSize());
		// 初始化接收和发送服务
		this.receiver = new Receiver(antenna);
		this.receiver.init();
		this.sender = new Sender(antenna);
		this.sender.init();
		// 启动接收和发送线程
		this.senderThread = new Thread(sender, "AsynUdpConnector-sender");
		this.receiverThread = new Thread(receiver, "AsynUdpConnector-receiver");
		this.receiverThread.start();
		this.senderThread.start();
	}

	/**
	 * 停止服务
	 * 
	 * @throws Exception
	 */
	public void stop() throws Exception {
		receiver.stop();
		sender.stop();
		try {
			receiverThread.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			senderThread.join();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			antenna.socket().close();
		} catch (Exception e) {
		}
		try {
			antenna.close();
		} catch (Exception e) {
		}
	}

	/**
	 * 取得接收者接待事件数
	 * 
	 * @return
	 */
	public long getInqueueIn() {
		return this.receiver.queueIn.longValue();
	}

	/**
	 * 取得接收者处理事件数
	 * 
	 * @return
	 */
	public long getInqueueOut() {
		return this.receiver.queueOut.longValue();
	}

	/**
	 * 取得发送者接待事件数
	 * 
	 * @return
	 */
	public long getOutqueueIn() {
		return this.sender.queueIn.longValue();
	}

	/**
	 * 取得发送者处理事件数
	 * 
	 * @return
	 */
	public long getOutqueueOut() {
		return this.sender.queueOut.longValue();
	}

	/**
	 * 取得未处理消息
	 * 
	 * @return
	 * @throws Exception
	 */
	public ClientMessage receive() throws Exception {
		return receiver.receive();
	}

	/**
	 * 发送服务端消息
	 * 
	 * @param message
	 * @return
	 * @throws Exception
	 */
	public boolean send(ServerMessage message) throws Exception {
		return sender.send(message);
	}

}
