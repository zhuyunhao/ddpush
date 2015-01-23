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
import java.nio.ByteBuffer;

import org.ddpush.im.util.StringUtil;

/**
 * 客户端消息体定义
 */
public final class ClientMessage{
	
	/** 套接字地址 */
	protected SocketAddress address;
	/** 消息体 */
	protected byte[] data;
	
	/**
	 * 初始化
	 */
	public ClientMessage(SocketAddress address, byte[] data) throws Exception{
		this.address = address;
		this.data = data;
	}
	
//	public static org.ddpush.im.node.Message getNewInstance(){
//		return null;
//	}
	
	/**
	 * 设定消息体
	 */
	public void setData(byte[] data){
		this.data = data;
	}
	
	/**
	 * 取得消息体
	 * @return
	 */
	public byte[] getData(){
		return this.data;
	}
	
	/**
	 * 取得套接字地址
	 * @return
	 */
	public SocketAddress getSocketAddress(){
		return this.address;
	}
	
	/**
	 * 设定消息体地址
	 * @param addr SocketAddress
	 */
	public void setSocketAddress(SocketAddress addr){
		this.address = addr;
	}
	
	/**
	 * 取得消息体内的version内容
	 * @return
	 */
	public int getVersionNum(){
		byte b = data[0];
		return b & 0xff;
	}
	
	/**
	 * 取得消息体内命令内容 0:心跳包 16:通用消息 17:分类信息 32:自定义消息
	 * @return
	 */
	public int getCmd(){
		byte b = data[2];
		return b & 0xff;
	}
	
	/**
	 * 取最后实际消息内容长度
	 * @return
	 */
	public int getDataLength(){
		return (int)ByteBuffer.wrap(data, 19, 2).getChar();
	}
	
	/**
	 * 取得uuid
	 * @return
	 */
	public String getUuidHexString(){
		return StringUtil.convert(data, 3, 16);
	}
	
	/**
	 * 验证消息体格式
	 * @return
	 */
	public boolean checkFormat(){
		if(this.data == null){
			return false;
		}
		if(data.length < Constant.CLIENT_MESSAGE_MIN_LENGTH){
			return false;
		}
		if(getVersionNum() != Constant.VERSION_NUM){
			return false;
		}

		int cmd = getCmd();
		if(cmd != ClientStatMachine.CMD_0x00
				//&& cmd != ClientStatMachine.CMD_0x01
				&& cmd != ClientStatMachine.CMD_0x10
				&& cmd != ClientStatMachine.CMD_0x11
				&& cmd != ClientStatMachine.CMD_0x20
				&& cmd != ClientStatMachine.CMD_0xff){
			return false;
		}
		int dataLen = getDataLength();
		if(data.length != dataLen + Constant.CLIENT_MESSAGE_MIN_LENGTH){
			return false;
		}
		
		if(cmd ==  ClientStatMachine.CMD_0x00 && dataLen != 0){
			return false;
		}
		
		if(cmd ==  ClientStatMachine.CMD_0x10 && dataLen != 0){
			return false;
		}
		
		if(cmd ==  ClientStatMachine.CMD_0x11 && dataLen != 8){
			return false;
		}
		
		if(cmd ==  ClientStatMachine.CMD_0x20 && dataLen != 0){
			return false;
		}
		
		return true;
	}
	
//	public byte[] getUUID(){
//		return 
//	}

}
