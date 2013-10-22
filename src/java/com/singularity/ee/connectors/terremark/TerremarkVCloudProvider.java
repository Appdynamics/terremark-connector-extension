package com.singularity.ee.connectors.terremark;

import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.rest.RestContext;
import org.jclouds.vcloud.terremark.TerremarkVCloudAsyncClient;
import org.jclouds.vcloud.terremark.TerremarkVCloudClient;

public class TerremarkVCloudProvider
{

	private final ComputeServiceContext computeServiceCtx;
	private final RestContext<TerremarkVCloudAsyncClient, TerremarkVCloudClient> provider;

	public TerremarkVCloudProvider(ComputeServiceContext computeServiceCtx)
	{
		this.computeServiceCtx = computeServiceCtx;
		this.provider = this.computeServiceCtx.getProviderSpecificContext();
	}

	public TerremarkVCloudClient getSyncApi()
	{
		return provider.getApi();
	}
	
	public TerremarkVCloudAsyncClient getAsyncApi()
	{
		return provider.getAsyncApi();
	}

	public ComputeService getComputeService()
	{
		return computeServiceCtx.getComputeService();
	}
}
