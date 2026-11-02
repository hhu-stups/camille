/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.actions;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.eventb.texteditor.ui.TextEditorPlugin;

/**
 * Switches the coloring of labels by their proof status on and off (toggle
 * button in the toolbar of the Camille editor). The state is the preference
 * of the preference page, so both stay in sync.
 */
public class ToggleProofStatusHandler extends AbstractHandler implements
		IElementUpdater {

	private final IPropertyChangeListener listener = new IPropertyChangeListener() {
		@Override
		public void propertyChange(final PropertyChangeEvent event) {
			if (TextEditorPlugin.PREF_PROOF_STATUS.equals(event.getProperty())) {
				refresh();
			}
		}
	};

	public ToggleProofStatusHandler() {
		getStore().addPropertyChangeListener(listener);
	}

	@Override
	public void dispose() {
		getStore().removePropertyChangeListener(listener);
		super.dispose();
	}

	private static IPreferenceStore getStore() {
		return TextEditorPlugin.getPlugin().getPreferenceStore();
	}

	@Override
	public Object execute(final ExecutionEvent event) {
		final IPreferenceStore store = getStore();
		store.setValue(TextEditorPlugin.PREF_PROOF_STATUS,
				!store.getBoolean(TextEditorPlugin.PREF_PROOF_STATUS));
		return null;
	}

	@Override
	public void updateElement(final UIElement element,
			@SuppressWarnings("rawtypes") final Map parameters) {
		element.setChecked(getStore().getBoolean(
				TextEditorPlugin.PREF_PROOF_STATUS));
	}

	private static void refresh() {
		final ICommandService service = PlatformUI.getWorkbench().getService(
				ICommandService.class);
		if (service != null) {
			service.refreshElements(
					TextEditorPlugin.TOGGLE_PROOF_STATUS_COMMAND_ID, null);
		}
	}
}
