/*
 *   Copyright 2018. AppDynamics LLC and its affiliates.
 *   All Rights Reserved.
 *   This is unpublished proprietary source code of AppDynamics LLC and its affiliates.
 *   The copyright notice above does not evidence any actual or intended publication of such source code.
 *
 */
package com.appdynamics.connectors.terremark;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.vcloud.domain.VApp;
import org.jclouds.vcloud.domain.VAppStatus;
import org.jclouds.vcloud.options.InstantiateVAppTemplateOptions;
import org.jclouds.vcloud.terremark.TerremarkVCloudClient;

import com.singularity.ee.connectors.api.ConnectorException;
import com.singularity.ee.connectors.api.IConnector;
import com.singularity.ee.connectors.api.IControllerServices;
import com.singularity.ee.connectors.api.InvalidObjectException;
import com.singularity.ee.connectors.entity.api.IComputeCenter;
import com.singularity.ee.connectors.entity.api.IImage;
import com.singularity.ee.connectors.entity.api.IImageStore;
import com.singularity.ee.connectors.entity.api.IMachine;
import com.singularity.ee.connectors.entity.api.IMachineDescriptor;
import com.singularity.ee.connectors.entity.api.IProperty;
import com.singularity.ee.connectors.entity.api.MachineState;

public class TerremarkVCloudConnector implements IConnector
{

	private static final String SERVER_INSTANTIATION_TYPE_PROP = "Server Instantiation Type";
	private static final String IMAGE_TEMPLAE_ID = "Template Id";
	private static final String PROCESSOR_COUNT_PROP = "Processor Count";
	private static final String MEMORY_PROP = "Memory";

	private IControllerServices controllerServices;

	private final Logger logger = Logger.getLogger(TerremarkVCloudConnector.class.getName());

	@Override
	public IMachine createMachine(IComputeCenter computeCenter, IImage image, IMachineDescriptor machineDescriptor)
			throws InvalidObjectException, ConnectorException
	{
		boolean succeeded = false;
		Exception createFailureRootCause = null;
		VApp vApp = null;

		IProperty[] macProps = machineDescriptor.getProperties();
		
		boolean fromTemplate = isCreateMachineFromTemplate(macProps);
		
		TerremarkVCloudProvider connector = ConnectorLocator.getInstance()
				.getConnector(computeCenter, controllerServices);

		try
		{
			if(!fromTemplate)
			{
				// we just need to start the vApp server which will happen 
				// on refresh machine state
				vApp = getVApp(macProps, connector);
			}
			else
			{
				// create a new VApp
				vApp = createVApp(image, macProps, connector);
				
				logger.info("Terremark vCloud machine created; id:" + vApp.getId());
			}
			
			IMachine machine = controllerServices.createMachineInstance(
					vApp.getId(), vApp.getId(), computeCenter, machineDescriptor,
					image, getAgentPort());

			succeeded = true;

			return machine;

		}
		catch (InvalidObjectException e)
		{
			createFailureRootCause = e;
			throw e;
		}
		catch (Exception e)
		{
			createFailureRootCause = e;
			throw new ConnectorException(e.getMessage(), e);
		}
		finally
		{
			// We have to make sure to terminate any orphan instances if
			// the machine create fails.
			if (!succeeded && vApp != null && fromTemplate)
			{
				try
				{
					connector.getAsyncApi().deleteVApp(vApp.getId());
				}
				catch (Exception e)
				{
					throw new ConnectorException("Machine create failed, but terminate failed as well! "
							+ "We have an orphan Terremark vCloud instance with id: " + vApp.getId()
							+ " that must be shut down manually. Root cause for machine "
							+ "create failure is following: ", createFailureRootCause);
				}
			}
		}
	}

	private String getPublicIPAddress(VApp vApp, TerremarkVCloudProvider connector)
	{
		// get the public ip and if not present then throw exception
		NodeMetadata nodeMetadata = connector.getComputeService().getNodesWithTag(
				vApp.getName()).get(vApp.getId());
		
		if(nodeMetadata == null)
		{
			throw new IllegalArgumentException("Invalid vApp id " 
					+ vApp.getId() + " is specified");
		}
		
		Set<InetAddress> publicAddresses = nodeMetadata.getPublicAddresses();
		
		if(!publicAddresses.isEmpty())
		{
			// set any one of the public ip address
			return publicAddresses.iterator().next().getHostAddress();
		}
		
		return null;
	}

	private VApp createVApp(IImage image, IProperty[] macProps,
			TerremarkVCloudProvider connector) throws ConnectorException
	{
		String templateId = controllerServices.getStringPropertyValueByName(
				image.getProperties(), IMAGE_TEMPLAE_ID);

		int processorCount = getProcessorCount(macProps);
		int memory = getMemory(macProps);

		logger.info("Starting Terremark vCloud machine of Template :" 
				+ templateId + " processor count :"
				+ processorCount + " memory (MB) :" + memory);

		InstantiateVAppTemplateOptions options = InstantiateVAppTemplateOptions
				.Builder.processorCount(processorCount).memory(memory);
		
		TerremarkVCloudClient syncApi = connector.getSyncApi();
		
		return syncApi.instantiateVAppTemplateInVDC(syncApi.getDefaultVDC().getId(),
				"AD" + System.currentTimeMillis(), templateId, options);
	}

	private VApp getVApp(IProperty[] macProps, TerremarkVCloudProvider connector)
	{
		String vAppId = getVAppId(macProps);
		
		if(StringUtils.isBlank(vAppId))
		{
			throw new IllegalArgumentException("VApp id is not specified");
		}
		
		VApp vApp = connector.getSyncApi().getVApp(vAppId);
		
		if(vApp == null)
		{
			throw new IllegalArgumentException("Invalid VApp id  "
					+ vAppId + " is specified");
		}
		
		return vApp;
	}

	private String getVAppId(IProperty... macProps)
	{
		return controllerServices.getStringPropertyValueByName(macProps, "VApp Id");
	}
	
	private boolean isCreateMachineFromTemplate(IProperty... macProps)
	{
		String value = controllerServices.getStringPropertyValueByName(macProps,
				SERVER_INSTANTIATION_TYPE_PROP);
		
		if(value.equals("Template"))
			return true;
		else
			return false;
	}
	
	private boolean isCreateMachineFromTemplate(IMachine machine)
	{
		return isCreateMachineFromTemplate(machine.getMachineDescriptor().getProperties());
	}

	private int getProcessorCount(IProperty... properties) throws ConnectorException
	{
		String processorCountStr = controllerServices.getStringPropertyValueByName(
				properties, PROCESSOR_COUNT_PROP);
		try
		{
			return Integer.parseInt(processorCountStr);
		}
		catch (Exception e)
		{
			throw new ConnectorException("Invalid processor count " 
					+ processorCountStr + " specified");
		}
	}
	
	private int getMemory(IProperty... properties) throws ConnectorException
	{
		String memory = controllerServices.getStringPropertyValueByName(
				properties, MEMORY_PROP);
		try
		{
			return Integer.parseInt(memory);
		}
		catch (Exception e)
		{
			throw new ConnectorException("Invalid memory " + memory + "specified");
		}
	}
	
	@Override
	public void refreshMachineState(IMachine machine) throws InvalidObjectException, ConnectorException
	{
		MachineState currentState = machine.getState();
		
		boolean fromTemplate = isCreateMachineFromTemplate(machine);

		if(currentState == MachineState.STARTING)
		{
			// during starting and restarting the machine
			TerremarkVCloudProvider connector = ConnectorLocator.getInstance()
				.getConnector(machine.getComputeCenter(), controllerServices);
		
			VApp vApp = connector.getSyncApi().getVApp(machine.getName());
			
			if(vApp == null)
			{
				machine.setState(MachineState.STOPPED);
				return;
			}
			
			if(vApp.getStatus() == VAppStatus.OFF)
			{
				// machine is created. Power on the mahine
				connector.getAsyncApi().powerOnVApp(vApp.getId());
			}
			else if(vApp.getStatus() == VAppStatus.ON)
			{
				// first get the public ip address
				// if its not present then get the private ip address
				String ipAddress = getPublicIPAddress(vApp, connector);

				if (ipAddress == null)
				{
					Iterator<InetAddress> iterator = vApp.getNetworkToAddresses().values().iterator();

					if (iterator.hasNext())
					{
						ipAddress = iterator.next().getHostAddress();
					}
				}

				String currentIpAddress = machine.getIpAddress();
				if (!currentIpAddress.equals(ipAddress))
				{
					machine.setIpAddress(ipAddress);
				}

				machine.setState(MachineState.STARTED);
			}
		}
		else if(currentState == MachineState.STOPPING)
		{
			// this is called by terminate mahine
			TerremarkVCloudProvider connector = ConnectorLocator.getInstance()
					.getConnector(machine.getComputeCenter(), controllerServices);
	
			VApp vApp = connector.getSyncApi().getVApp(machine.getName());

			if(vApp == null)
			{
				machine.setState(MachineState.STOPPED);
				
				return;
			}

			if(vApp.getStatus() == VAppStatus.OFF)
			{
				if(fromTemplate)
				{
					// machine is created. Power on the mahine
					connector.getAsyncApi().deleteVApp(vApp.getId());
				}
				else
				{
					// the machine which is started from the VApp we just mark it
					// as stopped
					machine.setState(MachineState.STOPPED);
				}
			}
			else if(vApp.getStatus() == VAppStatus.UNRESOLVED)
			{
				machine.setState(MachineState.STOPPED);
			}
		}
	}
	
	@Override
	public void restartMachine(IMachine machine) throws InvalidObjectException, ConnectorException
	{
		TerremarkVCloudProvider connector = ConnectorLocator.getInstance()
				.getConnector(machine.getComputeCenter(), controllerServices);

		VApp vApp = connector.getSyncApi().getVApp(machine.getName());
		
		if(vApp == null)
		{
			// machine is terminated due to some reason.
			// do nothing as the state will be changed to terminated
		}

		connector.getAsyncApi().resetVApp(vApp.getId());
	}

	@Override
	public void terminateMachine(IMachine machine) throws InvalidObjectException, ConnectorException
	{
		try
		{
			TerremarkVCloudProvider connector = ConnectorLocator.getInstance()
					.getConnector(machine.getComputeCenter(), controllerServices);

			if(!isCreateMachineFromTemplate(machine))
			{
				machine.setName(UUID.randomUUID().toString());
			}
			
			VApp vApp = connector.getSyncApi().getVApp(machine.getName());
			
			if(vApp == null)
			{
				machine.setState(MachineState.STOPPED);
				return;
			}
			
			if(vApp.getStatus() == VAppStatus.ON)
			{
				connector.getAsyncApi().powerOffVApp(vApp.getId());
			}
		}
		catch (Exception e)
		{
			throw new ConnectorException("Machine terminate failed: "+machine.getName(), e);
		}
	}

	@Override
	public void validate(IComputeCenter computeCenter) throws InvalidObjectException, ConnectorException
	{
		try
		{
			ConnectorLocator.getInstance().getConnector(computeCenter.getProperties(),
					controllerServices, true);
		}
		catch (Exception e)
		{
			logger.log(Level.WARNING, "", e);
			
			throw new InvalidObjectException("Failed to validate the connector properties", e);
		}
	}

	@Override
	public void setControllerServices(IControllerServices controllerServices)
	{
		this.controllerServices = controllerServices;
	}

	@Override
	public void validate(IImageStore imageStore) throws InvalidObjectException, ConnectorException
	{
		try
		{
			ConnectorLocator.getInstance().getConnector(imageStore.getProperties(),
					controllerServices, true);
		}
		catch (Exception e)
		{
			logger.log(Level.WARNING, "", e);
			
			throw new InvalidObjectException("Failed to validate the image store properties", e);
		}
	}

	@Override
	public void validate(IImage image) throws InvalidObjectException, ConnectorException
	{
		// do nothing
	}

	@Override
	public void unconfigure(IComputeCenter computeCenter) throws InvalidObjectException, ConnectorException
	{
		// do nothing
	}

	@Override
	public void unconfigure(IImageStore imageStore) throws InvalidObjectException, ConnectorException
	{
		// do nothing
	}

	@Override
	public void unconfigure(IImage image) throws InvalidObjectException, ConnectorException
	{
		// do nothing
	}

	@Override
	public void configure(IComputeCenter computeCenter) throws InvalidObjectException, ConnectorException
	{
		// do nothing
	}

	@Override
	public void configure(IImageStore imageStore) throws InvalidObjectException, ConnectorException
	{
		// do nothing
	}

	@Override
	public void configure(IImage image) throws InvalidObjectException, ConnectorException
	{
		// do nothing
	}

	@Override
	public void deleteImage(IImage image) throws InvalidObjectException, ConnectorException
	{
		// do nothing
	}

	@Override
	public int getAgentPort()
	{
		return controllerServices.getDefaultAgentPort();
	}

	@Override
	public void refreshImageState(IImage image) throws InvalidObjectException, ConnectorException
	{
		// do nothing
	}

}
