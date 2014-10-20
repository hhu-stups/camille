package org.eventb.texteditor.ui;

import java.io.IOException;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.ui.EclipseUIPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;
import org.eventb.texteditor.ui.editor.codecompletion.DefaultContentAssist.ContextType;

/**
 * The actual implementation of the Eclipse <b>Plugin</b> (
 * {@link EclipseUIPlugin}).
 */
public class TextEditorPluginImplementation extends EclipseUIPlugin {
	private ResourceBundle resourceBundle;
	private ContributionTemplateStore templateStore;
	private ContributionContextTypeRegistry contextTypeRegistry;

	public TextEditorPluginImplementation() {
		super();
		TextEditorPlugin.plugin = this;

		try {
			resourceBundle = ResourceBundle.getBundle("plugin");
		} catch (final MissingResourceException e) {
			log(new Status(IStatus.ERROR, TextEditorPlugin.PLUGIN_ID,
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