package com.logilite.dms.factories;

/**
 * Index Query Build Factory
 * 
 * @author Sachin Bhimani @ Logilite Technologies
 */
public interface IIndexQueryBuildFactory
{
	public IIndexQueryBuilder get(String indexingType);
}
