package seg.jUCMNav.editparts.treeEditparts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.eclipse.gef.EditPolicy;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;

import seg.jUCMNav.JUCMNavPlugin;
import seg.jUCMNav.editpolicies.element.MapComponentEditPolicy;
import seg.jUCMNav.model.util.EObjectClassNameComparator;
import ucm.map.Connect;
import ucm.map.Map;
import ucm.map.PathGraph;
import ucm.map.PathNode;

/**
 * TreeEditPart for the Map and PathGraph.
 * 
 * @author TremblaE
 */
public class MapTreeEditPart extends UcmModelElementTreeEditPart {

    /**
     * @param model
     *            the map
     */
    public MapTreeEditPart(Map model) {
        super(model);
    }

    /**
     * Listens to both Map and PathGraph
     */
    public void activate() {
        super.activate();
        getMap().getPathGraph().eAdapters().add(this);
    }

    /**
     * Remove listeners.
     */
    public void deactivate() {
        super.deactivate();
        getMap().getPathGraph().eAdapters().remove(this);
    }

    /**
     * @see org.eclipse.gef.editparts.AbstractTreeEditPart#createEditPolicies()
     */
    protected void createEditPolicies() {
        installEditPolicy(EditPolicy.COMPONENT_ROLE, new MapComponentEditPolicy());
    }

    /**
     * Returns list of components and pathnodes, sorted by type and name using EObjectClassNameComparator
     * 
     * Doesn't include connects
     * 
     * @see org.eclipse.gef.EditPart#getChildren()
     */
    public List getModelChildren() {
        ArrayList list = new ArrayList();
        Map map = getMap();
        PathGraph graph = map.getPathGraph();
        list.addAll(map.getCompRefs());
        Vector v = new Vector();
        for (Iterator iter = graph.getPathNodes().iterator(); iter.hasNext();) {
            PathNode element = (PathNode) iter.next();
            if (!(element instanceof Connect))
                v.add(element);
        }
        list.addAll(v);

        Collections.sort(list, new EObjectClassNameComparator());
        return list;
    }

    /**
     * @return the map being represented.
     */
    private Map getMap() {
        return ((Map) getModel());
    }

    /**
     * Returns an icon representing a map.
     */
    protected Image getImage() {
        if (super.getImage() == null)
            setImage((ImageDescriptor.createFromFile(JUCMNavPlugin.class, "icons/icon16.gif")).createImage()); //$NON-NLS-1$
        return super.getImage();
    }

}