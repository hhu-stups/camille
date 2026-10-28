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


import org.eclipse.emf.common.EMFPlugin;
import org.eclipse.emf.common.ui.EclipseUIPlugin;
import org.eclipse.emf.common.util.ResourceLocator;
import org.eventb.texteditor.ui.build.dom.DomManager;

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
	 * Preference key: automatically replace ASCII notation in formulas by
	 * mathematical symbols while typing
	 */
	public static final String PREF_AUTO_TRANSLATE = PLUGIN_ID
			+ ".autoTranslate";

	/** Preference keys: highlight the bracket matching the one at the cursor */
	public static final String PREF_MATCHING_BRACKETS = PLUGIN_ID
			+ ".matchingBrackets";
	public static final String PREF_MATCHING_BRACKETS_COLOR = PLUGIN_ID
			+ ".matchingBracketsColor";
	public static final String PREF_ENCLOSING_BRACKETS = PLUGIN_ID
			+ ".enclosingBrackets";
	public static final String PREF_BRACKET_AT_CARET = PLUGIN_ID
			+ ".highlightBracketAtCaret";

	/** ID of the "Go to Matching Bracket" command */
	public static final String GOTO_MATCHING_BRACKET_COMMAND_ID = "org.eventb.texteditor.command.gotoMatchingBracket";

	/**
	 * Singleton instance of EMFPlugin
	 */
	public static final TextEditorPlugin INSTANCE = new TextEditorPlugin();

	/**
	 * Singleton instance of EclipseUIPlugin
	 */
	static TextEditorPluginImplementation plugin;

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
	public static TextEditorPluginImplementation getPlugin() {
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
}
