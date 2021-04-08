package org.idempiere.dms.factories;

public class DefaultDMSPermissionFactory implements IPermissionFactory
{
	@Override
	public IPermission getPermission( )
	{
		return new DMSPermissionValidator();
	}
}
