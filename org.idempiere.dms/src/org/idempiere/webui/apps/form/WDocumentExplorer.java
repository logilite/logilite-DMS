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
			this.appendChild(docviewer);
		}
		catch (Exception e)
		{
			log.log(Level.SEVERE, "Render component problem", e);
			throw new AdempiereException("Render component problem: " + e.getLocalizedMessage());
		}
	}

}
