/**
 * 
 */
package seg.jUCMNav.editors;

import java.util.EventObject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.KeyHandler;
import org.eclipse.gef.KeyStroke;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite.FlyoutPreferences;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.SelectionSynchronizer;
import org.eclipse.gef.ui.parts.TreeViewer;
import org.eclipse.gef.ui.properties.UndoablePropertySheetEntry;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.PropertySheetPage;

import seg.jUCMNav.Messages;
import seg.jUCMNav.actions.SelectDefaultPaletteToolAction;
import seg.jUCMNav.views.outline.UrnOutlinePage;
import urncore.IURNDiagram;

/**
 * This is an abstract class for any urn editor used in UCMNavMultiPageEditor
 * 
 * @author Jean-Fran�ois Roy
 *TODO Remove extends to GraphicalEditorWithFlyoutPalette and copy code in this class
 */
public abstract class UrnEditor extends GraphicalEditorWithFlyoutPalette {

    /** the parent containing the action registry */
    protected UCMNavMultiPageEditor parent;

    /** Cache save-request status. */
    private boolean saveAlreadyRequested;

    /** KeyHandler with common bindings for both the Outline View and the Editor. */
    private KeyHandler sharedKeyHandler;
   
    // our outline page.
    private UrnOutlinePage outline;

    /** Create a new UrnEditor instance. */
    public UrnEditor(UCMNavMultiPageEditor parent) {
        this.parent = parent;
        setEditDomain(new DefaultEditDomain(this));
    }

    /**
     * Handle events to know when a command was executed. So we can know when we can or cannot save the file.
     */
    public void commandStackChanged(EventObject event) {
        // Note: some actions go directly to this command stack and others go to our parents.

        super.commandStackChanged(event);
        if (isDirty() && !saveAlreadyRequested) {
            saveAlreadyRequested = true;
            firePropertyChange(IEditorPart.PROP_DIRTY);
        } else {
            saveAlreadyRequested = false;
            firePropertyChange(IEditorPart.PROP_DIRTY);
        }
    }

    /**
     * Create all the actions in the action registry.
     * 
     * Now done in multipage.
     * 
     * DO NOT DELETE: SEE SUPERCLASS
     * @see org.eclipse.gef.ui.parts.GraphicalEditor#createActions()
     */
    protected void createActions() {
        // now done in MultiPage
    }

    /**
     * Used when UcmEditor was the root model element.
     * 
     * @deprecated
     */
    public void doSave(IProgressMonitor monitor) {
        System.out.println(Messages.getString("UcmEditor.oldSave")); //$NON-NLS-1$
    }

    /**
     * Used when UcmEditor was the root model element.
     * 
     * @deprecated
     */
    public void doSaveAs() {
        System.out.println(Messages.getString("UcmEditor.oldSaveAs")); //$NON-NLS-1$
    }

    /**
     * Execute a command in the parent's command stack.
     * 
     * @param cmd
     */
    public void execute(Command cmd) {
        parent.getDelegatingCommandStack().execute(cmd);
    }
    
    /**
     * Returns a reference to the multi page action registry.
     */
    protected ActionRegistry getActionRegistry() {
        // one action registry for all editors
        return parent.getActionRegistry();
    }

    /**
     * Returns the adapter for the specified key. Such as the property sheet, the outline view etc.
     * 
     * @see org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette#getAdapter(java.lang.Class)
     */
    public Object getAdapter(Class type) {
        if (type == ZoomManager.class)
            return getGraphicalViewer().getProperty(ZoomManager.class.toString());
        else if (type == ActionRegistry.class)
            return getActionRegistry();
        else if (type == IContentOutlinePage.class)
            return getOutlinePage();
        else if (type == org.eclipse.ui.views.properties.IPropertySheetPage.class) {
            PropertySheetPage page = new PropertySheetPage();
            page.setRootEntry(new UndoablePropertySheetEntry(getParent().getDelegatingCommandStack()));
            return page;
        }

        return super.getAdapter(type);
    }

    /**
     * Returns the KeyHandler with common bindings for both the Outline and Graphical Views. For example, delete is a common action.
     */
    KeyHandler getCommonKeyHandler() {
        if (sharedKeyHandler == null) {
            sharedKeyHandler = new KeyHandler();

            // Add key and action pairs to sharedKeyHandler
            sharedKeyHandler.put(KeyStroke.getPressed(SWT.DEL, 127, 0), getActionRegistry().getAction(ActionFactory.DELETE.getId()));

            sharedKeyHandler.put(KeyStroke.getPressed(SWT.F2, 0), getActionRegistry().getAction(GEFActionConstants.DIRECT_EDIT));

            sharedKeyHandler.put(KeyStroke.getPressed((char) 1, (int) 'a', SWT.CTRL), getActionRegistry().getAction(ActionFactory.SELECT_ALL.getId()));

            sharedKeyHandler.put(KeyStroke.getReleased(SWT.ESC, SWT.ESC, 0), getActionRegistry()
                    .getAction(SelectDefaultPaletteToolAction.SETDEFAULTPALETTETOOL));

        }
        return sharedKeyHandler;
    }
    
    /**
     * Overridden to change to public visibility.
     */
    public abstract CommandStack getCommandStack();
    
    /**
     * Overriden to allow external access.
     * 
     * @see org.eclipse.gef.ui.parts.GraphicalEditor#getEditDomain()
     */
    public DefaultEditDomain getEditDomain() {
        return super.getEditDomain();
    }

    /**
     * Overriden to allow external access.
     */
    public GraphicalViewer getGraphicalViewer() {
        return super.getGraphicalViewer();
    }

    /**
     * Return the model of this editor
     * 
     * @return The model of this editor
     */
    public abstract IURNDiagram getModel();

    /**
     * Set the model of this editor
     * 
     */

    /**
     * @return the outline associated with this editor
     */
    private UrnOutlinePage getOutlinePage() {
        if (outline == null)
            outline = new UrnOutlinePage(getParent(), new TreeViewer());
        return outline;
    }
    
    public abstract void setModel(IURNDiagram m);
    
    /* (non-Javadoc)
     * @see org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette#getPalettePreferences()
     */
    protected abstract FlyoutPreferences getPalettePreferences(); 

    /* (non-Javadoc)
     * @see org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette#getPaletteRoot()
     */
    protected abstract PaletteRoot getPaletteRoot();

    /**
     * Returns this editor's container.
     * 
     * @return parent multi-page editor
     */
    public UCMNavMultiPageEditor getParent() {
        return parent;
    }

    /**
     * Returns the selection syncronizer object. The synchronizer can be used to sync the selection of 2 or more EditPartViewers.
     * 
     * @return the syncrhonizer
     */
    protected SelectionSynchronizer getSelectionSynchronizer() {
        return parent.getSelectionSynchronizer();
    }

    /**
     * Return true if the editor contains any changes.
     * 
     * @see org.eclipse.ui.ISaveablePart#isDirty()
     */
    public boolean isDirty() {
        return getCommandStack().isDirty();
    }
    
    /**
     * Save is always allowed, even though it won't happen here.
     * 
     * @see org.eclipse.ui.ISaveablePart#isSaveAsAllowed()
     */
    public boolean isSaveAsAllowed() {
        return true;
    }

    /**
     * Redo's the command at the top of the parent's redo stack.
     * 
     */
    public void redo() {
        parent.getDelegatingCommandStack().redo();
    }

    /**
     * Undo's the command at the top of the parent's undo stack.
     * 
     */
    public void undo() {
        parent.getDelegatingCommandStack().undo();
    }
}