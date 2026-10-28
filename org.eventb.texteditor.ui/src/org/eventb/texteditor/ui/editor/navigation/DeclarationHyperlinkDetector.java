/**
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen,
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html)
 * */

package org.eventb.texteditor.ui.editor.navigation;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.ui.IFileEditorInput;
import org.eventb.texteditor.ui.editor.EventBTextEditor;
import org.eventb.texteditor.ui.editor.codecompletion.StructureAnalyzer;
import org.eventb.texteditor.ui.editor.hover.EventBTextHover;
import org.eventb.texteditor.ui.editor.hover.HoverIndex;
import org.eventb.texteditor.ui.editor.hover.HoverIndex.Declaration;
import org.eventb.texttools.Constants;

/**
 * Jump to declaration: Ctrl/Cmd+click on a variable, constant,
 * carrier set, parameter, event or referenced machine/context opens its
 * declaration. Also used by the "Open Declaration" command (F3).
 */
public class DeclarationHyperlinkDetector implements IHyperlinkDetector {

	private static final Declaration.Kind[] LOCAL_KINDS = {
			Declaration.Kind.PARAMETER, Declaration.Kind.VARIABLE,
			Declaration.Kind.CONSTANT, Declaration.Kind.CARRIER_SET,
			Declaration.Kind.EVENT };

	private final EventBTextEditor editor;
	private final EventBTextHover hover;

	/**
	 * @param hover
	 *            provides the information read from the Rodin database
	 */
	public DeclarationHyperlinkDetector(final EventBTextEditor editor,
			final EventBTextHover hover) {
		this.editor = editor;
		this.hover = hover;
	}

	@Override
	public IHyperlink[] detectHyperlinks(final ITextViewer textViewer,
			final IRegion region, final boolean canShowMultipleHyperlinks) {
		final IDocument document = textViewer.getDocument();
		if (document == null || region == null) {
			return null;
		}

		final IRegion wordRegion = findWord(document, region.getOffset());
		if (wordRegion == null) {
			return null;
		}

		final Declaration declaration = findDeclaration(document, wordRegion);
		return declaration != null ? new IHyperlink[] { new DeclarationHyperlink(
				editor, wordRegion, declaration) } : null;
	}

	private Declaration findDeclaration(final IDocument document,
			final IRegion wordRegion) {
		final String text = document.get();
		final String word = text.substring(wordRegion.getOffset(),
				wordRegion.getOffset() + wordRegion.getLength());
		final String event = EventBTextHover.findEvent(text,
				wordRegion.getOffset());
		final HoverIndex index = hover.getIndex();

		Declaration declaration = null;

		// "refines"/"extends" of an event: the abstract event
		final String reference = StructureAnalyzer.findReferenceKeyword(text,
				wordRegion.getOffset());
		if (event != null
				&& (Constants.REFINES.equals(reference) || Constants.EXTENDS
						.equals(reference))) {
			declaration = index.findAbstractEvent(word);
		}

		if (declaration == null) {
			declaration = index.findDeclaration(word, event);
		}

		final IFile currentFile = getCurrentFile();
		if (declaration == null) {
			// not saved yet: look for a declaration in the text
			for (final Declaration.Kind kind : LOCAL_KINDS) {
				if (DeclarationLocator.find(text, kind, word, event) != null) {
					declaration = new Declaration(kind, word, currentFile,
							event);
					break;
				}
			}
		}

		if (declaration == null) {
			return null;
		}

		// no link on the declaration itself
		if (declaration.file == null || declaration.file.equals(currentFile)) {
			final IRegion target = DeclarationLocator.find(text,
					declaration.kind, declaration.name, declaration.event);
			if (target == null
					|| target.getOffset() == wordRegion.getOffset()) {
				return null;
			}
		}
		return declaration;
	}

	private IFile getCurrentFile() {
		return editor.getEditorInput() instanceof IFileEditorInput ? ((IFileEditorInput) editor
				.getEditorInput()).getFile() : null;
	}

	private static IRegion findWord(final IDocument document, final int offset) {
		try {
			int start = offset;
			while (start > 0 && isIdentifierPart(document.getChar(start - 1))) {
				start--;
			}
			int end = offset;
			while (end < document.getLength()
					&& isIdentifierPart(document.getChar(end))) {
				end++;
			}
			if (end == start || document.getChar(start) == '@'
					|| (start > 0 && document.getChar(start - 1) == '@')) {
				// nothing or a label
				return null;
			}
			return new Region(start, end - start);
		} catch (final BadLocationException e) {
			return null;
		}
	}

	private static boolean isIdentifierPart(final char c) {
		return Character.isJavaIdentifierPart(c) || c == '\'';
	}
}
