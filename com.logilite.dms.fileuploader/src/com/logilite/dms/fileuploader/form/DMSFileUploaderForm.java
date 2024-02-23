package com.logilite.dms.fileuploader.form;

import org.adempiere.webui.panel.ADForm;
import org.adempiere.webui.panel.CustomForm;
import org.adempiere.webui.panel.IFormController;

import com.logilite.dms.fileuploader.WUploadContentForm;

public class DMSFileUploaderForm implements IFormController
{
	@Override
	public ADForm getForm()
	{
		CustomForm form = new CustomForm();
		form.appendChild(new WUploadContentForm());
		return form;
	}
}
