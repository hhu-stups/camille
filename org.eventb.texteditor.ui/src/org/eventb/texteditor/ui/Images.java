/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eventb.ui.EventBUIPlugin;

public class Images {
	public static final String IMG_MACHINE = "Machine";
	public static final String IMG_CONTEXT = "Context";
	public static final String IMG_REFINES = "Refines";
	public static final String IMG_INVARIANT = "Invariant";
	public static final String IMG_THEOREM = "Theorem";
	public static final String IMG_VARIABLE = "Variable";
	public static final String IMG_EVENT = "Event";
	public static final String IMG_GUARD = "Guard";
	public static final String IMG_ACTION = "Action";
	public static final String IMG_AXIOM = "Axiom";
	public static final String IMG_CONSTANT = "Constant";
	public static final String IMG_CARRIER_SET = "Carrier Set";
	public static final String IMG_REFINE_OVERLAY = IMG_REFINES + "Overlay";
	public static final String IMG_COMMENT_OVERLAY = "CommentOverlay";
	public static final String IMG_WARNING_OVERLAY = "WarningOverlay";
	public static final String IMG_ERROR_OVERLAY = "ErrorOverlay";

	private static final String IMG_MACHINE_PATH = "icons/full/obj16/mch_obj.gif";
	private static final String IMG_CONTEXT_PATH = "icons/full/obj16/ctx_obj.gif";
	private static final String IMG_REFINES_PATH = "icons/full/ctool16/refines.gif";
	private static final String IMG_INVARIANT_PATH = "icons/full/obj16/inv_obj.gif";
	private static final String IMG_THEOREM_PATH = "icons/full/obj16/thm_obj.gif";
	private static final String IMG_VARIABLE_PATH = "icons/full/obj16/var_obj.gif";
	private static final String IMG_EVENT_PATH = "icons/full/obj16/evt_obj.gif";
	private static final String IMG_GUARD_PATH = "icons/full/obj16/grd_obj.gif";
	// private static final String IMG_WITNESS_PATH =
	// "icons/full/obj16/evt_obj.gif";
	private static final String IMG_ACTION_PATH = "icons/full/obj16/act_obj.gif";
	private static final String IMG_AXIOM_PATH = "icons/full/obj16/axm_obj.gif";
	private static final String IMG_CONSTANT_PATH = "icons/full/obj16/cst_obj.gif";
	private static final String IMG_CARRIER_SET_PATH = "icons/full/obj16/set_obj.gif";
	private static final String IMG_REFINE_OVERLAY_PATH = "icons/full/ovr16/ref_ovr.gif";
	private static final String IMG_COMMENT_OVERLAY_PATH = "icons/full/ovr16/cmt_ovr.gif";
	private static final String IMG_WARNING_OVERLAY_PATH = "icons/full/ovr16/warning_ovr.gif";
	private static final String IMG_ERROR_OVERLAY_PATH = "icons/full/ovr16/error_ovr.gif";

	private static ImageRegistry imageRegistry = TextEditorPlugin.getPlugin()
			.getImageRegistry();
	private static Map<String, String> rodinIcons = new HashMap<String, String>();

	static {
		rodinIcons.put(IMG_MACHINE, IMG_MACHINE_PATH);
		rodinIcons.put(IMG_CONTEXT, IMG_CONTEXT_PATH);
		rodinIcons.put(IMG_REFINES, IMG_REFINES_PATH);
		rodinIcons.put(IMG_INVARIANT, IMG_INVARIANT_PATH);
		rodinIcons.put(IMG_THEOREM, IMG_THEOREM_PATH);
		rodinIcons.put(IMG_VARIABLE, IMG_VARIABLE_PATH);
		rodinIcons.put(IMG_EVENT, IMG_EVENT_PATH);
		rodinIcons.put(IMG_GUARD, IMG_GUARD_PATH);
		rodinIcons.put(IMG_ACTION, IMG_ACTION_PATH);
		rodinIcons.put(IMG_AXIOM, IMG_AXIOM_PATH);
		rodinIcons.put(IMG_CONSTANT, IMG_CONSTANT_PATH);
		rodinIcons.put(IMG_CARRIER_SET, IMG_CARRIER_SET_PATH);

		rodinIcons.put(IMG_REFINE_OVERLAY, IMG_REFINE_OVERLAY_PATH);
		rodinIcons.put(IMG_COMMENT_OVERLAY, IMG_COMMENT_OVERLAY_PATH);
		rodinIcons.put(IMG_WARNING_OVERLAY, IMG_WARNING_OVERLAY_PATH);
		rodinIcons.put(IMG_ERROR_OVERLAY, IMG_ERROR_OVERLAY_PATH);
	}

	public static final String IMG_TEMPLATE = "Template";
	public static final String IMG_TEMPLATE_PATH = "icons/template_obj.gif";

	public static Image getImage(final String key) {
		final Image image = imageRegistry.get(key);

		if (image == null && rodinIcons.containsKey(key)) {
			return getImage(key, rodinIcons.get(key));
		} else {
			return image;
		}
	}

	public static Image getImage(final String key, final String path) {
		Image image = imageRegistry.get(key);

		if (image == null) {
			registerImage(key, path);
			image = imageRegistry.get(key);
		}

		return image;
	}

	public static void registerImage(final String key, final String path) {
		if (rodinIcons.containsKey(key)) {
			registerImage(EventBUIPlugin.PLUGIN_ID, key, path);
		} else {
			registerImage(TextEditorPlugin.PLUGIN_ID, key, path);
		}
	}

	private static void registerImage(final String pluginID, final String key,
			final String path) {
		final ImageDescriptor desc = AbstractUIPlugin
				.imageDescriptorFromPlugin(pluginID, path);
		imageRegistry.put(key, desc);
	}

	public static void dispose() {
		// imageRegistry.remove(key);
	}

	public static ImageRegistry getRegistry() {
		return imageRegistry;
	}
}
