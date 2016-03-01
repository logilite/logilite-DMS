package org.idempiere.webui.apps.form;

import org.adempiere.webui.panel.ADForm;

public class WDocumentExplorer extends ADForm
{

	/**
	 * 
	 */
	private static final long	serialVersionUID	= 4615438048526323068L;

	/* (non-Javadoc)
	 * @see org.adempiere.webui.panel.ADForm#initForm()
	 */
	@Override
	protected void initForm()
	{
		this.setHeight("100%");
		this.setWidth("100%");

		WDMSPanel docviewer = new WDMSPanel();
		this.appendChild(docviewer);
	}

}
