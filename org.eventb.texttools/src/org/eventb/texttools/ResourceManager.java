/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texttools;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.emf.common.command.BasicCommandStack;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.provider.ReflectiveItemProviderAdapterFactory;
import org.eclipse.emf.edit.provider.resource.ResourceItemProviderAdapterFactory;
import org.eventb.emf.core.context.provider.ContextItemProviderAdapterFactory;
import org.eventb.emf.core.machine.provider.MachineItemProviderAdapterFactory;
import org.eventb.emf.core.provider.CoreItemProviderAdapterFactory;

public class ResourceManager {
	private final Map<IProject, AdapterFactoryEditingDomain> projectEditingDomains = new HashMap<IProject, AdapterFactoryEditingDomain>();

	public AdapterFactoryEditingDomain getEditingDomain(final IProject project) {
		if (!projectEditingDomains.containsKey(project)) {
			final AdapterFactoryEditingDomain editingDomain = initializeEditingDomain();
			projectEditingDomains.put(project, editingDomain);
		}

		return projectEditingDomains.get(project);
	}

	private AdapterFactoryEditingDomain initializeEditingDomain() {
		// Create an adapter factory that yields item providers.
		final ComposedAdapterFactory adapterFactory = new ComposedAdapterFactory(
				ComposedAdapterFactory.Descriptor.Registry.INSTANCE);

		adapterFactory
				.addAdapterFactory(new ResourceItemProviderAdapterFactory());
		adapterFactory.addAdapterFactory(new CoreItemProviderAdapterFactory());
		adapterFactory
				.addAdapterFactory(new MachineItemProviderAdapterFactory());
		adapterFactory
				.addAdapterFactory(new ContextItemProviderAdapterFactory());
		adapterFactory
				.addAdapterFactory(new ReflectiveItemProviderAdapterFactory());

		final BasicCommandStack commandStack = new BasicCommandStack();

		return new AdapterFactoryEditingDomain(adapterFactory, commandStack,
				new HashMap<Resource, Boolean>());
	}
}
