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

package org.idempiere.webui.apps.form;

import java.util.logging.Level;

import org.adempiere.exceptions.AdempiereException;
import org.adempiere.webui.panel.ADForm;
import org.compiere.util.CLogger;

public class WDocumentExplorer extends ADForm
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 4615438048526323068L;

	private static CLogger		log					= CLogger.getCLogger(WDocumentExplorer.class);

	/*
	 * (non-Javadoc)
	 * @see org.adempiere.webui.panel.ADForm#initForm()
	 */
	@Override
	protected void initForm()
	{
		this.setHeight("100%");
		this.setWidth("100%");

		try
		{
			WDMSPanel docviewer = new WDMSPanel();
			docviewer.renderViewer();
			this.appendChild(docviewer);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Render component problem", e);
			throw new AdempiereException("Render component problem: " + e.getLocalizedMessage());
		}
	}

}
