package org.idempiere.dms.factories;

import org.idempiere.dms.DMS;
import org.idempiere.dms.form.WUploadContent;
import org.idempiere.model.I_DMS_Content;

/**
 * Default factory for Upload Content in DMS
 * 
 * @author Sachin Bhimani
 */
public class DefaultDMSUploadContentFactory implements IDMSUploadContentFactory
{

	@Override
	public IDMSUploadContent getUploadForm(DMS dms, I_DMS_Content content, boolean isVersion, int tableID, int recordID, int windowNo, int tabNo)
	{
		return new WUploadContent(dms, content, isVersion, tableID, recordID, windowNo, tabNo);
	}

}
