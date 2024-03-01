package com.logilite.dms.fileuploader.form;

import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.CustomForm;
import org.adempiere.webui.panel.IFormController;

public class DMSFileUploaderForm implements IFormController
{
	@Override
	public ADForm getForm()
	{
		CustomForm form = new CustomForm();
		form.appendChild(new WDMSBulkUploadForm());
		return form;
	}
}
