/*******************************************************************************
 * This file is part of OpenNMS(R).
 *
 * Copyright (C) 2013-2014 The OpenNMS Group, Inc.
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

package org.opennms.netmgt.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.type.EnumType;
import org.hibernate.type.IntegerType;
import org.opennms.netmgt.model.OspfElement.Status;

public class StatusUserType extends EnumType {

    private static final long serialVersionUID = 2935892942529340988L;

    private static final int[] SQL_TYPES = new int[] { java.sql.Types.INTEGER };

	/**
     * A public default constructor is required by Hibernate.
     */
    public StatusUserType() {}

    @Override
    public int hashCode(final Object x) throws HibernateException {
        return x == null ? 0 : x.hashCode();
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, SessionImplementor session, Object owner) throws SQLException {
        Integer c = IntegerType.INSTANCE.nullSafeGet(rs, names[0], session);
        if (c == null) {
            return null;
        }
        for (Status type : Status.values()) {
            if (type.getValue().intValue() == c.intValue()) {
                return type;
            }
        }
        throw new HibernateException("Invalid value for Status: " + c);
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index, SessionImplementor session) throws HibernateException, SQLException {
        if (value == null) {
            IntegerType.INSTANCE.nullSafeSet(st, null, index, session);
        } else if (value instanceof Status){
            IntegerType.INSTANCE.nullSafeSet(st, ((Status)value).getValue(), index, session);
        }
    }

    @Override
    public Class<Status> returnedClass() {
        return Status.class;
    }

    @Override
    public int[] sqlTypes() {
        return SQL_TYPES;
    }

    @Override
    public void setParameterValues(Properties parameters) {
    }
}
