/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.ColorSelector;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eventb.texteditor.ui.TextDecoration;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.eventb.texteditor.ui.TextDecoration.ESyntaxElement;

public class HighlightingPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	private final List<Field> fields = new ArrayList<Field>();

	public HighlightingPreferencePage() {
		super();
		setPreferenceStore(TextEditorPlugin.getPlugin().getPreferenceStore());
		setDescription("Please configure the syntax highlighting for the Event-B TextEditor. You can select the color and the style for each element type.");
	}

	@Override
	protected Control createContents(final Composite parent) {
		final Composite colorComposite = new Composite(parent, SWT.NONE);
		final GridLayout layout = new GridLayout();
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		colorComposite.setLayout(layout);

		for (final ESyntaxElement element : TextDecoration.ESyntaxElement
				.values()) {
			createField(element, colorComposite);
		}

		loadPreferences();

		colorComposite.layout(false);
		return colorComposite;
	}

	private void savePreferences() {
		final IPreferenceStore store = getPreferenceStore();

		for (final Field f : fields) {
			final ESyntaxElement element = f.element;

			final int bold = f.boldButton.getSelection() ? SWT.BOLD : SWT.NONE;
			final int italic = f.italicButton.getSelection() ? SWT.ITALIC
					: SWT.NONE;
			final int underline = f.underlineButton.getSelection() ? TextAttribute.UNDERLINE
					: SWT.NONE;
			final int style = bold | italic | underline;

			PreferenceConverter.setValue(store, element.getColorKey(),
					f.colorSelector.getColorValue());
			store.setValue(element.getStyleKey(), style);

			element.resetToken();
		}
	}

	private void loadPreferences() {
		final IPreferenceStore store = getPreferenceStore();

		for (final Field f : fields) {
			final ESyntaxElement element = f.element;
			final RGB color = PreferenceConverter.getColor(store, element
					.getColorKey());
			final int style = store.getInt(element.getStyleKey());

			setFieldValues(f, color, style);
		}
	}

	private void loadDefaultPreferences() {
		final IPreferenceStore store = getPreferenceStore();

		for (final Field f : fields) {
			final ESyntaxElement element = f.element;
			final RGB color = PreferenceConverter.getDefaultColor(store,
					element.getColorKey());
			final int style = store.getDefaultInt(element.getStyleKey());

			setFieldValues(f, color, style);
		}
	}

	private void setFieldValues(final Field f, final RGB color, final int style) {
		f.colorSelector.setColorValue(color);
		f.boldButton.setSelection((style & SWT.BOLD) == SWT.BOLD);
		f.italicButton.setSelection((style & SWT.ITALIC) == SWT.ITALIC);
		f.underlineButton
				.setSelection((style & TextAttribute.UNDERLINE) == TextAttribute.UNDERLINE);
	}

	private void createField(final ESyntaxElement element,
			final Composite parent) {
		final Composite field = new Composite(parent, SWT.NONE);

		final GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.grabExcessHorizontalSpace = true;

		final GridLayout layout = new GridLayout(5, false);
		// layout.marginHeight = 5;
		layout.marginWidth = 5;
		field.setLayout(layout);
		field.setLayoutData(gridData);

		final Label label = new Label(field, SWT.NONE);
		label.setText(element.getLabel());
		label.setLayoutData(gridData);

		final ColorSelector colorSelector = new ColorSelector(field);

		final Button boldButton = new Button(field, SWT.CHECK);
		boldButton.setText("Bold");

		final Button italicButton = new Button(field, SWT.CHECK);
		italicButton.setText("Italic");

		final Button underlineButton = new Button(field, SWT.CHECK);
		underlineButton.setText("Underline");

		fields.add(new Field(element, colorSelector, boldButton, italicButton,
				underlineButton));
	}

	@Override
	public boolean performOk() {
		savePreferences();

		// TODO find active editor and trigger reconcilePresentation on
		// EventBPresentationReconciler

		return true;
	}

	@Override
	protected void performDefaults() {
		loadDefaultPreferences();
		super.performDefaults();
	}

	public void init(final IWorkbench workbench) {
		// TODO Auto-generated method stub
	}

	private class Field {
		protected final ESyntaxElement element;
		protected final ColorSelector colorSelector;
		protected final Button boldButton;
		protected final Button italicButton;
		protected final Button underlineButton;

		protected Field(final ESyntaxElement element,
				final ColorSelector colorSelector, final Button boldButton,
				final Button italicButton, final Button underlineButton) {
			this.element = element;
			this.colorSelector = colorSelector;
			this.boldButton = boldButton;
			this.italicButton = italicButton;
			this.underlineButton = underlineButton;
		}
	}
}