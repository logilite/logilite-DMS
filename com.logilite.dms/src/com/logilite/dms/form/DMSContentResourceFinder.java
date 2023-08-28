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
package com.logilite.dms.form;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.logging.Level;

import org.adempiere.base.IResourceFinder;
import org.compiere.util.CLogger;

/**
 * DMS Content related resource finder from the current plug-in like JS, Images, etc.
 * 
 * @author Logilite Technologies
 */
public class DMSContentResourceFinder implements IResourceFinder
{
	public static final CLogger log = CLogger.getCLogger(DMSContentResourceFinder.class);

	@Override
	public URL getResource(String name)
	{
		try
		{
			Enumeration<URL> eUrl = this.getClass().getClassLoader().getResources(name);
			return eUrl != null && eUrl.hasMoreElements() ? eUrl.nextElement() : null;
		}
		catch (IOException e)
		{
			log.log(Level.SEVERE, "Issue while loading resource " + name + " cause :" + e.getLocalizedMessage(), e);
		}
		return null;
	}
}
