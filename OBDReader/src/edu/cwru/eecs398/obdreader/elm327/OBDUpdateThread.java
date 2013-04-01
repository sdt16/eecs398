package edu.cwru.eecs398.obdreader.elm327;


/*
 * OBDUpdateThread.java
 *
 *
 * Copyright (c) 2007-2008 Tim Wootton <tim@tee-jay.demon.co.uk>
 *
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) anyradians_radians_later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

import java.util.BitSet;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 *
 * @author Tim Wootton <tim@tee-jay.demon.co.uk>
 */
public class OBDUpdateThread extends Thread{
	public static enum Priority{
		OBD_PRIORITY_HIGH,
		OBD_PRIORITY_MEDIUM,
		OBD_PRIORITY_LOW,
		OBD_PRIORITY_FF
	}

	private boolean time_to_stop;
	private final OBDProtocolHandler obd;

	private final HashSet<Byte> h_q = new HashSet<Byte>();
	private final HashSet<Byte> m_q = new HashSet<Byte>();
	private final HashSet<Byte> l_q = new HashSet<Byte>();
	private final HashSet<Byte> ff_q = new HashSet<Byte>();
	private final HashMap<Byte,HashSet<OBDListener>> pids = new HashMap<Byte,HashSet<OBDListener>>();
	BitSet pid_map;

	/** Creates a new instance of OBDUpdateThread */
	public OBDUpdateThread(final OBDProtocolHandler obd) {
		this.obd = obd;
		this.start();
	}

	public BitSet getPidMap(){
		if (pid_map == null) {
			pid_map = obd.getSupportedParamIds();
		}
		return pid_map;
	}

	public void addListener(final Priority priority, final byte pid, final OBDListener listener){
		final HashSet<OBDListener> listeners;

		synchronized (pids){
			switch (priority){
			case OBD_PRIORITY_HIGH:
				synchronized (h_q){
					if (!h_q.contains(pid)) {
						h_q.add(pid);
					}
				}
				break;
			case OBD_PRIORITY_MEDIUM:
				synchronized (m_q){
					if (!m_q.contains(pid)) {
						m_q.add(pid);
					}
				}
				break;
			case OBD_PRIORITY_LOW:
				synchronized (l_q){
					if (!l_q.contains(pid)) {
						l_q.add(pid);
					}
				}
				break;
			case OBD_PRIORITY_FF:
				synchronized (ff_q){
					if (!ff_q.contains(pid)) {
						ff_q.add(pid);
					}
				}
				break;
			}

			if (pids.containsKey(pid)){
				listeners = pids.get(pid);
				listeners.add(listener);
			}
			else{
				listeners = new HashSet<OBDListener>();
				listeners.add(listener);
				pids.put(pid,listeners);
			}
		}
	}

	public void removeListener(final byte pid, final OBDListener listener){
		final HashSet<OBDListener> listeners;

		synchronized (pids){
			if (pids.containsKey(pid)){
				listeners = pids.get(pid);
				if(listeners.contains(listener)) {
					listeners.remove(listener);
				}
				if (listeners.isEmpty()){
					pids.remove(pid);
					synchronized (h_q){
						if (h_q.contains(pid)) {
							h_q.remove(pid);
						}
					}
					synchronized (m_q){
						if (m_q.contains(pid)) {
							m_q.remove(pid);
						}
					}
					synchronized (l_q){
						if (l_q.contains(pid)) {
							l_q.remove(pid);
						}
					}
					synchronized (ff_q){
						if (ff_q.contains(pid)) {
							ff_q.remove(pid);
						}
					}
				}
			}
		}
	}

	@Override
	public void run(){
		byte pid;
		Iterator lqi, mqi, hqi, ffqi;
		time_to_stop = false;
		while (!time_to_stop){
			synchronized(pids){
				try{
					synchronized (h_q){
						hqi = h_q.iterator();
						while (hqi.hasNext()){
							pid = (Byte)hqi.next();
							processPid(pid);
						}
					}
					synchronized (m_q){
						mqi = m_q.iterator();
						while (mqi.hasNext()){
							synchronized (h_q){
								hqi = h_q.iterator();
								while (hqi.hasNext()){
									pid = (Byte)hqi.next();
									processPid(pid);
								}
							}
							pid = (Byte)mqi.next();
							processPid(pid);
						}
					}
					synchronized (l_q){
						lqi = l_q.iterator();
						while (lqi.hasNext()){
							synchronized (m_q){
								mqi = m_q.iterator();
								while (mqi.hasNext()){
									synchronized (h_q){
										hqi = h_q.iterator();
										while (hqi.hasNext()){
											pid = (Byte)hqi.next();
											processPid(pid);
										}
									}
									pid = (Byte)mqi.next();
									processPid(pid);
								}
							}
							pid = (Byte)lqi.next();
							processPid(pid);
						}
					}
					synchronized (ff_q){
						ffqi = ff_q.iterator();
						while (ffqi.hasNext()){
							synchronized (l_q){
								lqi = l_q.iterator();
								while (lqi.hasNext()){
									synchronized (m_q){
										mqi = m_q.iterator();
										while (mqi.hasNext()){
											synchronized (h_q){
												hqi = h_q.iterator();
												while (hqi.hasNext()){
													pid = (Byte)hqi.next();
													processPid(pid);
												}
											}
											pid = (Byte)mqi.next();
											processPid(pid);
										}
									}
									pid = (Byte)lqi.next();
									processPid(pid);
								}
							}
							pid = (Byte)ffqi.next();
							processPid(pid);
						}
					}
				}
				catch(final ConcurrentModificationException c){}
			}
			yield();

		}

	}

	private void processPid(final byte pid){
		OBDParameter p;
		byte sub = 0;
		p = obd.getParam(pid,sub,false);
		sendToListeners(p);
		while (p.hasMore()){
			sub++;
			p = obd.getParam(pid,sub,false);
			sendToListeners(p);
		}
		yield();                                                                //micro nap
	}

	private void sendToListeners(final OBDParameter param){
		HashSet<OBDListener> listeners;
		Iterator listit;
		OBDListener listener;

		if (pids.containsKey(param.pid)){
			listeners = pids.get(param.pid);
			listit = listeners.iterator();
			while (listit.hasNext()){
				listener = (OBDListener)listit.next();
				listener.OBDUpdate(param);
			}
		}

	}

}