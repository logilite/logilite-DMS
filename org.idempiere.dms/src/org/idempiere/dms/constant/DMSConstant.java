/******************************************************************************
 * Copyright (C) 2016 Logilite Technologies LLP								  *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/

package org.idempiere.dms.constant;

import org.compiere.util.Env;
import org.compiere.util.Msg;

/**
 * DMS Constant
 * 
 * @author Sachin
 */
public final class DMSConstant
{

	public static final int		MAX_FILENAME_LENGTH	= 250;

	public static final String	REG_EXP_FILENAME	= "^[A-Za-z0-9\\s\\-\\._\\(\\)]+$";
	public static final String	REG_SPACE			= "\\S+";

	public static String		MSG_FILL_MANDATORY	= Msg.translate(Env.getCtx(), "FillMandatory");

}
