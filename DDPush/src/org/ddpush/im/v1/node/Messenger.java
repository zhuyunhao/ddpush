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

import java.util.ArrayList;

import org.ddpush.im.v1.node.udpconnector.UdpConnector;

/**
 * UDP消息管理者
 */
public class Messenger implements Runnable {

	/** UDP处理者 */
	private UdpConnector connector;
	/** 节点状态机 */
	private NodeStatus nodeStat;// this is very large and dynamic
	/** 地址管理线程 */
	private Thread hostThread;
	/** 开始状态位 */
	boolean started = false;
	/** 结束状态位 */
	boolean stoped = false;

	/**
	 * 初始化
	 * 
	 * @param connector
	 * @param nodeStat
	 */
	public Messenger(UdpConnector connector, NodeStatus nodeStat) {
		this.connector = connector;
		this.nodeStat = nodeStat;
	}

	@Override
	public void run() {
		this.started = true;

		while (stoped == false) {
			try {
				procMessage();
			} catch (Exception e) {
				e.printStackTrace();
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}

	}

	public void stop() {
		this.stoped = true;
	}

	private void procMessage() throws Exception {
		ClientMessage m = this.obtainMessage();
		if (m == null) {
			try {
				Thread.sleep(5);
			} catch (Exception e) {
				e.printStackTrace();
			}
			return;
		}

		this.deliverMessage(m);

	}

	/**
	 * push消息
	 * 
	 * @param m
	 * @throws Exception
	 */
	private void deliverMessage(ClientMessage m) throws Exception {
		// System.out.println(this.hostThread.getName()+" receive:"+StringUtil.convert(m.getData()));
		// System.out.println(m.getSocketAddress().getClass().getName());
		// 根据客户端消息取得UUID
		String uuid = m.getUuidHexString();
		// ClientStatMachine csm = NodeStatus.getInstance().getClientStat(uuid);
		// 取得此客戶端的狀態
		ClientStatMachine csm = nodeStat.getClientStat(uuid);
		if (csm == null) {
			// 如果状态机不存在，创建新的状态机
			csm = ClientStatMachine.newByClientTick(m);
			if (csm == null) {
				return;
			}
			nodeStat.putClientStat(uuid, csm);
		}
		// 收到客户端消息
		ArrayList<ServerMessage> smList = csm.onClientMessage(m);
		if (smList == null) {
			return;
		}
		// 回执客户端消息
		for (int i = 0; i < smList.size(); i++) {
			ServerMessage sm = smList.get(i);
			if (sm.getSocketAddress() == null)
				continue;
			this.connector.send(sm);
		}

	}

	/**
	 * 申请处理消息
	 * 
	 * @return
	 * @throws Exception
	 */
	private ClientMessage obtainMessage() throws Exception {
		return connector.receive();
	}

	/**
	 * 设定地址管理线程
	 * 
	 * @param t
	 */
	public void setHostThread(Thread t) {
		this.hostThread = t;
	}

	public Thread getHostThread() {
		return this.hostThread;
	}

}
