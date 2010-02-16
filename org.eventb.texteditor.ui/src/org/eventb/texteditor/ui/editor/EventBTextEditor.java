/** 
 * (c) 2009 Lehrstuhl fuer Softwaretechnik und Programmiersprachen, 
 * Heinrich Heine Universitaet Duesseldorf
 * This software is licenced under EPL 1.0 (http://www.eclipse.org/org/documents/epl-v10.html) 
 * */

package org.eventb.texteditor.ui.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.ui.util.EditUIUtil;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.LineChangeHover;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eventb.emf.core.context.ContextPackage;
import org.eventb.emf.core.machine.MachinePackage;
import org.eventb.texteditor.ui.Images;
import org.eventb.texteditor.ui.TextEditorPlugin;
import org.eventb.texteditor.ui.outline.OutlinePage;
import org.eventb.texteditor.ui.outline.RevealSelectionListener;
import org.eventb.texteditor.ui.reconciler.EventBPresentationReconciler;
import org.eventb.texttools.PersistenceHelper;
import org.eventb.texttools.TextToolsPlugin;

public class EventBTextEditor extends TextEditor implements IGotoMarker {

	public static final String CONTENT_FORMAT = "ContentFormat";
	public static final String CONTENT_ASSIST_PROPOSAL = "ContentAssistProposal";

	private GotoMarker gotoMarkerAdapter;
	private IContentOutlinePage contentOutlinePage;
	private Resource resource;
	private ModelChangeListener modelChangeListener;
	private AdapterFactoryEditingDomain editingDomain;
	private boolean isCurrentlySaving = false;
	private EventBPresentationReconciler presentationReconciler;
	private final IPartListener activeListener;

	private boolean resourceChangedWhileDirty;

	private boolean inLinkedMode = false;

	public EventBTextEditor() {
		activeListener = new ActivateListener();
	}

	@Override
	public void init(final IEditorSite site, final IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		site.getPage().addPartListener(activeListener);
	}

	@Override
	protected void initializeEditor() {
		super.initializeEditor();

		setDocumentProvider(new DocumentProvider(this));
		final SourceViewerConfiguration viewerConfiguration = new SourceViewerConfiguration(
				this, getPreferenceStore());
		presentationReconciler = (EventBPresentationReconciler) viewerConfiguration
				.getPresentationReconciler(getSourceViewer());
		setSourceViewerConfiguration(viewerConfiguration);

		// not supported for our editor at the moment
		showChangeInformation(false);

		setEditorContextMenuId(TextEditorPlugin.TEXTEDITOR_CONTEXTMENU_ID);
	}

	@Override
	protected void createActions() {
		super.createActions();

		/*
		 * Add content assist
		 */
		final Action action = new ContentAssistAction(TextEditorPlugin
				.getPlugin().getResourceBundle(), "ContentAssistProposal.",
				this);
		action
				.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		setAction(CONTENT_ASSIST_PROPOSAL, action);
		markAsStateDependentAction(CONTENT_ASSIST_PROPOSAL, true);
	}

	@Override
	protected void doSetInput(final IEditorInput input) throws CoreException {
		final IEditorInput oldInput = getEditorInput();

		super.doSetInput(input);
		resource = null;

		registerChangeListener(oldInput, getEditorInput());
		presentationReconciler.setInputResource(getResource());

		adjustLabels();
	}

	private void registerChangeListener(final IEditorInput oldInput,
			final IEditorInput editorInput) {
		if (modelChangeListener != null && oldInput instanceof IFileEditorInput) {
			((IFileEditorInput) oldInput).getFile().getWorkspace()
					.removeResourceChangeListener(modelChangeListener);
		}

		if (editorInput instanceof IFileEditorInput) {
			modelChangeListener = new ModelChangeListener(this);
			((IFileEditorInput) editorInput).getFile().getWorkspace()
					.addResourceChangeListener(modelChangeListener);
		}
	}

	public AdapterFactoryEditingDomain getEditingDomain() {
		if (editingDomain == null) {
			final IFileEditorInput fileInput = (IFileEditorInput) getEditorInput();
			final IProject project = fileInput.getFile().getProject();

			editingDomain = TextToolsPlugin.getDefault().getResourceManager()
					.getEditingDomain(project);
		}

		return editingDomain;
	}

	@Override
	protected String[] collectContextMenuPreferencePages() {
		final List<String> result = new ArrayList<String>(Arrays.asList(super
				.collectContextMenuPreferencePages()));
		result.add("org.eventb.texteditor.ui.preferences");
		result
				.add("org.eventb.texteditor.ui.preferences.HighlightingPreferencePage");
		result.add("org.eventb.texteditor.ui.preferences.TemplatePreferences");

		return result.toArray(new String[result.size()]);
	}

	@Override
	protected LineChangeHover createChangeHover() {
		// disabling change hover because we don't support it (yet)
		return null;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void doSaveAs() {
		throw new UnsupportedOperationException(
				"'SaveAs' is not allowed for the Event-B text editor.");
	}

	@Override
	public void dispose() {
		getSite().getPage().removePartListener(activeListener);
		registerChangeListener(getEditorInput(), null);
		super.dispose();
	}

	public Resource getResource() {
		if (resource == null) {
			final IEditorInput editorInput = getEditorInput();
			if (editorInput != null) {
				final AdapterFactoryEditingDomain editingDomain = getEditingDomain();
				final URI inputUri = EditUIUtil.getURI(editorInput);
				resource = editingDomain.getResourceSet().getResource(inputUri,
						true);
			}
		}

		return resource;
	}

	public StyledText getTextWidget() {
		return getSourceViewer().getTextWidget();
	}

	public void insert(final String symbol, final boolean correctCursorPosition) {
		if (symbol != null && symbol.length() > 0) {
			getSite().getShell().getDisplay().asyncExec(new Runnable() {
				public void run() {
					final IDocument document = getDocumentProvider()
							.getDocument(getEditorInput());
					final int offset = getCursorOffset();

					try {
						document.replace(offset, 0, symbol);

						if (correctCursorPosition) {
							setCurserOffset(offset + symbol.length());
						}
					} catch (final BadLocationException e) {
						TextEditorPlugin
								.getPlugin()
								.getLog()
								.log(
										new Status(
												IStatus.WARNING,
												TextEditorPlugin.PLUGIN_ID,
												"Could not insert text into Event-B text editor.",
												e));
					}
				}
			});
		}
	}

	public int getCursorOffset() {
		final ISourceViewer sourceViewer = getSourceViewer();
		if (sourceViewer != null) {
			return widgetOffset2ModelOffset(sourceViewer, sourceViewer
					.getTextWidget().getCaretOffset());
		}

		return 0;
	}

	public void setCurserOffset(final int offsetInDocument) {
		final ISourceViewer sourceViewer = getSourceViewer();
		if (sourceViewer != null) {
			final int widgetOffset = modelOffset2WidgetOffset(sourceViewer,
					offsetInDocument);
			final StyledText textWidget = sourceViewer.getTextWidget();
			textWidget.setCaretOffset(widgetOffset);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object getAdapter(final Class adapter) {
		if (IGotoMarker.class.equals(adapter)) {
			return getGotoMarker();
		}

		if (adapter.equals(IContentOutlinePage.class)) {
			return getContentOutlinePage();
		}

		return super.getAdapter(adapter);
	}

	private Object getGotoMarker() {
		if (gotoMarkerAdapter == null) {
			gotoMarkerAdapter = new GotoMarker(this);
		}

		return gotoMarkerAdapter;
	}

	private IContentOutlinePage getContentOutlinePage() {
		if (contentOutlinePage == null) {
			contentOutlinePage = new OutlinePage(this);
			contentOutlinePage
					.addSelectionChangedListener(new RevealSelectionListener(
							this));
		}

		return contentOutlinePage;
	}

	public void updateIsDirty() {
		firePropertyChange(PROP_DIRTY);
	}

	public void setIsSaving(final boolean state) {
		isCurrentlySaving = state;
	}

	public boolean isSaving() {
		return isCurrentlySaving;
	}

	public final int widgetOffset2ModelOffset(final int widgetOffset) {
		return widgetOffset2ModelOffset(getSourceViewer(), widgetOffset);
	}

	public final int modelOffset2WidgetOffset(final int modelOffset) {
		return modelOffset2WidgetOffset(getSourceViewer(), modelOffset);
	}

	public EventBPresentationReconciler getPresentationReconciler() {
		return presentationReconciler;
	}

	public void changeWhileDirty() {
		resourceChangedWhileDirty = true;
	}

	/**
	 * Change whether the editor is currently in 'linked mode'.
	 * 
	 * @param linkedMode
	 */
	public void setInLinkedMode(final boolean linkedMode) {
		inLinkedMode = linkedMode;
	}

	/**
	 * Returns whether the editor is currently in 'linked mode', i.e., when a
	 * template has just been inserted.
	 * 
	 * @see LinkedModeUI
	 * @return
	 */
	public boolean isInLinkedMode() {
		return inLinkedMode;
	}

	private void adjustLabels() {
		final IEditorInput input = getEditorInput();

		if (input != null) {
			final URI inputUri = EditUIUtil.getURI(input);
			final Resource resource = getEditingDomain().getResourceSet()
					.getResource(inputUri, true);

			changeImage(resource);

			if (input instanceof IFileEditorInput) {
				final IFile file = ((IFileEditorInput) input).getFile();
				String name = file.getName();
				name = name.substring(0, name.length()
						- file.getFileExtension().length() - 1);
				setPartName(name);
			}
		}
	}

	private void changeImage(final Resource resource) {
		final EClass componentType = PersistenceHelper
				.getComponentType(resource);
		if (componentType != null) {
			final String packageNsURI = componentType.getEPackage().getNsURI();
			if (packageNsURI.equals(MachinePackage.eNS_URI)) {
				setTitleImage(Images.getImage(Images.IMG_MACHINE));
			} else if (packageNsURI.equals(ContextPackage.eNS_URI)) {
				setTitleImage(Images.getImage(Images.IMG_CONTEXT));
			}
		}
	}

	private void handleActive() {
		if (resourceChangedWhileDirty) {
			try {
				handleEditorInputChanged();
			} finally {
				resourceChangedWhileDirty = false;
			}
		}

		// only update presentation
		presentationReconciler.reconcilePresentation();
	}

	class ActivateListener implements IPartListener {

		public void partActivated(final IWorkbenchPart part) {
			if (part == EventBTextEditor.this) {
				handleActive();
			}
		}

		public void partBroughtToTop(final IWorkbenchPart part) {
			// IGNORE
		}

		public void partClosed(final IWorkbenchPart part) {
			// IGNORE
		}

		public void partDeactivated(final IWorkbenchPart part) {
			// IGNORE
		}

		public void partOpened(final IWorkbenchPart part) {
			// IGNORE
		}
	}
}
