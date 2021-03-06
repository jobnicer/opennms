/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2011-2014 The OpenNMS Group, Inc.
 * OpenNMS(R) is Copyright (C) 1999-2014 The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
 *
 * OpenNMS(R) is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * OpenNMS(R) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with OpenNMS(R).  If not, see:
 *      http://www.gnu.org/licenses/
 *
 * For more information contact:
 *     OpenNMS(R) Licensing <license@opennms.org>
 *     http://www.opennms.org/
 *     http://www.opennms.com/
 *******************************************************************************/

package org.opennms.netmgt.scriptd.helper;

import java.net.UnknownHostException;

import org.opennms.netmgt.xml.event.Event;

public class SnmpV2InformAlarmForwarder extends SnmpTrapForwarderHelper implements
		EventForwarder {


	public SnmpV2InformAlarmForwarder(String ip, int port, String community, int timeout, int retries, SnmpTrapHelper snmpTrapHelper) {
		super(ip, port, community, timeout, retries, snmpTrapHelper);
	}

        @Override
	public void flushEvent(Event event) {
		event =	super.filter(event);
		if (event != null){
		try {
			sendV2AlarmInform(event, false);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SnmpTrapHelperException e) {
			e.printStackTrace();
		}
		}		
	}

        @Override
	public void flushSyncEvent(Event event) {
		event =	super.filter(event);
		if (event != null) {
		try {
			sendV2AlarmInform(event, true);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (SnmpTrapHelperException e) {
			e.printStackTrace();
		}
		}
	}

        @Override
	public void sendStartSync() {
		super.sendV2StartSyncInform();
	}

        @Override
	public void sendEndSync() {
		super.sendV2EndSyncInform();		
	}
	
}
