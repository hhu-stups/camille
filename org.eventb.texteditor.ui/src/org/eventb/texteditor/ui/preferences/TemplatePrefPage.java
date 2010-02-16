/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.preferences;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.texteditor.templates.TemplatePreferencePage;
import org.eventb.texteditor.ui.TextEditorPlugin;

public class TemplatePrefPage extends TemplatePreferencePage {

	@Override
	public void init(final IWorkbench workbench) {
		super.init(workbench);

		setTemplateStore(TextEditorPlugin.getPlugin().getTemplateStore());
		setContextTypeRegistry(TextEditorPlugin.getPlugin()
				.getContextTypeRegistry());
		setPreferenceStore(TextEditorPlugin.getPlugin().getPreferenceStore());
	}

	@Override
	protected boolean isShowFormatterSetting() {
		return false;
	}
}
