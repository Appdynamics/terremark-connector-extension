/*
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */
package com.appdynamics.connectors.terremark;

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
