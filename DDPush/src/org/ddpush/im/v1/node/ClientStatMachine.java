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

import java.lang.ref.WeakReference;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.ddpush.im.util.PropertyUtil;
import org.ddpush.im.v1.node.ClientMessage;
import org.ddpush.im.v1.node.Constant;
import org.ddpush.im.v1.node.ServerMessage;
import org.ddpush.im.v1.node.tcpconnector.MessengerTask;

/**
 * uuid终端状态机
 */
public class ClientStatMachine {

	/** 心跳包 */
	public static final int CMD_0x00 = 0;// 心跳包
	/** 登录包 */
	public static final int CMD_0x01 = 1;// 登录包
	/** 通用信息 */
	public static final int CMD_0x10 = 16;// 通用信息
	/** 分类信息 */
	public static final int CMD_0x11 = 17;// 分类信息
	/** 自定义信息 */
	public static final int CMD_0x20 = 32;// 自定义信息
	/** 重置命令 */
	public static final int CMD_0xff = 255;// 重置命令
	/** 客户端离线判定 */
	public static int PUSH_IM_AFTER_ACTIVE_SECOND = 60;
	/** 回应客户端心跳：从不 */
	private static final int POLICY_NEVER = 0;
	/** 回应客户端心跳：套接字变更时回应 */
	private static final int POLICY_SA_CHANGED = 1;
	/** 回应客户端心跳：总是回应 */
	private static final int POLICY_ALWAYS = 2;
	/** 回应客户端心跳协议 */
	private static int ackHeartbeatPolicy;
	/** 是否生产状态机 */
	private static boolean createByClient = "YES".equalsIgnoreCase(PropertyUtil.getProperty("CREATE_MACHINE_BY_CLIENT")) ? true : false;
	/** 最后心跳时间 */
	private long lastTick = -1;// 最后心跳时间
	/** 最后网络地址 */
	private SocketAddress lastAddr = null;// 最后心跳等的网络地址
	/** 是否有通用信息未接收 */
	private boolean hasMessage0x10 = false;// 是否有通用信息未接收
	/** 最后通用信息时间 */
	private long last0x10Time = -1;// 最后通用信息时间
	/** 最新分类信息通知 */
	private long message0x11 = 0;// 最新分类信息通知
	/** 最新分类信息通知时间 */
	private long last0x11Time = -1;// 最新分类信息通知时间
	/** 是否有自定义信息未接收 */
	private int message0x20Len = 0;// 是否有自定义信息未接收
	/** 最新自定义信息时间 */
	private long last0x20Time = -1;// 最新自定义信息时间
	/** 最新自定义信息内容数组 */
	private byte[] message0x20 = null;
	/** 若引用消息处理任务 */
	private WeakReference<MessengerTask> messengerTaskRef = null;
	/** 取得回应心跳协议 */
	static {
		String strPolicy = PropertyUtil.getProperty("ACK_HEARTBEAT_POLICY");
		if ("always".equalsIgnoreCase(strPolicy)) {
			ackHeartbeatPolicy = POLICY_ALWAYS;
		} else if ("sa_changed".equalsIgnoreCase(strPolicy)) {
			ackHeartbeatPolicy = POLICY_SA_CHANGED;
		} else {
			ackHeartbeatPolicy = POLICY_NEVER;
		}
	}

	/**
	 * 空实例
	 */
	private ClientStatMachine() {

	}

	/**
	 * 设定处理消息任务
	 * 
	 * @param messengerTask
	 */
	public void setMessengerTask(MessengerTask messengerTask) {
		this.messengerTaskRef = new WeakReference<MessengerTask>(messengerTask);
	}

	/**
	 * 取得消息处理任务
	 * 
	 * @return
	 */
	public MessengerTask getMessengerTask() {
		if (this.messengerTaskRef == null)
			return null;
		return this.messengerTaskRef.get();
	}

	/**
	 * 取得最后心跳时间
	 * 
	 * @return
	 */
	public long getLastTick() {
		return lastTick;
	}

	/**
	 * 取得最后心跳地址
	 * 
	 * @return
	 */
	public SocketAddress getLastAddr() {
		return lastAddr;
	}

	/**
	 * 取得最后通用命令时间
	 * 
	 * @return
	 */
	public long getLast0x10Time() {
		return last0x10Time;
	}

	/**
	 * 最后分类信息时间
	 * 
	 * @return
	 */
	public long getLast0x11Time() {
		return last0x11Time;
	}

	/**
	 * 最后自定义时间
	 * 
	 * @return
	 */
	public long getLast0x20Time() {
		return last0x20Time;
	}

	/**
	 * 取得自定义消息内容长度
	 * 
	 * @return
	 */
	public int getMessage0x20Len() {
		return this.message0x20Len;
	}

	/**
	 * 取得自定义消息内容数组
	 * 
	 * @return
	 */
	public byte[] getMessage0x20() {
		return this.message0x20;
	}

	/**
	 * 根据心跳包创建新的状态机
	 * 
	 * @param m
	 * @return
	 * @throws NullPointerException
	 */
	public static ClientStatMachine newByClientTick(ClientMessage m) throws NullPointerException {

		if (m == null) {
			return null;
		}
		if (createByClient == false) {
			return null;
		}
		// if(m.getSocketAddress() == null){
		// return null;
		// }
		if (m.getCmd() != ClientStatMachine.CMD_0x00) {
			return null;// 非心跳包不产生新的状态机，以后可能登录包也会产生新状态机
		}
		ClientStatMachine csm = new ClientStatMachine();
		// csm.lastAddr = address;不能设置该值，否则创建状态机后第一次onClientMessage不回发心跳了
		csm.lastTick = System.currentTimeMillis();
		return csm;
	}

	/**
	 * 根据push消息创建新的状态机
	 * 
	 * @param pm
	 * @return
	 * @throws NullPointerException
	 */
	public static ClientStatMachine newByPushReq(PushMessage pm) throws NullPointerException {

		if (pm == null) {
			return null;
		}

		ClientStatMachine csm = new ClientStatMachine();
		if (pm.getCmd() == ClientStatMachine.CMD_0x10) {
			csm.hasMessage0x10 = true;
			csm.last0x10Time = System.currentTimeMillis();
		} else if (pm.getCmd() == ClientStatMachine.CMD_0x11) {
			byte[] data = pm.getData();
			csm.message0x11 = ByteBuffer.wrap(data, data.length - 8, 8).getLong();
			csm.last0x11Time = System.currentTimeMillis();
		} else if (pm.getCmd() == ClientStatMachine.CMD_0x20) {
			csm.message0x20Len = pm.getContentLength();
			csm.last0x20Time = System.currentTimeMillis();
			csm.message0x20 = new byte[csm.message0x20Len];
			System.arraycopy(pm.getData(), Constant.PUSH_MSG_HEADER_LEN, csm.message0x20, 0, csm.message0x20Len);
		} else {
			return null;
		}
		csm.lastTick = System.currentTimeMillis();

		return csm;
	}

	/**
	 * 通过保存的文件创建状态机
	 * 
	 * @param lastTick
	 * @param hasMessage0x10
	 * @param last0x10Time
	 * @param message0x11
	 * @param last0x11Time
	 * @param message0x20Len
	 * @param last0x20Time
	 * @param message0x20
	 * @return
	 */
	public static ClientStatMachine newFromFile(long lastTick, boolean hasMessage0x10, long last0x10Time, long message0x11, long last0x11Time,
			int message0x20Len, long last0x20Time, byte[] message0x20) {
		ClientStatMachine csm = new ClientStatMachine();
		csm.lastTick = lastTick;
		csm.hasMessage0x10 = hasMessage0x10;
		csm.last0x10Time = last0x10Time;
		csm.message0x11 = message0x11;
		csm.last0x11Time = last0x11Time;
		csm.message0x20Len = message0x20Len;
		csm.last0x20Time = last0x20Time;
		csm.message0x20 = message0x20;
		return csm;
	}

	/**
	 * 是否有通用消息
	 * 
	 * @return
	 */
	public boolean has0x10Message() {
		return hasMessage0x10;
	}

	/**
	 * 是否有分类消息
	 * 
	 * @return
	 */
	public boolean has0x11Message() {
		if (this.message0x11 == 0) {
			return false;
		}
		return true;
	}

	/**
	 * 是否有自定义消息
	 * 
	 * @return
	 */
	public boolean has0x20Message() {
		if (this.message0x20 == null || this.message0x20Len <= 0) {
			return false;
		}
		return true;
	}

	/**
	 * 取得最后的分类消息
	 * 
	 * @return
	 */
	public long get0x11Message() {
		return this.message0x11;
	}

	/**
	 * 设定有最新的分类消息
	 */
	public void new0x10Message() {
		this.hasMessage0x10 = true;
		this.last0x10Time = System.currentTimeMillis();
	}

	/**
	 * 删除最后的分类消息
	 */
	public void clear0x10Message() {
		this.hasMessage0x10 = false;
		// this.last0x10Time = -1;
		// this.last0x10Time = System.currentTimeMillis();
	}

	/**
	 * 设定新的分类消息,或运算
	 * 
	 * @param newMessage
	 */
	public void new0x11Message(long newMessage) {
		this.message0x11 = this.message0x11 | newMessage;
		this.last0x11Time = System.currentTimeMillis();
	}

	/**
	 * 按命令，清除分类消息
	 * 
	 * @param confirm
	 */
	public void clear0x11Message(long confirm) {
		this.message0x11 = this.message0x11 & (~confirm);
	}

	/**
	 * 按命令内容，清除分类消息
	 * 
	 * @param array
	 * @param pos
	 * @throws Exception
	 */
	public void clear0x11Message(final byte[] array, int pos) throws Exception {
		if (array == null) {
			throw new NullPointerException("param byte array is null");
		}
		if (array.length < pos + 8) {
			throw new ArrayIndexOutOfBoundsException("illegal byte array length and position, at least 8 byte");
		}
		clear0x11Message(((ByteBuffer) ByteBuffer.wrap(array, pos, 8)).getLong());
	}

	/**
	 * 清除自定义消息
	 */
	public void clear0x20Message() {
		message0x20Len = 0;
		// last0x20Time = System.currentTimeMillis();
		message0x20 = null;
	}

	/**
	 * 向客户端push消息
	 * 
	 * @param pm
	 * @throws Exception
	 */
	public synchronized void onPushMessage(PushMessage pm) throws Exception {
		if (pm == null) {
			throw new NullPointerException("param push message is null");
		}
		if (pm.getCmd() == ClientStatMachine.CMD_0x10) {
			this.hasMessage0x10 = true;
			this.last0x10Time = System.currentTimeMillis();
			push0x10();
		} else if (pm.getCmd() == ClientStatMachine.CMD_0x11) {
			message0x11 = message0x11 | ByteBuffer.wrap(pm.getData(), Constant.PUSH_MSG_HEADER_LEN, 8).getLong();
			this.last0x11Time = System.currentTimeMillis();
			push0x11();
		} else if (pm.getCmd() == ClientStatMachine.CMD_0x20) {
			message0x20Len = pm.getContentLength();
			last0x20Time = System.currentTimeMillis();
			message0x20 = new byte[message0x20Len];
			System.arraycopy(pm.getData(), Constant.PUSH_MSG_HEADER_LEN, message0x20, 0, message0x20Len);
			push0x20();
		} else {
			// do nothing
		}
	}

	private void push0x10() throws Exception {
		if (this.hasMessage0x10 == false) {
			return;
		}
		if ((this.lastAddr == null || (System.currentTimeMillis() - this.lastTick) > 1000 * PUSH_IM_AFTER_ACTIVE_SECOND) && this.getMessengerTask() == null) {
			return;
		}
		byte[] data = new byte[Constant.SERVER_MESSAGE_MIN_LENGTH];// 5 bytes
		ByteBuffer bb = ByteBuffer.wrap(data);
		bb.put((byte) 1);// version
		bb.put((byte) 0);// app id,0 here
		bb.put((byte) ClientStatMachine.CMD_0x10);// cmd
		bb.putShort((short) 0);// length 0
		bb.flip();
		ServerMessage sm = new ServerMessage(this.lastAddr, data);
		if (this.lastAddr != null && (System.currentTimeMillis() - this.lastTick) < 1000 * PUSH_IM_AFTER_ACTIVE_SECOND) {
			IMServer.getInstance().pushInstanceMessage(sm);
		}
		if (this.getMessengerTask() != null) {
			try {
				this.getMessengerTask().pushInstanceMessage(sm);
			} catch (Exception e) {
			}
		}
	}

	private void push0x11() throws Exception {
		if (this.message0x11 == 0) {
			return;
		}
		if ((this.lastAddr == null || (System.currentTimeMillis() - this.lastTick) > 1000 * PUSH_IM_AFTER_ACTIVE_SECOND) && this.getMessengerTask() == null) {
			return;
		}
		byte[] data = new byte[Constant.SERVER_MESSAGE_MIN_LENGTH + 8];// 13
																		// bytes
		ByteBuffer bb = ByteBuffer.wrap(data);
		bb.put((byte) 1);// version
		bb.put((byte) 0);// app id, 0 here
		bb.put((byte) ClientStatMachine.CMD_0x11);// cmd
		bb.putShort((short) 8);// length 8
		bb.putLong(message0x11);
		bb.flip();
		ServerMessage sm = new ServerMessage(this.lastAddr, data);
		if (this.lastAddr != null && (System.currentTimeMillis() - this.lastTick) < 1000 * PUSH_IM_AFTER_ACTIVE_SECOND) {
			IMServer.getInstance().pushInstanceMessage(sm);
		}
		if (this.getMessengerTask() != null) {
			try {
				this.getMessengerTask().pushInstanceMessage(sm);
			} catch (Exception e) {
			}
		}
	}

	private void push0x20() throws Exception {
		if (has0x20Message() == false) {
			return;
		}
		if ((this.lastAddr == null || (System.currentTimeMillis() - this.lastTick) > 1000 * PUSH_IM_AFTER_ACTIVE_SECOND) && this.getMessengerTask() == null) {
			return;
		}
		byte[] data = new byte[Constant.SERVER_MESSAGE_MIN_LENGTH + message0x20Len];
		ByteBuffer bb = ByteBuffer.wrap(data);
		bb.put((byte) 1);// version
		bb.put((byte) 0);// app id, 0 here
		bb.put((byte) ClientStatMachine.CMD_0x20);// cmd
		bb.putShort((short) message0x20Len);
		bb.put(this.message0x20);
		bb.flip();
		ServerMessage sm = new ServerMessage(this.lastAddr, data);
		if (this.lastAddr != null && (System.currentTimeMillis() - this.lastTick) < 1000 * PUSH_IM_AFTER_ACTIVE_SECOND) {
			IMServer.getInstance().pushInstanceMessage(sm);
		}
		if (this.getMessengerTask() != null) {
			try {
				this.getMessengerTask().pushInstanceMessage(sm);
			} catch (Exception e) {
			}
		}
	}

	/*
	 * 返回true代表有消息需通知用户，返回false代表无消息需通知
	 */
	public synchronized ArrayList<ServerMessage> onClientMessage(ClientMessage cm) throws Exception {

		if (cm == null) {
			throw new NullPointerException("param client message is null");
		}

		// if(cm.getSocketAddress() == null){
		// throw new NullPointerException("client socket address is null");
		// }
		ArrayList<ServerMessage> smList = new ArrayList<ServerMessage>();
		if (cm.getCmd() == ClientStatMachine.CMD_0x00) {// 心跳
			// nothing to do
		} else if (cm.getCmd() == ClientStatMachine.CMD_0x10) {// 确认通用信息
			this.clear0x10Message();
			return smList;
		} else if (cm.getCmd() == ClientStatMachine.CMD_0x11) {// 确认分组信息
			this.clear0x11Message(cm.getData(), Constant.CLIENT_MESSAGE_MIN_LENGTH);
			return smList;
		} else if (cm.getCmd() == ClientStatMachine.CMD_0x20) {// 确认自定义信息
			this.clear0x20Message();
			return smList;
		} else {// 非法命令
			return null;
		}

		this.genServerMessageList(cm, smList);

		return smList;
	}

	private void genServerMessageList(ClientMessage cm, ArrayList<ServerMessage> smList) throws Exception {
		this.lastTick = System.currentTimeMillis();
		boolean needTickBack = false;
		if (cm.getSocketAddress() == null) {
			needTickBack = false;
		} else if (ackHeartbeatPolicy == POLICY_ALWAYS) {
			needTickBack = true;
		} else if (ackHeartbeatPolicy == POLICY_NEVER) {
			needTickBack = false;
		} else {
			if (cm.getSocketAddress().equals(lastAddr)) {// 最新地址和上次地址一致，不回应心跳包
				needTickBack = false;
			} else {// 地址改变，回应心跳包
				needTickBack = true;
			}
		}
		if (cm.getSocketAddress() != null) {
			lastAddr = cm.getSocketAddress();
		}
		gen0x10Message(cm, smList);
		gen0x11Message(cm, smList);
		gen0x20Message(cm, smList);
		if (needTickBack == true && smList.size() == 0) {
			gen0x00Message(cm, smList);
		}

	}

	/**
	 * 下发心跳通知
	 * 
	 * @param cm
	 * @param smList
	 * @throws Exception
	 */
	private void gen0x00Message(ClientMessage cm, ArrayList<ServerMessage> smList) throws Exception {
		byte[] data = new byte[Constant.SERVER_MESSAGE_MIN_LENGTH];// 5 bytes
		ByteBuffer bb = ByteBuffer.wrap(data);
		bb.put((byte) 1);// version
		bb.put(cm.getData()[1]);// app id
		bb.put((byte) ClientStatMachine.CMD_0x00);// cmd
		bb.putShort((short) 0);// length 0
		bb.flip();
		ServerMessage sm = new ServerMessage(cm.getSocketAddress(), data);
		smList.add(sm);
	}

	/**
	 * 下发通用消息
	 * 
	 * @param cm
	 * @param smList
	 * @throws Exception
	 */
	private void gen0x10Message(ClientMessage cm, ArrayList<ServerMessage> smList) throws Exception {
		if (!hasMessage0x10) {
			return;
		}
		byte[] data = new byte[Constant.SERVER_MESSAGE_MIN_LENGTH];// 5 bytes
		ByteBuffer bb = ByteBuffer.wrap(data);
		bb.put((byte) 1);// version
		bb.put(cm.getData()[1]);// app id
		bb.put((byte) ClientStatMachine.CMD_0x10);// cmd
		bb.putShort((short) 0);// length 0
		bb.flip();
		ServerMessage sm = new ServerMessage(cm.getSocketAddress(), data);
		smList.add(sm);

	}

	/**
	 * 下发自定义消息
	 * 
	 * @param cm
	 * @param smList
	 * @throws Exception
	 */
	private void gen0x20Message(ClientMessage cm, ArrayList<ServerMessage> smList) throws Exception {
		if (this.has0x20Message() == false) {
			return;
		}
		byte[] data = new byte[Constant.SERVER_MESSAGE_MIN_LENGTH + message0x20Len];// 5+length
																					// bytes
		ByteBuffer bb = ByteBuffer.wrap(data);
		bb.put((byte) 1);// version
		bb.put(cm.getData()[1]);// app id
		bb.put((byte) ClientStatMachine.CMD_0x20);// cmd
		bb.putShort((short) message0x20Len);// length
		bb.put(message0x20);
		bb.flip();
		ServerMessage sm = new ServerMessage(cm.getSocketAddress(), data);
		smList.add(sm);

	}

	/**
	 * 下发分类消息
	 * 
	 * @param cm
	 * @param smList
	 * @throws Exception
	 */
	private void gen0x11Message(ClientMessage cm, ArrayList<ServerMessage> smList) throws Exception {
		if (message0x11 == 0) {
			return;
		}
		byte[] data = new byte[Constant.SERVER_MESSAGE_MIN_LENGTH + 8];// 13
																		// bytes
		ByteBuffer bb = ByteBuffer.wrap(data);
		bb.put((byte) 1);// version
		bb.put(cm.getData()[1]);// app id
		bb.put((byte) ClientStatMachine.CMD_0x11);// cmd
		bb.putShort((short) 8);// length 8
		bb.putLong(message0x11);
		bb.flip();
		ServerMessage sm = new ServerMessage(cm.getSocketAddress(), data);
		smList.add(sm);
	}

}
