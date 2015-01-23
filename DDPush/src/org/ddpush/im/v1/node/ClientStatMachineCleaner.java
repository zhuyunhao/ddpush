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

import org.ddpush.im.util.PropertyUtil;

/**
 * 状态机清除器
 */
public class ClientStatMachineCleaner implements Runnable {

	/** 停止标志位 */
	private boolean stoped = false;
	/** 最后清除时间 */
	private long lastCleanTime = 0;
	/** 过期小时数 */
	private int expiredHours = PropertyUtil.getPropertyInt("CLEANER_DEFAULT_EXPIRED_HOURS");;

	@Override
	public void run() {
		while (!stoped) {
			try {
				synchronized (this) {
					this.wait();
					if (stoped == true) {
						return;
					}
					doClean();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println("cleaner quit");

	}

	public void wakeup() {
		synchronized (this) {
			this.notifyAll();
		}
	}

	private void doClean() {
		lastCleanTime = System.currentTimeMillis();
		System.out.println("clearn stat of expired hours of " + expiredHours + "....");
		System.out.println("max   mem: " + Runtime.getRuntime().maxMemory());
		System.out.println("total mem: " + Runtime.getRuntime().totalMemory());
		System.out.println("free  mem: " + Runtime.getRuntime().freeMemory());
		System.gc();
		try {
			int removed = NodeStatus.getInstance().cleanStatus(expiredHours);
			System.out.println("clean " + removed + " expired stat machines of expired hours of " + expiredHours);
			lastCleanTime = System.currentTimeMillis();
			System.gc();
			System.out.println("gc committed");
			System.out.println("max   mem: " + Runtime.getRuntime().maxMemory());
			System.out.println("total mem: " + Runtime.getRuntime().totalMemory());
			System.out.println("free  mem: " + Runtime.getRuntime().freeMemory());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 设置停止标志位
	 */
	public void stop() {
		stoped = true;
	}

	/**
	 * 设定过期小时数
	 * 
	 * @param expiredHours
	 */
	public void setExpiredHours(int expiredHours) {
		this.expiredHours = expiredHours;
	}

	/**
	 * 取得最后清除时间
	 * 
	 * @return
	 */
	public long getLastCleanTime() {
		return this.lastCleanTime;
	}

}
