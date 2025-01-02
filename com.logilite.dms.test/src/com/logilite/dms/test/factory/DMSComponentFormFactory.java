package com.logilite.dms.test.factory;

import org.adempiere.webui.factory.IFormFactory;
import org.adempiere.webui.panel.ADForm;

import com.logilite.dms.test.form.DMSComponentSampleForm;

/**
 * DMS component sample Form Factory
 */
public class DMSComponentFormFactory implements IFormFactory
{
	@Override
	public ADForm newFormInstance(String formName)
	{
		if (formName.equals("com.logilite.dms.test.form.DMSComponentSampleForm"))
			return new DMSComponentSampleForm();
		return null;
	}
}
