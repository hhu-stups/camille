/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texttools;

import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.BundleContext;
import org.rodinp.core.IAttributeType;
import org.rodinp.core.RodinCore;

/**
 * The activator class controls the plug-in life cycle
 */
public class TextToolsPlugin extends Plugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "org.eventb.texttools";

	public static final IAttributeType.String TYPE_TEXTREPRESENTATION = RodinCore
			.getStringAttrType(PLUGIN_ID + ".text_representation");
	public static final IAttributeType.Long TYPE_LASTMODIFIED = RodinCore
			.getLongAttrType(PLUGIN_ID + ".text_lastmodified");

	// The shared instance
	private static TextToolsPlugin plugin;

	private ResourceManager resourceManager;

	/**
	 * The constructor
	 */
	public TextToolsPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.runtime.Plugins#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(final BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
		resourceManager = new ResourceManager();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(final BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 * 
	 * @return the shared instance
	 */
	public static TextToolsPlugin getDefault() {
		return plugin;
	}

	public ResourceManager getResourceManager() {
		return resourceManager;
	}
}
