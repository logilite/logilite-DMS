package com.logilite.dms.dmscleanuponrecorddelete.factory;

import org.adempiere.base.IModelValidatorFactory;
import org.compiere.model.ModelValidator;

import com.logilite.dms.dmscleanuponrecorddelete.model.DMSCleanupOnRecordDeleteModelValidator;

/**
 * DMS Cleanup On Record Delete Factory
 * 
 * @author Logilite technology
 */
public class DMSCleanupModelValidatorFactory implements IModelValidatorFactory
{

	@Override
	public ModelValidator newModelValidatorInstance(String className)
	{
		if (DMSCleanupOnRecordDeleteModelValidator.class.getName().equals(className))
			return new DMSCleanupOnRecordDeleteModelValidator();
		return null;
	}
} // DMSCleanupOnModelValidatorFactory
