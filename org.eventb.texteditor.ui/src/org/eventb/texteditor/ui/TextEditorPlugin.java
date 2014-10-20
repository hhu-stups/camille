/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package org.eventb.texteditor.ui;

import java.io.IOException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.EMFPlugin;
import org.eclipse.emf.common.ui.EclipseUIPlugin;
import org.eclipse.emf.common.util.ResourceLocator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;
import org.eventb.texteditor.ui.build.dom.DomManager;
import org.eventb.texteditor.ui.editor.codecompletion.DefaultContentAssist.ContextType;

public final class TextEditorPlugin extends EMFPlugin {
	/**
	 * Plug-in ID
	 */
	public static final String PLUGIN_ID = "org.eventb.texteditor.ui";

	/**
	 * ID to identify the texteditor
	 */
	public static final String TEXTEDITOR_ID = PLUGIN_ID + ".texteditor";

	/**
	 * ID for text editor context menu
	 */
	public static final String TEXTEDITOR_CONTEXTMENU_ID = "#texteditor.context.menu";

	/**
	 * ID for problem markers of this plugin
	 */
	public static final String SYNTAXERROR_MARKER_ID = PLUGIN_ID
			+ ".syntaxerror";

	/**
	 * ID the 'format' command
	 */
	public static final String FORMAT_COMMAND_ID = "org.eventb.texteditor.command.format";

	/**
	 * Singleton instance of EMFPlugin
	 */
	public static final TextEditorPlugin INSTANCE = new TextEditorPlugin();

	/**
	 * Singleton instance of EclipseUIPlugin
	 */
	private static Implementation plugin;

	private static final DomManager domManager = new DomManager();

	public TextEditorPlugin() {
		super(new ResourceLocator[] {});
	}

	@Override
	public ResourceLocator getPluginResourceLocator() {
		return plugin;
	}

	/**
	 * Returns the singleton instance of the {@link EclipseUIPlugin} plugin.
	 * 
	 * @return the singleton instance.
	 */
	public static Implementation getPlugin() {
		return plugin;
	}

	public static DomManager getDomManager() {
		return domManager;
	}

	/**
	 * This looks up a string in plugin.properties, making a substitution.
	 */
	public static String getString(final String key, final Object s1) {
		return INSTANCE.getString(key, new Object[] { s1 });
	}

	/**
	 * The actual implementation of the Eclipse <b>Plugin</b> (
	 * {@link EclipseUIPlugin}).
	 */
	public static class Implementation extends EclipseUIPlugin {
		private ResourceBundle resourceBundle;
		private ContributionTemplateStore templateStore;
		private ContributionContextTypeRegistry contextTypeRegistry;

		public Implementation() {
			super();
			plugin = this;

			try {
				resourceBundle = ResourceBundle.getBundle("plugin");
			} catch (final MissingResourceException e) {
				log(new Status(IStatus.ERROR, PLUGIN_ID,
						"Cannot load resource bundle", e));
				resourceBundle = null;
			}
		}

		public ResourceBundle getResourceBundle() {
			return resourceBundle;
		}

		public IWorkbenchPage getActivePage() {
			final IWorkbenchWindow window = getWorkbench()
					.getActiveWorkbenchWindow();
			if (window == null) {
				return null;
			}
			return window.getActivePage();
		}

		public ContributionContextTypeRegistry getContextTypeRegistry() {
			if (contextTypeRegistry == null) {
				contextTypeRegistry = new ContributionContextTypeRegistry();
				contextTypeRegistry.addContextType(ContextType.Anywhere.key);
				contextTypeRegistry.addContextType(ContextType.Machine.key);
				contextTypeRegistry.addContextType(ContextType.Events.key);
				contextTypeRegistry.addContextType(ContextType.Context.key);
			}

			return contextTypeRegistry;
		}

		public ContributionTemplateStore getTemplateStore() {
			if (templateStore == null) {
				final IPreferenceStore preferenceStore = TextEditorPlugin
						.getPlugin().getPreferenceStore();
				templateStore = new ContributionTemplateStore(
						getContextTypeRegistry(), preferenceStore, "templates");
				try {
					templateStore.load();
				} catch (final IOException e) {
					TextEditorPlugin
							.getPlugin()
							.getLog()
							.log(new Status(IStatus.ERROR,
									TextEditorPlugin.PLUGIN_ID,
									"Cannot load templates", e));
				}
			}

			return templateStore;
		}
	}
}
