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
package org.ddpush.im.v1.client.appserver;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

import org.ddpush.im.util.StringUtil;

/**
 * 协议包定义
 */
public class Pusher {

	/** 版本号 */
	private int version = 1;
	/** 可支持255个应用同时使用一个DDPush服务器，也可以一个应用有255个分类或渠道。注：0保留，用于实时推送 */
	private int appId = 1;
	/** socket超时时间 */
	private int timeout;
	/** 服务器host */
	private String host;
	/** 服务器port */
	private int port;
	/** socket 套接 */
	private Socket socket;
	/** 输入流 */
	private InputStream in;
	/** 输出流 */
	private OutputStream out;

	/**
	 * 初始化
	 * 
	 * @param host
	 * @param port
	 * @param timeoutMillis
	 * @param version
	 * @param appId
	 * @throws Exception
	 */
	public Pusher(String host, int port, int timeoutMillis, int version, int appId) throws Exception {
		this.setVersion(version);
		this.setAppId(appId);
		this.host = host;
		this.port = port;
		this.timeout = timeoutMillis;
		initSocket();
	}

	/**
	 * 初始化 version和appid为1
	 * 
	 * @param host
	 * @param port
	 * @param timeoutMillis
	 * @throws Exception
	 */
	public Pusher(String host, int port, int timeoutMillis) throws Exception {
		this(host, port, timeoutMillis, 1, 1);
	}

	/**
	 * 初始化
	 * 
	 * @param socket
	 * @throws Exception
	 */
	public Pusher(Socket socket) throws Exception {
		this.socket = socket;
		in = socket.getInputStream();
		out = socket.getOutputStream();
	}

	/**
	 * 初始化socket套接
	 * 
	 * @throws Exception
	 */
	private void initSocket() throws Exception {
		socket = new Socket(this.host, this.port);
		socket.setSoTimeout(timeout);
		in = socket.getInputStream();
		out = socket.getOutputStream();
	}

	/**
	 * 关闭socket套接
	 * 
	 * @throws Exception
	 */
	public void close() throws Exception {
		if (socket == null) {
			return;
		}
		socket.close();
	}

	/**
	 * 设置版本号，必须在1到255之间
	 * 
	 * @param version
	 * @throws java.lang.IllegalArgumentException
	 */
	public void setVersion(int version) throws java.lang.IllegalArgumentException {
		if (version < 1 || version > 255) {
			throw new java.lang.IllegalArgumentException("version must be 1 to 255");
		}
		this.version = version;
	}

	/**
	 * 取得设定的版本号
	 * 
	 * @return
	 */
	public int getVersion() {
		return this.version;
	}

	/**
	 * 设置appid必须在1到255之间
	 * 
	 * @param appId
	 * @throws IllegalArgumentException
	 */
	public void setAppId(int appId) throws IllegalArgumentException {
		if (appId < 1 || appId > 255) {
			throw new java.lang.IllegalArgumentException("appId must be 1 to 255");
		}
		this.appId = appId;
	}

	/**
	 * 取得应用id
	 * 
	 * @return
	 */
	public int getAppId() {
		return this.appId;
	}

	/**
	 * 验证uuid，长度必须为16
	 * 
	 * @param uuid
	 * @return
	 * @throws IllegalArgumentException
	 */
	private boolean checkUuidArray(byte[] uuid) throws IllegalArgumentException {
		if (uuid == null || uuid.length != 16) {
			throw new IllegalArgumentException("uuid byte array must be not null and length of 16");
		}
		return true;
	}

	/**
	 * 验证long，长度必须为8
	 * 
	 * @param longArray
	 * @return
	 * @throws IllegalArgumentException
	 */
	private boolean checkLongArray(byte[] longArray) throws IllegalArgumentException {
		if (longArray == null || longArray.length != 8) {
			throw new IllegalArgumentException("array must be not null and length of 8");
		}
		return true;
	}

	/**
	 * 发送通用推送命令(命令码：16（0x10）。格式：[1][1][0x10][0x0000]
	 * 客户端响应：16（0x10）。格式：[1][1][0x10][uuid][0x0000])
	 * 
	 * @param uuid
	 * @return
	 * @throws Exception
	 */
	public boolean push0x10Message(byte[] uuid) throws Exception {
		checkUuidArray(uuid);
		out.write(version);
		out.write(appId);
		out.write(16);
		out.write(uuid);
		out.write(0);
		out.write(0);
		out.flush();

		byte[] b = new byte[1];
		in.read(b);
		if ((int) b[0] == 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 发送分类推送命令，命令码为17
	 * [version][appid][0x11][uuid][0x0008][8字节无符号整数]注意：分类信息至多64种类型
	 * ，且按位叠加操作（|和&）进行确认
	 * 
	 * @param uuid
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public boolean push0x11Message(byte[] uuid, long data) throws Exception {
		byte[] tmp = new byte[8];
		ByteBuffer.wrap(tmp).putLong(data);
		return this.push0x11Message(uuid, tmp);
	}

	/**
	 * 发送分类推送命令(命令码：17（0x11）。格式：[1][1][0x11][0x0008][8字节无符号整数]
	 * 客户端响应：17（0x11）。格式：[1][1][0x11][uuid][0x0008][8字节无符号整数]
	 * 注意：分类信息至多64种类型，且按位叠加操作（|和&）进行确认)
	 * 
	 * @param uuid
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public boolean push0x11Message(byte[] uuid, byte[] data) throws Exception {
		this.checkLongArray(data);
		out.write(version);
		out.write(appId);
		out.write(17);
		out.write(uuid);
		out.write(0);
		out.write(8);
		out.write(data);
		out.flush();

		byte[] b = new byte[1];
		in.read(b);
		if ((int) b[0] == 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 发送自定义信息推送命令(命令码：32（0x20）。格式：[1][1][0x20][0x内容长度][内容]
	 * 客户端响应：32（0x20）。格式：[1][1][0x20] [uuid] [0x0000]
	 * 注意：内容长度必须与指定的长度一致，并且至少为1，最多500字节，否则被认为是非法格式。
	 * DDPush强烈建议将自定义信息的长度限制为100个字节以内)
	 * 
	 * @param uuid
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public boolean push0x20Message(byte[] uuid, byte[] data) throws Exception {
		this.checkUuidArray(uuid);
		if (data == null) {
			throw new NullPointerException("data array is null");
		}
		if (data.length == 0 || data.length > 500) {
			throw new IllegalArgumentException("data array length illegal, min 1, max 500");
		}
		byte[] dataLen = new byte[2];
		ByteBuffer.wrap(dataLen).putChar((char) data.length);
		out.write(version);
		out.write(appId);
		out.write(32);
		out.write(uuid);
		out.write(dataLen);
		out.write(data);
		out.flush();

		byte[] b = new byte[1];
		in.read(b);
		if ((int) b[0] == 0) {
			return true;
		} else {
			return false;
		}

	}

	/**
	 * 测试
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Pusher pusher = null;
		try {
			boolean result;
			pusher = new Pusher("192.168.2.111", 9999, 5000);
			result = pusher.push0x20Message(StringUtil.hexStringToByteArray("2cb1abca847b4491bc2b206b592b64fd"),
					"cmd=ntfurl|title=通知标题|content=通知内容|tt=提示标题|url=/m/admin/eml/inbox/list".getBytes("UTF-8"));
			// result =
			// pusher.push0x10Message(StringUtil.hexStringToByteArray("2cb1abca847b4491bc2b206b592b64fd"));
			// result =
			// pusher.push0x11Message(StringUtil.hexStringToByteArray("2cb1abca847b4491bc2b206b592b64fd"),128);

			System.out.println(result);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (pusher != null) {
				try {
					pusher.close();
				} catch (Exception e) {
				}
			}
		}
	}

}
