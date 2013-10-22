package com.singularity.ee.connectors.terremark;

import java.util.HashMap;
import java.util.Map;

import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.domain.ComputeMetadata;
import org.jclouds.vcloud.terremark.TerremarkVCloudContextFactory;

import com.singularity.ee.connectors.api.IControllerServices;
import com.singularity.ee.connectors.entity.api.IComputeCenter;
import com.singularity.ee.connectors.entity.api.IImageStore;
import com.singularity.ee.connectors.entity.api.IProperty;

class ConnectorLocator
{
	private static final String ACCOUNT_USER_NAME = "Account User Name";
	private static final String PASSWORD = "Password";

	private static final ConnectorLocator INSTANCE = new ConnectorLocator();

	private final Map<String, TerremarkVCloudProvider> userNameVsComputeServiceCtx =
			new HashMap<String, TerremarkVCloudProvider>();

	private final Object connectorLock = new Object();

	/**
	 * Private constructor on singleton.
	 */
	private ConnectorLocator()
	{
	}

	public static ConnectorLocator getInstance()
	{
		return INSTANCE;
	}

	public TerremarkVCloudProvider getConnector(IComputeCenter computeCenter,
			IControllerServices controllerServices)
	{
		return getConnector(computeCenter.getProperties(), controllerServices, false);
	}

	public TerremarkVCloudProvider getConnector(IImageStore imageStore,
			IControllerServices controllerServices)
	{
		return getConnector(imageStore.getProperties(), controllerServices, false);
	}

	public TerremarkVCloudProvider getConnector(IProperty[] properties,
			IControllerServices controllerServices, boolean validate)
	{
		String userName = controllerServices.getStringPropertyValueByName(
				properties, ACCOUNT_USER_NAME);
		String password = controllerServices.getStringPropertyValueByName(
				properties, PASSWORD);

		return getConnector(userName, password, validate);
	}

	private TerremarkVCloudProvider getConnector(String userName, String password,
			boolean validate)
	{
		synchronized (connectorLock)
		{
			TerremarkVCloudProvider provider = userNameVsComputeServiceCtx.get(userName);
			if (provider != null)
				return provider;

			ComputeServiceContext computeServiceCtx = TerremarkVCloudContextFactory
					.createContext(userName, password);

			provider = new TerremarkVCloudProvider(computeServiceCtx);
			
			if(validate)
			{
				provider.getComputeService().getImages();
			}
			userNameVsComputeServiceCtx.put(userName, provider);

			return provider;
		}
	}
	
	public static void main(String[] args)
	{
		//$Ep455l0ud!2
		// id = 332841
		TerremarkVCloudProvider connector = ConnectorLocator.getInstance().getConnector(
				"jbansal@appdynamics.com", "274brannan", false);
		
		ComputeService computeService = connector.getComputeService();

//		TemplateBuilder template = computeService.templateBuilder().imageId("9").smallest();
//		
//		long startTime = System.currentTimeMillis();
//		
//		computeService.runNodesWithTag("server", 2, template.build());
//		
//		System.out.println("Time taken " + (System.currentTimeMillis() - startTime));
//		
//		System.out.println(computeService.getNodesWithTag("VEk").get("332780"));
		
		
		System.out.println("===========");
		for(ComputeMetadata node: computeService.getNodes().values())
		{
			System.out.println(node);
		}
//
//		InstantiateVAppTemplateOptions options = InstantiateVAppTemplateOptions
//		.Builder.processorCount(8).memory(1024 * 4);
//
//TerremarkVCloudClient syncApi = connector.getSyncApi();
//
//long startTime = System.currentTimeMillis();
// VApp vApp = syncApi.instantiateVAppTemplateInVDC(syncApi.getDefaultVDC().getId(),
//		"AD" + System.currentTimeMillis(), "9", options);
//
// System.out.println("Time taken " + (System.currentTimeMillis() - startTime));
	}
}
