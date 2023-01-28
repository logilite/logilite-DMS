package com.logilite.dms.factories;

public class DefaultPermissionFactory implements IPermissionFactory
{
	@Override
	public IPermissionManager getPermissionManager()
	{
		return new DefaultPermission();
	}
}
