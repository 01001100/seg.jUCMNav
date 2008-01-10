package seg.jUCMNav.importexport.reports;

import java.awt.image.BufferedImage;
import java.util.HashMap;

import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.SWTGraphics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.widgets.Display;

import seg.jUCMNav.importexport.ExportImageGIF;
import seg.jUCMNav.importexport.reports.utils.ReportUtils;
import seg.jUCMNav.importexport.reports.utils.jUCMNavErrorDialog;
import ucm.map.UCMmap;
import urncore.IURNDiagram;
import urncore.URNdefinition;
import urncore.URNmodelElement;

import com.lowagie.text.Cell;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Table;

/**
 * Class creating the report's diagram figure and its description.

 * 
 * @author dessure
 * 
 */
public class PDFReportDiagram extends PDFReport {

    public PDFReportDiagram() {

    }

    /**
     * creates the diagram figure and its description
     * 
     * @param document
     *            the document we are reporting into
     * @param urndef
     *            the urn definition of the elements
     * @param mapDiagrams
     *            list of diagrams to be documented
     * @param diagram
     *            the current diagram
     * @param pagesize
     *            the rectangle with dimensions for the size of page
     */
    @SuppressWarnings("unchecked")
    public void createPDFReportDiagramsAndDescription(Document document, URNdefinition urndef, HashMap mapDiagrams, Rectangle pagesize) {
        try {
            document.add(Chunk.NEXTPAGE);

            for (int i = 0; i < urndef.getSpecDiagrams().size(); i++) {
                IURNDiagram diagram = (IURNDiagram) urndef.getSpecDiagrams().get(i);
                URNmodelElement element = (URNmodelElement) diagram;

                // diagram Header
                createHeader1(document, element.getName());

                // insert the figure
                insertDiagram(document, mapDiagrams, diagram, urndef, i, pagesize);

                // insert diagram name under figure
                insertDiagramLegend(document, element, i);

                if (diagram instanceof UCMmap) {
                    UCMDiagramSection ucmSection = new UCMDiagramSection();
                    ucmSection.createUCMDiagramDescription(document, element, diagram);
                }

                // empty line
                document.add(Chunk.NEWLINE);

                // New page
                document.add(Chunk.NEXTPAGE);
            }

        } catch (Exception e) {
            jUCMNavErrorDialog error = new jUCMNavErrorDialog(e.getMessage());
            e.printStackTrace();
        }

    }

    /**
     * creates the diagram figure header
     * 
     * @param document
     *            the document we are reporting into
     * @param string
     *            the string to show in the figure header
     */
    public void createHeader1(Document document, String string) {
        try {

            Table headerTable = ReportUtils.createTable(2, 2, 0, 100);

            Chunk name = new Chunk(string, header1Font);
            Cell nameCell = new Cell(name);
            nameCell.setColspan(2);
            nameCell.setBorderWidthBottom(1.5f);

            headerTable.addCell(nameCell);
            document.add(headerTable);

        } catch (Exception e) {
            jUCMNavErrorDialog error = new jUCMNavErrorDialog(e.getMessage());
            e.printStackTrace();

        }
    }

    /**
     * inserts the diagram figure
     * 
     * @param document
     *            the document we are reporting into
     * @param mapDiagrams
     *            list of diagrams to be documented
     * @param diagram
     *            the diagram to insert
     * @param urndef
     *            the urn definition of the elements
     * @param i
     *            the diagram number
     * @param pagesize
     *            the rectangle with dimensions for the size of page
     */
    @SuppressWarnings("unchecked")
    public void insertDiagram(Document document, HashMap mapDiagrams, IURNDiagram diagram, URNdefinition urndef, int i, Rectangle pagesize) {
        try {
            // get the high level IFigure to be saved.
            IFigure pane = (IFigure) mapDiagrams.get(diagram);

            // TODO refactor, this comes from ExportImageGIF
            Image image = new Image(Display.getCurrent(), pane.getSize().width, pane.getSize().height);

            GC gc = new GC(image);
            SWTGraphics graphics = new SWTGraphics(gc);

            // if the bounds are in the negative x/y, we don't see them without a
            // translation
            graphics.translate(-pane.getBounds().x, -pane.getBounds().y);
            pane.paint(graphics);

            // downSample the image to an 8-bit palette, using the 256 most frequently used color
            ImageData ideaImageData = ExportImageGIF.downSample(image);

            ImageData croppedImage = ReportUtils.cropImage(ideaImageData);
            java.awt.Image awtImage = ReportUtils.SWTimageToAWTImage(croppedImage);

            BufferedImage bufferedImage = (BufferedImage) awtImage;
            int imageHeight = bufferedImage.getHeight();
            int imageWidth = bufferedImage.getWidth();

            ReportUtils.insertImage(document, awtImage, pagesize, imageWidth, imageHeight);

            gc.dispose();
            image.dispose();
            awtImage.flush();

            boolean isLast = i == urndef.getSpecDiagrams().size() - 1;

        } catch (Exception e) {
            jUCMNavErrorDialog error = new jUCMNavErrorDialog(e.getMessage());
            e.printStackTrace();

        }
    }

    /**
     * inserts the diagram figure legend
     * 
     * @param document
     *            the document we are reporting into
     * @param element
     *            the element illustrated by this diagram
     * @param i
     *            the diagram number
     */
    public void insertDiagramLegend(Document document, URNmodelElement element, int i) {
        try {
            int figureNo = i + 1;
            Table headerTable = ReportUtils.createTable(1, 1, 0, 100);

            Chunk name = new Chunk("Figure " + figureNo + " - " + element.getName(), figureLegendFont);
            Cell nameCell = new Cell(name);
            nameCell.setColspan(1);
            nameCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            nameCell.setBorderWidthTop(2f);

            headerTable.addCell(nameCell);
            document.add(headerTable);

        } catch (Exception e) {
            jUCMNavErrorDialog error = new jUCMNavErrorDialog(e.getMessage());
            e.printStackTrace();

        }
    }

    /**
     * inserts the diagram figure description
     * 
     * @param document
     *            the document we are reporting into
     * @param element
     *            the element illustrated by this diagram
     */
    public void insertDiagramDescription(Document document, URNmodelElement element) {
        try {

            Table table = ReportUtils.createTable(1, 2, 0, 100);

            Chunk chunk = new Chunk("Description ", descriptionBoldFont);
            Cell descriptionCell = new Cell(chunk);
            descriptionCell.setColspan(1);
            descriptionCell.setHorizontalAlignment(Element.ALIGN_LEFT);
            descriptionCell.setBorderWidthBottom(1.5f);

            Chunk descText = new Chunk(element.getDescription(), descriptionFont);

            table.addCell(descriptionCell);
            document.add(table);
            document.add(descText);

        } catch (Exception e) {
            jUCMNavErrorDialog error = new jUCMNavErrorDialog(e.getMessage());
            e.printStackTrace();

        }
    }

}