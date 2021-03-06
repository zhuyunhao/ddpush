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
package org.ddpush.im.v1.node;

import java.net.SocketAddress;

/**
 * 服务端消息
 */
public final class ServerMessage {

	/** 套接字地址 */
	protected SocketAddress address;
	/** 消息体数组 */
	protected byte[] data;

	/**
	 * 初始化服务器消息体
	 * 
	 * @param address
	 * @param data
	 * @throws Exception
	 */
	public ServerMessage(SocketAddress address, byte[] data) throws Exception {
		this.address = address;
		this.data = data;
	}

	// public static org.ddpush.im.node.Message getNewInstance(){
	// return null;
	// }

	/**
	 * 设定服务消息内容
	 * 
	 * @param data
	 */
	public void setData(byte[] data) {
		this.data = data;
	}

	/**
	 * 取得服务消息内容
	 * 
	 * @return
	 */
	public byte[] getData() {
		return this.data;
	}

	/**
	 * 取得套接字地址
	 * 
	 * @return
	 */
	public SocketAddress getSocketAddress() {
		return this.address;
	}

	/**
	 * 设定套接字地址
	 * 
	 * @param addr
	 */
	public void setSocketAddress(SocketAddress addr) {
		this.address = addr;
	}

}
