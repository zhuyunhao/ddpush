/*
 *Copyright 2014 DDPush
 *Author: AndyKwok(in English) GuoZhengzhu(in Chinese)
 *Email: ddpush@126.com
 *

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

 */
package org.ddpush.im.v1.client.appuser;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * UDP客户端接口
 */
public abstract class UDPClientBase implements Runnable {

	/** UDP套接字 */
	protected DatagramSocket ds;
	/** 最后发送时间戳 */
	protected long lastSent = 0;
	/** 最后接收时间戳 */
	protected long lastReceived = 0;
	/** socket端口 */
	protected int remotePort = 9966;
	/** 应用id */
	protected int appid = 1;
	/** 应用识别码 */
	protected byte[] uuid;
	/** 服务器地址 */
	protected String remoteAddress = null;
	/** 用并发队列维护服务消息体 */
	protected ConcurrentLinkedQueue<Message> mq = new ConcurrentLinkedQueue<Message>();
	/** 消息生成计数器 */
	protected AtomicLong queueIn = new AtomicLong(0);
	/** 消息接收计数器 */
	protected AtomicLong queueOut = new AtomicLong(0);
	/** 数组长度 */
	protected int bufferSize = 1024;
	/** 心跳间隔默认50秒 */
	protected int heartbeatInterval = 50;
	/** 消息体数组 */
	protected byte[] bufferArray;
	/** 缓冲buffer */
	protected ByteBuffer buffer;
	/** 重置标志位 */
	protected boolean needReset = true;
	/** 服务开始标志位 */
	protected boolean started = false;
	/** 服务停止标志位 */
	protected boolean stoped = false;
	/** UDPClientBase线程 */
	protected Thread receiverT;
	/** 工作接口 */
	protected Worker worker;
	/** 工作线程 */
	protected Thread workerT;
	/** 发出计数器 */
	private long sentPackets;
	/** 接收计数器 */
	private long receivedPackets;

	/**
	 * UDP客户端初始化
	 * 
	 * @param uuid
	 * @param appid
	 * @param serverAddr
	 * @param serverPort
	 * @throws Exception
	 */
	public UDPClientBase(byte[] uuid, int appid, String serverAddr, int serverPort) throws Exception {
		if (uuid == null || uuid.length != 16) {
			throw new java.lang.IllegalArgumentException("uuid byte array must be not null and length of 16 bytes");
		}
		if (appid < 1 || appid > 255) {
			throw new java.lang.IllegalArgumentException("appid must be from 1 to 255");
		}
		if (serverAddr == null || serverAddr.trim().length() == 0) {
			throw new java.lang.IllegalArgumentException("server address illegal: " + serverAddr);
		}

		this.uuid = uuid;
		this.appid = appid;
		this.remoteAddress = serverAddr;
		this.remotePort = serverPort;
	}

	/**
	 * 消息体入队
	 * 
	 * @param message
	 * @return
	 */
	protected boolean enqueue(Message message) {
		boolean result = mq.add(message);
		if (result == true) {
			queueIn.addAndGet(1);
		}
		return result;
	}

	/**
	 * 消息出队
	 * 
	 * @return
	 */
	protected Message dequeue() {
		Message m = mq.poll();
		if (m != null) {
			queueOut.addAndGet(1);
		}
		return m;
	}

	/**
	 * 消息数组和缓冲初始化
	 */
	private synchronized void init() {
		bufferArray = new byte[bufferSize];
		buffer = ByteBuffer.wrap(bufferArray);
	}

	/**
	 * 客户端服务重置
	 * 
	 * @throws Exception
	 */
	protected synchronized void reset() throws Exception {
		if (needReset == false) {
			return;
		}

		if (ds != null) {
			try {
				ds.close();
			} catch (Exception e) {
			}
		}
		if (hasNetworkConnection() == true) {
			ds = new DatagramSocket();
			ds.connect(new InetSocketAddress(remoteAddress, remotePort));
			needReset = false;
		} else {
			try {
				Thread.sleep(1000);
			} catch (Exception e) {
			}
		}
	}

	/**
	 * 客户端服务启动
	 * 
	 * @throws Exception
	 */
	public synchronized void start() throws Exception {
		if (this.started == true) {
			return;
		}
		this.init();

		receiverT = new Thread(this, "udp-client-receiver");
		receiverT.setDaemon(true);
		synchronized (receiverT) {
			receiverT.start();
			receiverT.wait();
		}

		worker = new Worker();
		workerT = new Thread(worker, "udp-client-worker");
		workerT.setDaemon(true);
		synchronized (workerT) {
			workerT.start();
			workerT.wait();
		}

		this.started = true;
	}

	/**
	 * 客户端服务停止
	 */
	public void stop() throws Exception {
		stoped = true;
		if (ds != null) {
			try {
				ds.close();
			} catch (Exception e) {
			}
			ds = null;
		}
		if (receiverT != null) {
			try {
				receiverT.interrupt();
			} catch (Exception e) {
			}
		}

		if (workerT != null) {
			try {
				workerT.interrupt();
			} catch (Exception e) {
			}
		}
	}

	public void run() {

		synchronized (receiverT) {
			receiverT.notifyAll();
		}

		while (stoped == false) {
			try {
				if (hasNetworkConnection() == false) {
					try {
						trySystemSleep();
						Thread.sleep(1000);
					} catch (Exception e) {
					}
					continue;
				}
				reset();
				heartbeat();
				receiveData();
			} catch (java.net.SocketTimeoutException e) {

			} catch (Exception e) {
				e.printStackTrace();
				this.needReset = true;
			} catch (Throwable t) {
				t.printStackTrace();
				this.needReset = true;
			} finally {
				if (needReset == true) {
					try {
						trySystemSleep();
						Thread.sleep(1000);
					} catch (Exception e) {
					}
				}
				if (mq.isEmpty() == true || hasNetworkConnection() == false) {
					try {
						trySystemSleep();
						Thread.sleep(1000);
					} catch (Exception e) {
					}
				}
			}
		}
		if (ds != null) {
			try {
				ds.close();
			} catch (Exception e) {
			}
			ds = null;
		}
	}

	/**
	 * 向服务器发送心跳包
	 * 
	 * @throws Exception
	 */
	private void heartbeat() throws Exception {
		if (System.currentTimeMillis() - lastSent < heartbeatInterval * 1000) {
			return;
		}
		byte[] buffer = new byte[Message.CLIENT_MESSAGE_MIN_LENGTH];
		ByteBuffer.wrap(buffer).put((byte) Message.version).put((byte) appid).put((byte) Message.CMD_0x00).put(uuid).putChar((char) 0);
		send(buffer);
	}

	/**
	 * 接收服务器消息
	 * 
	 * @throws Exception
	 */
	private void receiveData() throws Exception {
		DatagramPacket dp = new DatagramPacket(bufferArray, bufferArray.length);
		ds.setSoTimeout(5 * 1000);
		ds.receive(dp);
		if (dp.getLength() <= 0 || dp.getData() == null || dp.getData().length == 0) {
			return;
		}
		byte[] data = new byte[dp.getLength()];

		System.arraycopy(dp.getData(), 0, data, 0, dp.getLength());
		Message m = new Message(dp.getSocketAddress(), data);
		if (m.checkFormat() == false) {
			return;
		}
		this.receivedPackets++;
		this.lastReceived = System.currentTimeMillis();
		this.ackServer(m);
		if (m.getCmd() == Message.CMD_0x00) {
			return;
		}
		this.enqueue(m);
		worker.wakeup();
	}

	/**
	 * 向服务器发送收到消息的应答
	 * 
	 * @param m
	 * @throws Exception
	 */
	private void ackServer(Message m) throws Exception {
		if (m.getCmd() == Message.CMD_0x10) {
			byte[] buffer = new byte[Message.CLIENT_MESSAGE_MIN_LENGTH];
			ByteBuffer.wrap(buffer).put((byte) Message.version).put((byte) appid).put((byte) Message.CMD_0x10).put(uuid).putChar((char) 0);
			send(buffer);
		}
		if (m.getCmd() == Message.CMD_0x11) {
			byte[] buffer = new byte[Message.CLIENT_MESSAGE_MIN_LENGTH + 8];
			byte[] data = m.getData();
			ByteBuffer.wrap(buffer).put((byte) Message.version).put((byte) appid).put((byte) Message.CMD_0x11).put(uuid).putChar((char) 8)
					.put(data, Message.SERVER_MESSAGE_MIN_LENGTH, 8);
			send(buffer);
		}
		if (m.getCmd() == Message.CMD_0x20) {
			byte[] buffer = new byte[Message.CLIENT_MESSAGE_MIN_LENGTH];
			ByteBuffer.wrap(buffer).put((byte) Message.version).put((byte) appid).put((byte) Message.CMD_0x20).put(uuid).putChar((char) 0);
			send(buffer);
		}
	}

	/**
	 * 向服务器发送消息
	 * 
	 * @param data
	 * @throws Exception
	 */
	private void send(byte[] data) throws Exception {
		if (data == null) {
			return;
		}
		if (ds == null) {
			return;
		}
		DatagramPacket dp = new DatagramPacket(data, data.length);
		dp.setSocketAddress(ds.getRemoteSocketAddress());
		ds.send(dp);
		lastSent = System.currentTimeMillis();
		this.sentPackets++;
	}

	/**
	 * 取得发送消息次数
	 * 
	 * @return
	 */
	public long getSentPackets() {
		return this.sentPackets;
	}

	/**
	 * 取得接收消息次数
	 * 
	 * @return
	 */
	public long getReceivedPackets() {
		return this.receivedPackets;
	}

	/**
	 * 设置端口号
	 * 
	 * @param port
	 */
	public void setServerPort(int port) {
		this.remotePort = port;
	}

	/**
	 * 取得端口号
	 * 
	 * @return
	 */
	public int getServerPort() {
		return this.remotePort;
	}

	/**
	 * 设置服务器地址
	 * 
	 * @param addr
	 */
	public void setServerAddress(String addr) {
		this.remoteAddress = addr;
	}

	/**
	 * 取得服务器地址
	 * 
	 * @return
	 */
	public String getServerAddress() {
		return this.remoteAddress;
	}

	/**
	 * 设置消息数组长度
	 * 
	 * @param bytes
	 */
	public void setBufferSize(int bytes) {
		this.bufferSize = bytes;
	}

	/**
	 * 设置消息数组长度
	 * 
	 * @return
	 */
	public int getBufferSize() {
		return this.bufferSize;
	}

	/**
	 * 取得最后心跳时间戳
	 * 
	 * @return
	 */
	public long getLastHeartbeatTime() {
		return lastSent;
	}

	/**
	 * 取得最后收到消息时间
	 * 
	 * @return
	 */
	public long getLastReceivedTime() {
		return lastReceived;
	}

	/**
	 * 设置心跳时间间隔
	 * 
	 * @param second
	 */
	public void setHeartbeatInterval(int second) {
		if (second <= 0) {
			return;
		}
		this.heartbeatInterval = second;
	}

	/**
	 * 取得心跳时间间隔
	 * 
	 * @return
	 */
	public int getHeartbeatInterval() {
		return this.heartbeatInterval;
	}

	/**
	 * 验证网络状态，PC直接返回true
	 * 
	 * @return
	 */
	public abstract boolean hasNetworkConnection();

	/**
	 * 休眠，PC不需要休眠
	 */
	public abstract void trySystemSleep();

	/**
	 * 接收消息回调
	 * 
	 * @param message
	 */
	public abstract void onPushMessage(Message message);

	/**
	 * 客户端工作线程
	 */
	class Worker implements Runnable {
		public void run() {
			synchronized (workerT) {
				workerT.notifyAll();
			}
			while (stoped == false) {
				try {
					handleEvent();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					waitMsg();
				}
			}
		}

		private void waitMsg() {
			synchronized (this) {
				try {
					this.wait(1000);
				} catch (java.lang.InterruptedException e) {

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		private void wakeup() {
			synchronized (this) {
				this.notifyAll();
			}
		}

		private void handleEvent() throws Exception {
			Message m = null;
			while (true) {
				m = dequeue();
				if (m == null) {
					return;
				}
				if (m.checkFormat() == false) {
					continue;
				}

				// real work here
				onPushMessage(m);
			}
			// finish work here, such as release wake lock
		}

	}
}
