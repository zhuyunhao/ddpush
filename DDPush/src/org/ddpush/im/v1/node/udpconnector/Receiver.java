package org.ddpush.im.v1.node.udpconnector;

import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

import org.ddpush.im.util.DateTimeUtil;
import org.ddpush.im.util.StringUtil;
import org.ddpush.im.v1.node.ClientMessage;

/**
 * 客户端UDP协议接收者
 */
public class Receiver implements Runnable {

	/** UDP天线 */
	protected DatagramChannel channel;
	/** 缓存分组大小 */
	protected int bufferSize = 1024;
	/** 停止状态位 */
	protected boolean stoped = false;
	/** 缓存buffer */
	protected ByteBuffer buffer;
	/** 套接字地址 */
	private SocketAddress address;
	/** 接收事务计数器 */
	protected AtomicLong queueIn = new AtomicLong(0);
	/** 处理事务计数器 */
	protected AtomicLong queueOut = new AtomicLong(0);
	/** 客户端消息队列 */
	protected ConcurrentLinkedQueue<ClientMessage> mq = new ConcurrentLinkedQueue<ClientMessage>();

	/**
	 * 初始化
	 * 
	 * @param channel
	 */
	public Receiver(DatagramChannel channel) {
		this.channel = channel;
	}

	/**
	 * 初始化频道缓存
	 */
	public void init() {
		buffer = ByteBuffer.allocate(this.bufferSize);
	}

	/**
	 * 变更停止位
	 */
	public void stop() {
		this.stoped = true;
	}

	/**
	 * 启动处理消息
	 */
	public void run() {
		while (!this.stoped) {
			try {
				// synchronized(enQueSignal){
				processMessage();
				// if(mq.isEmpty() == true){
				// enQueSignal.wait();
				// }
				// }
			} catch (Exception e) {
				e.printStackTrace();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}

	/**
	 * 处理接收消息
	 * 
	 * @throws Exception
	 */
	protected void processMessage() throws Exception {
		address = null;
		buffer.clear();
		try {
			address = this.channel.receive(buffer);
		} catch (SocketTimeoutException timeout) {

		}
		if (address == null) {
			try {
				Thread.sleep(1);
			} catch (Exception e) {

			}
			return;
		}

		buffer.flip();
		byte[] swap = new byte[buffer.limit() - buffer.position()];
		System.arraycopy(buffer.array(), buffer.position(), swap, 0, swap.length);

		ClientMessage m = new ClientMessage(address, swap);

		enqueue(m);
		System.out.println(DateTimeUtil.getCurDateTime()+" r:"+StringUtil.convert(m.getData())+" from:"+m.getSocketAddress().toString());

	}

	/**
	 * 接收消息事务入队
	 * 
	 * @param message
	 * @return
	 */
	protected boolean enqueue(ClientMessage message) {
		boolean result = mq.add(message);
		if (result == true) {
			queueIn.addAndGet(1);
		}
		return result;
	}

	/**
	 * 分发消息出队
	 * 
	 * @return
	 */
	protected ClientMessage dequeue() {
		ClientMessage m = mq.poll();
		if (m != null) {
			queueOut.addAndGet(1);
		}
		return m;
	}

	/**
	 * 取出消息处理
	 * 
	 * @return
	 */
	public ClientMessage receive() {

		ClientMessage m = null;
		while (true) {
			m = dequeue();
			if (m == null) {
				return null;
			}
			if (m.checkFormat() == true) {// 检查包格式是否合法，为了网络快速响应，在这里检查，不在接收线程检查
				return m;
			}
		}
	}
}
