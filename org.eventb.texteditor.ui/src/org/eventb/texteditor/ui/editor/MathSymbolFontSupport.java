/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextPresentationListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension4;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.rodinp.keyboard.ui.RodinKeyboardUIPlugin;

/**
 * Some Event-B operators only exist in the private use area of Unicode, so
 * they can only be displayed with the Brave Sans Mono font that comes with
 * Rodin:
 * <ul>
 * <li>U+E100 total relation (<code>&lt;&lt;-&gt;</code>)</li>
 * <li>U+E101 surjective relation (<code>&lt;-&gt;&gt;</code>)</li>
 * <li>U+E102 total surjective relation (<code>&lt;&lt;-&gt;&gt;</code>)</li>
 * <li>U+E103 relational override (<code>&lt;+</code>)</li>
 * </ul>
 * With any other font they are shown as boxes. This class displays just these
 * characters in Brave Sans Mono (with the size of the editor font), the rest
 * of the text keeps the font chosen by the user.
 */
public class MathSymbolFontSupport implements ITextPresentationListener {

	private static final char FIRST = '';
	private static final char LAST = '';
	private static final String FONT_NAME = "Brave Sans Mono";

	private final ITextViewer viewer;
	/*
	 * One font per size. Fonts are disposed only with the editor, because
	 * style ranges of the text widget may still refer to them.
	 */
	private final Map<Integer, Font> fonts = new HashMap<Integer, Font>();

	/** applies a new size of the editor font to the symbols, too */
	private final IPropertyChangeListener fontListener = new IPropertyChangeListener() {
		@Override
		public void propertyChange(final PropertyChangeEvent event) {
			final StyledText widget = viewer.getTextWidget();
			if (widget != null && !widget.isDisposed()) {
				widget.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						if (!widget.isDisposed()) {
							viewer.invalidateTextPresentation();
						}
					}
				});
			}
		}
	};

	public MathSymbolFontSupport(final ITextViewer viewer) {
		this.viewer = viewer;
	}

	public void install() {
		ensureFontIsLoaded();
		if (viewer instanceof ITextViewerExtension4) {
			((ITextViewerExtension4) viewer).addTextPresentationListener(this);
		}
		JFaceResources.getFontRegistry().addListener(fontListener);
	}

	public void uninstall() {
		if (viewer instanceof ITextViewerExtension4) {
			((ITextViewerExtension4) viewer)
					.removeTextPresentationListener(this);
		}
		JFaceResources.getFontRegistry().removeListener(fontListener);
		for (final Font font : fonts.values()) {
			font.dispose();
		}
		fonts.clear();
	}

	/**
	 * Rodin ships the font but registers it only on demand.
	 */
	private static void ensureFontIsLoaded() {
		try {
			RodinKeyboardUIPlugin.getDefault().ensureMathFontIsAvailable();
		} catch (final RuntimeException e) {
			TextEditorPlugin.getPlugin().getLog()
					.warn("Could not load the Rodin math font", e);
		} catch (final LinkageError e) {
			// older Rodin versions load the font themselves
		}
	}

	@Override
	public void applyTextPresentation(final TextPresentation presentation) {
		final IDocument document = viewer.getDocument();
		final IRegion extent = presentation.getExtent();
		if (document == null || extent == null) {
			return;
		}

		final String text;
		try {
			final int length = Math.min(extent.getLength(),
					document.getLength() - extent.getOffset());
			if (length <= 0) {
				return;
			}
			text = document.get(extent.getOffset(), length);
		} catch (final BadLocationException e) {
			return;
		}

		Font font = null;
		for (int i = 0; i < text.length(); i++) {
			final char c = text.charAt(i);
			if (c < FIRST || c > LAST) {
				continue;
			}
			if (font == null) {
				font = getSymbolFont();
				if (font == null) {
					return;
				}
			}
			final StyleRange range = new StyleRange();
			range.start = extent.getOffset() + i;
			range.length = 1;
			range.font = font;
			// keeps the colors of the syntax highlighting
			presentation.mergeStyleRange(range);
		}
	}

	/**
	 * @return Brave Sans Mono with the size of the editor font
	 */
	private Font getSymbolFont() {
		final StyledText widget = viewer.getTextWidget();
		if (widget == null || widget.isDisposed()) {
			return null;
		}

		final FontData[] editorFont = widget.getFont().getFontData();
		final int height = editorFont.length > 0 ? editorFont[0].getHeight()
				: 10;
		Font font = fonts.get(height);
		if (font == null) {
			font = new Font(widget.getDisplay(), FONT_NAME, height, 0);
			fonts.put(height, font);
		}
		return font;
	}
}
