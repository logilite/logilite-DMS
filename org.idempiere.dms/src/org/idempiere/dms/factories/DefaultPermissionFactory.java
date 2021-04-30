package org.idempiere.dms.factories;

public class DefaultPermissionFactory implements IPermissionFactory
{
	@Override
	public IPermissionManager getPermissionManager()
	{
		return new DefaultPermission();
	}
}
