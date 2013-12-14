/**
 * Copyright 2013 AppDynamics, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
