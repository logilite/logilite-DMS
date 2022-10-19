package org.idempiere.dms.factories;

import org.idempiere.dms.DMS;
import org.idempiere.model.I_DMS_Content;

/**
 * Use as a factory for Upload Content in DMS
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public interface IDMSUploadContentFactory
{

	IDMSUploadContent getUploadForm(DMS dms, I_DMS_Content content, boolean isVersion, int tableID, int recordID, int windowNo, int tabNo);

}
