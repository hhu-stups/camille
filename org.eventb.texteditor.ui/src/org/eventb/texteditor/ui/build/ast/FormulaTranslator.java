/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.build.ast;

import org.eclipse.core.runtime.Assert;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eventb.emf.core.EventBCommentedExpressionElement;
import org.eventb.emf.core.EventBElement;
import org.eventb.emf.core.EventBNamedCommentedComponentElement;
import org.eventb.emf.core.EventBNamedCommentedPredicateElement;
import org.eventb.emf.core.context.Context;
import org.eventb.emf.core.machine.Action;
import org.eventb.emf.core.machine.Machine;
import org.eventb.texteditor.ui.editor.EventBTextEditor;
import org.eventb.texttools.TextPositionUtil;
import org.eventb.texttools.model.texttools.TextRange;
import org.rodinp.keyboard.ui.RodinKeyboardUIPlugin;

public class FormulaTranslator {
	private IDocument document;
	private EventBTextEditor editor;

	public void translateFormulas(
			final EventBNamedCommentedComponentElement astRoot,
			final IDocument document, final EventBTextEditor editor) {
		this.document = document;
		this.editor = editor;

		// traverse tree using an iterator
		final TreeIterator<EObject> iterator = EcoreUtil.getAllContents(
				astRoot, false);

		if (astRoot instanceof Machine) {
			final MachineTranslateSwitch switcher = new MachineTranslateSwitch(
					this);
			while (iterator.hasNext()) {
				// visit node
				switcher.doSwitch(iterator.next());
			}
		} else if (astRoot instanceof Context) {
			final ContextTranslateSwitch switcher = new ContextTranslateSwitch(
					this);
			while (iterator.hasNext()) {
				// visit node
				switcher.doSwitch(iterator.next());
			}
		}
	}

	public void replace(final EventBNamedCommentedPredicateElement predicate) {
		final String input = predicate.getPredicate();

		if (input != null) {
			final String translatedInput = RodinKeyboardUIPlugin.getDefault().translate(input);

			if (!input.equals(translatedInput)) {
				// replace in emf node
				predicate.setPredicate(translatedInput);
				final TextRange range = updatePosition(predicate, input,
						translatedInput);

				updateDocument(input, translatedInput, range);
			}
		}
	}

	public void replace(final EventBCommentedExpressionElement expression) {
		final String input = expression.getExpression();

		if (input != null) {
			final String translatedInput = RodinKeyboardUIPlugin.getDefault().translate(input);

			if (!input.equals(translatedInput)) {
				// replace in emf node
				expression.setExpression(translatedInput);
				final TextRange range = updatePosition(expression, input,
						translatedInput);

				updateDocument(input, translatedInput, range);
			}
		}
	}

	public void replace(final Action action) {
		final String input = action.getAction();

		if (input != null) {
			final String translatedInput = RodinKeyboardUIPlugin.getDefault().translate(input);

			if (!input.equals(translatedInput)) {
				// replace in emf node
				action.setAction(translatedInput);
				final TextRange range = updatePosition(action, input,
						translatedInput);

				updateDocument(input, translatedInput, range);
			}
		}
	}

	private void updateDocument(final String input,
			final String translatedInput, final TextRange range) {
		/*
		 * Check length and fill output string with whitespaces to length of
		 * input. So we avoid position changes for the following text elements.
		 * Output should always be <= input.
		 */
		final int lengthDiff = input.length() - translatedInput.length();
		Assert
				.isTrue(lengthDiff >= 0,
						"Expecting length of translated formula to be less or equal to original length");
		final String filledString = fillOutput(translatedInput, lengthDiff);

		Display.getDefault().asyncExec(new Runnable() {
			public void run() {
				try {
					int cursorOffset = -1;
					if (editor != null) {
						cursorOffset = editor.getCursorOffset();
					}

					/*
					 * Replace text in document. Important: Don't use length
					 * from TextRange because it's only set to the short new
					 * version without the trailing whitespaces.
					 */
					final int startOffset = range.getOffset();
					document.replace(startOffset, filledString.length(),
							filledString);

					// correct cursor position afterwards
					if (editor != null && cursorOffset >= 0) {
						correctCursorPosition(cursorOffset, range, lengthDiff);
					}
				} catch (final BadLocationException e) {
					// IGNORE, cannot fix it anyway
				}
			}
		});
	}

	private void correctCursorPosition(final int oldOffset,
			final TextRange range, final int lengthCorrection) {
		// was cursor in changed region?
		if (oldOffset >= range.getOffset()
				&& oldOffset <= range.getOffset() + range.getLength()
						+ lengthCorrection) {
			// then move cursor to front a bit
			editor.setCurserOffset(oldOffset - lengthCorrection);
		}
	}

	private String fillOutput(final String output, final int count) {
		if (count > 0) {
			final StringBuilder buffer = new StringBuilder(output);
			for (int i = 0; i < count; i++) {
				buffer.append(' ');
			}

			return buffer.toString();
		} else {
			return output;
		}
	}

	private TextRange updatePosition(final EventBElement parent,
			final String oldContent, final String newContent) {
		final TextRange range = TextPositionUtil.getInternalPosition(parent,
				oldContent);

		range.setLength(newContent.length());
		TextPositionUtil.replaceInternalPosition(parent, oldContent,
				newContent, range);

		return range;
	}
}
