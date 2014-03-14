/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texttools;

import java.io.IOException;
import java.util.Collections;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.EMap;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eventb.emf.core.Attribute;
import org.eventb.emf.core.AttributeType;
import org.eventb.emf.core.CoreFactory;
import org.eventb.emf.core.EventBElement;
import org.eventb.emf.core.EventBNamedCommentedComponentElement;
import org.eventb.texttools.merge.ModelMerge;
import org.eventb.texttools.prettyprint.PrettyPrinter;

import de.be4.eventb.core.parser.BException;
import de.be4.eventb.core.parser.EventBParser;
import de.be4.eventb.core.parser.node.AContextParseUnit;
import de.be4.eventb.core.parser.node.AMachineParseUnit;
import de.be4.eventb.core.parser.node.PParseUnit;
import de.be4.eventb.core.parser.node.Start;

public class PersistenceHelper {

	public static final Boolean DEBUG = false;

	public static IResource getIResource(final Resource resource) {
		final URI uri = resource.getURI();
		if (uri.isPlatformResource()) {
			return ResourcesPlugin.getWorkspace().getRoot()
					.findMember(uri.toPlatformString(true));
		}

		return null;
	}

	public static void saveText(final Resource resource,
			final boolean overwrite, final IProgressMonitor monitor)
			throws CoreException {
		try {
			resource.save(Collections.EMPTY_MAP);

			/*
			 * Try to set timestamp to the same as in the annotation. Setting on
			 * both Resource and IResource to be save.
			 */
			//final long textTimestamp = getTextTimestamp(resource);
			// REMOVED WORKAROUND BELOW: a better fix is to not ignore attributes in EventBReferencesCheck
			// --- TODO: Workaround for save bug. The method getTextTimestamp
			// --- returns in some cases -1 (no time annotation exists). I noticed
			// --- that many different instances of the same resource exist. Some
			// --- are annotated, the others not.
			long textTimestamp = getTextTimestamp(resource);
//			if(textTimestamp == -1)
//				textTimestamp = System.currentTimeMillis();
			resource.setTimeStamp(textTimestamp);
			getIResource(resource).setLocalTimeStamp(textTimestamp);
		} catch (final IOException e) {
			throw new CoreException(new Status(IStatus.ERROR,
					TextToolsPlugin.PLUGIN_ID, "Saving to RodinDB failed", e));
		}
	}

	public static void addTextAnnotation(final Resource resource,
			final String textRepresentation, final long timeStamp)
			throws CoreException {
		final EventBNamedCommentedComponentElement component = getComponent(resource);
		if (component != null) {
			addTextAnnotation(component, textRepresentation, timeStamp);
		} else {
			throw new CoreException(new Status(IStatus.ERROR,
					TextToolsPlugin.PLUGIN_ID,
					"Resource has no EventBComponent"));
		}
	}

	public static void addTextAnnotation(final EventBElement element,
			final String textRepresentation, final long timeStamp) {
		final EMap<String, Attribute> attributes = element.getAttributes();

		// update text representation
		Attribute textAttribute = attributes
				.get(TextToolsPlugin.TYPE_TEXTREPRESENTATION.getId());
		if (textAttribute == null) {
			textAttribute = CoreFactory.eINSTANCE.createAttribute();
			textAttribute.setType(AttributeType.STRING);
			attributes.put(TextToolsPlugin.TYPE_TEXTREPRESENTATION.getId(),
					textAttribute);
		}
		textAttribute.setValue(textRepresentation);

		// update timestamp
		Attribute timeAttribute = attributes
				.get(TextToolsPlugin.TYPE_LASTMODIFIED.getId());
		if (timeAttribute == null) {
			timeAttribute = CoreFactory.eINSTANCE.createAttribute();
			timeAttribute.setType(AttributeType.LONG);
			attributes.put(TextToolsPlugin.TYPE_LASTMODIFIED.getId(),
					timeAttribute);
		}
		timeAttribute.setValue(timeStamp);
	}

	public static void addUsesAnnotation(final EventBElement element,
			final String usesStatements) {
		final EMap<String, Attribute> attributes = element.getAttributes();
		Attribute usesAttribute = attributes
				.get(TextToolsPlugin.TYPE_USESEXTENSION.getId());
		if (usesAttribute == null) {
			usesAttribute = CoreFactory.eINSTANCE.createAttribute();
			usesAttribute.setType(AttributeType.STRING);
			attributes.put(TextToolsPlugin.TYPE_USESEXTENSION.getId(),
					usesAttribute);
		}
		usesAttribute.setValue(usesStatements);

	}

	private static void mergeComponents(
			final EventBNamedCommentedComponentElement oldVersion,
			final EventBNamedCommentedComponentElement newVersion,
			final IProgressMonitor monitor) {
		try {
			long time0 = System.currentTimeMillis();
			final ModelMerge merge = new ModelMerge(oldVersion, newVersion);
			long time1 = System.currentTimeMillis();
			merge.applyChanges(monitor);
			long time2 = System.currentTimeMillis();
			if (DEBUG) {
				System.out.println("new ModelMerge: " + (time1 - time0));
				System.out.println("merge.applyChanges: " + (time2 - time1));
			}
		} catch (final InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void mergeRootElement(final Resource resource,
			final EventBNamedCommentedComponentElement newVersion,
			final IProgressMonitor monitor) {
		final EventBNamedCommentedComponentElement component = getComponent(resource);
		if (component != null) {
			// FIXME Hier stimmt die Reihenfolge noch
			long start = System.currentTimeMillis();
			mergeComponents(component, newVersion, monitor);
			long end = System.currentTimeMillis();
			if (DEBUG) {
				System.out
						.println("Time to merge components: " + (end - start));
			}
			// Hier stimmt die Reihenfolge in component nicht mehr
		} else {
			resource.getContents().add(newVersion);
		}
	}

	public static String loadText(final Resource resource,
			final String linebreak) throws IOException {
		// make sure resource is loaded
		if (!resource.isLoaded()) {
			resource.load(Collections.EMPTY_MAP);
		}

		// check if we have the most recent text representation
		if (isTextUptodate(resource)) {
			// we should find a text representation in the EMF
			final String text = getTextAnnotation(resource);


			/* workaround for Bug #3305107 
			 * 
			 * When a machine- or contextfile is renamed the lastmodified date does not change.
			 * Since isTextUptodate() compares timestamps only, it returns true for renamed files.
			 * */
			boolean namesMatch = true;
			final EventBNamedCommentedComponentElement rootElement = getComponent(resource);
			if (rootElement != null) {
				final EventBParser parser = new EventBParser();
				try {
					Start start = parser.parse(text, false);
					System.out.println(start);
					PParseUnit pParseUnit = start.getPParseUnit();
					String parsedName = null;
					if (pParseUnit instanceof AMachineParseUnit){
						parsedName = ((AMachineParseUnit)start.getPParseUnit()).getName().getText();
					}
					if (pParseUnit instanceof AContextParseUnit){
						parsedName = ((AContextParseUnit)start.getPParseUnit()).getName().getText();
					}
					
					if (parsedName != null){
						if (!parsedName.equals(rootElement.getName())){
							namesMatch = false;
							System.err.println("Conflicting names of ParseUnit! Expected name: '" + rootElement.getName() + "' actual name: '" + parsedName + "'! Prettyprinting unit...");
						}
					}
				} catch (BException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			/* End of woraround */
			
			if (text != null && namesMatch) {
				return text;
			}
		}

		/*
		 * Reload resource to get latest changes. This is necessary if the model
		 * was already loaded and external changes have occured meanwhile. Then
		 * unloading+loading makes sure the EMF persistence loads those changes.
		 */
		resource.unload();
		resource.load(Collections.EMPTY_MAP);

		// pretty-print the machine/context
		final EventBNamedCommentedComponentElement rootElement = getComponent(resource);
		if (rootElement != null) {
			return getPrettyPrint(rootElement, linebreak);
		} else {
			throw new IOException(
					"Cannot find load Event-B component: No machine/context found");
		}
	}

	public static String getTextAnnotation(final Resource resource) {
		final EMap<String, Attribute> attributes = getAttributesMap(resource);
		if (attributes != null) {
			final Attribute attr = attributes
					.get(TextToolsPlugin.TYPE_TEXTREPRESENTATION.getId());

			if (attr != null) {
				return (String) attr.getValue();
			}
		}

		return null;
	}

	public static EClass getComponentType(final Resource resource) {
		final EventBNamedCommentedComponentElement component = getComponent(resource);

		if (component != null) {
			return component.eClass();
		}

		return null;
	}

	private static EventBNamedCommentedComponentElement getComponent(
			final Resource resource) {
		final EList<EObject> contents = resource.getContents();
		if (contents.size() > 0
				&& contents.get(0) instanceof EventBNamedCommentedComponentElement) {
			return (EventBNamedCommentedComponentElement) contents.get(0);
		}

		return null;
	}

	/**
	 * Extracts the timestamp of the latest saved text representation from the
	 * EMF and returns it.
	 * 
	 * @param resource
	 * @return timestamp or <code>-1</code> if none is found
	 */
	private static long getTextTimestamp(final Resource resource) {
		final EMap<String, Attribute> attributes = getAttributesMap(resource);
		if (attributes != null) {
			final Attribute attr = attributes
					.get(TextToolsPlugin.TYPE_LASTMODIFIED.getId());
			return attr != null ? (Long) attr.getValue() : -1;
		}

		return -1;
	}

	private static EMap<String, Attribute> getAttributesMap(
			final Resource resource) {
		final EList<EObject> contents = resource.getContents();
		if (contents.size() > 0) {
			final EObject object = contents.get(0);
			if (object instanceof EventBNamedCommentedComponentElement) {
				final EventBNamedCommentedComponentElement component = (EventBNamedCommentedComponentElement) object;
				return component.getAttributes();
			}
		}

		return null;
	}

	/**
	 * Checks if the text representation saved in the EMF is up-to-date. The
	 * timestamps in the EMF and of the underlying file are compared for this
	 * decision.
	 * 
	 * @param resource
	 * @return <code>true</code> if there was no external change and the text
	 *         representation is still up-to-date
	 */
	private static boolean isTextUptodate(final Resource resource) {
		final long textTimestamp = getTextTimestamp(resource);

		if (true){
			//return false;
		}
		
		try {
			final IResource file = getIResource(resource);
			// refresh to get latest timestamp
			file.refreshLocal(IResource.DEPTH_ONE, null);
			final long resourceTimestamp = file.getLocalTimeStamp();

			// easy case: text is newer than resource
			if (textTimestamp >= resourceTimestamp) {
				return true;
			}

			final long diff = resourceTimestamp - textTimestamp;

			// tolerate 50ms offset (time to save file)
			// TODO this is ugly!!!
			if (diff < 50) {
				return true;
			}
		} catch (final CoreException e) {
			TextToolsPlugin
					.getDefault()
					.getLog()
					.log(new Status(IStatus.ERROR, TextToolsPlugin.PLUGIN_ID,
							"Error checking file timestamps", e));
		}

		return false;
	}

	private static String getPrettyPrint(final EventBElement rootElement,
			final String linebreak) {
		final StringBuilder buffer = new StringBuilder();
		new PrettyPrinter(buffer, linebreak, null).prettyPrint(rootElement);

		return buffer.toString();
	}
}
