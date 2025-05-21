package com.logilite.dms.contentdirdeduplication.factory;

import org.adempiere.base.IProcessFactory;
import org.compiere.process.ProcessCall;

import com.logilite.dms.contentdirdeduplication.process.ContentDirDeDuplication;

public class ContentDirDeDuplicationFactory implements IProcessFactory
{

	@Override
	public ProcessCall newProcessInstance(String className)
	{
		if (className.equals(ContentDirDeDuplication.class.getName()))
			return new ContentDirDeDuplication();
		return null;
	}

}
