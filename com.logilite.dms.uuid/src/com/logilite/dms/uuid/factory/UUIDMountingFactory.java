package com.logilite.dms.uuid.factory;

import com.logilite.dms.factories.IMountingFactory;
import com.logilite.dms.factories.IMountingStrategy;
import com.logilite.dms.uuid.classes.UUIDContentManager;
import com.logilite.dms.uuid.classes.UUIDMountingStrategy;

/**
 * UUID Mounting Factory
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public class UUIDMountingFactory implements IMountingFactory
{

	@Override
	public IMountingStrategy getMountingStrategy(String contentManagerType, String Table_Name)
	{
		if (UUIDContentManager.KEY.equals(contentManagerType))
			return new UUIDMountingStrategy();

		return null;
	}

}
