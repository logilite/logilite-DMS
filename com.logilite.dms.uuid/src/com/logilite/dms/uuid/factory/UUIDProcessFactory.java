package com.logilite.dms.uuid.factory;

import org.adempiere.base.IProcessFactory;
import org.compiere.process.ProcessCall;

import com.logilite.dms.uuid.process.ConvertRelationalToRelationalUUID;

/**
 * Process factory for DMS UUID
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class UUIDProcessFactory implements IProcessFactory
{

	@Override
	public ProcessCall newProcessInstance(String className)
	{
		if (className.equals(ConvertRelationalToRelationalUUID.class.getName()))
			return new ConvertRelationalToRelationalUUID();

		return null;
	}

}
