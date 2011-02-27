package org.maptools.services;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.image.BufferedImage;
import java.awt.image.ByteLookupTable;
import java.awt.image.LookupOp;
import java.awt.image.LookupTable;
import java.awt.image.Raster;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geotoolkit.geometry.GeneralDirectPosition;
import org.geotoolkit.referencing.CRS;
import org.geotoolkit.referencing.crs.DefaultGeocentricCRS;
import org.geotoolkit.referencing.crs.DefaultGeographicCRS;
import org.maptools.helper.servlet;
import org.imaging.composite.BlendComposite;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 *
 * @author opengeogroep
 *
 * Credits go to Johan Liesen for his tutorial at:
 * http://www.itstud.chalmers.se/~liesen/heatmap/
 * uses http://www.geotoolkit.org/
 * 
 */
public class heatmap extends HttpServlet {

    private static final long serialVersionUID = -2105845119293049049L;
    private final BufferedImage dotImage = createFadedCircleImage(200);
    private BufferedImage heatmapImage;
    private BufferedImage monochromeImage;
    private BufferedImage colorImage;
    private static final int WIDTH = 512;
    private static final int HEIGHT = 512;
    private LookupTable colorTable;
    private LookupOp colorOp;

    // Forward POST to handle it the same way as GET.
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        doGet(req, resp);
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Integer width = WIDTH;
            Integer height = HEIGHT;
            String stringWidth = servlet.getCaseInsensitiveParam(request.getParameterMap(), "width");

            if (stringWidth != null) {
                width = Integer.parseInt(stringWidth);
            }
            String stringHeight = servlet.getCaseInsensitiveParam(request.getParameterMap(), "height");

            if (stringHeight != null) {
                height = Integer.parseInt(stringHeight);
            }
            response.setContentType("image/png");

            ServletOutputStream out = response.getOutputStream();

            String stringType = servlet.getCaseInsensitiveParam(request.getParameterMap(), "type");
            if (stringType == null ? "demo" == null : stringType.equals("demo")) {
                renderDemo(width,height);
            } else {
                render();
            }
            ImageIO.write(heatmapImage, "png", out);
        } catch (Exception e) {
        } finally {
        }
    }
    
    private BufferedImage render() throws MismatchedDimensionException, FactoryException, TransformException {
        //TODO: transform from real coords to screen coords
        //TODO: set circle size in meters and transform them too to reflect projection distortion
        CoordinateReferenceSystem sourceCRS = DefaultGeocentricCRS.CARTESIAN;
        CoordinateReferenceSystem targetCRS = DefaultGeographicCRS.WGS84_3D;
        MathTransform tr = CRS.findMathTransform(sourceCRS, targetCRS);
        DirectPosition sourcePt = new GeneralDirectPosition(302742.5, 5636029.0, 2979489.2);
        DirectPosition targetPt = tr.transform(sourcePt, null);
        System.out.println("Source point: " + sourcePt);
        System.out.println("Target point: " + targetPt);
        //TODO: parse the new point to screen for the addDotImage function
        return null;
    }
    private BufferedImage renderDemo(Integer width, Integer height) {
        colorImage = createEvenlyDistributedGradientImage(new Dimension(
                //96, 1), Color.WHITE, Color.RED, Color.YELLOW, Color.GREEN.darker(), Color.CYAN, Color.BLUE, new Color(0, 0, 0x33));
                96, 1), Color.WHITE, Color.RED, Color.ORANGE, Color.ORANGE.brighter(), Color.YELLOW.brighter(), Color.WHITE);
        colorTable = createColorLookupTable(colorImage, .5f);
        colorOp = new LookupOp(colorTable, null);
        monochromeImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics g = monochromeImage.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        for (int number = 1; number <= 30; number++) {

        Integer randomx = new Integer((int) (Math.floor(Math.random() * (width + 1))));
        Integer randomy = new Integer((int) (Math.floor(Math.random() * (height + 1))));
        addDotImage(new Point(randomx, randomy), .75f);
        }
        
        heatmapImage = colorize(colorOp);
        return heatmapImage;

    }

    private void addDotImage(Point p, float alpha) {
        /*
         * TODO: Make circle radius optional and devided into radius in North and
         * west direction so the circle can be transformed
         * to reflect projection distortion
         */
        int circleRadius = dotImage.getWidth() / 2;

        Graphics2D g = (Graphics2D) monochromeImage.getGraphics();

        g.setComposite(BlendComposite.Multiply.derive(alpha));
        g.drawImage(dotImage, null, p.x - circleRadius, p.y - circleRadius);
    }

    public static LookupTable createColorLookupTable(BufferedImage im,
            float alpha) {
        int tableSize = 256;
        Raster imageRaster = im.getData();
        double sampleStep = 1D * im.getWidth() / tableSize; // Sample pixels
        // evenly
        byte[][] colorTable = new byte[4][tableSize];
        int[] pixel = new int[1]; // Sample pixel
        Color c;

        for (int i = 0; i < tableSize; ++i) {
            imageRaster.getDataElements((int) (i * sampleStep), 0, pixel);

            c = new Color(pixel[0]);

            colorTable[0][i] = (byte) c.getRed();
            colorTable[1][i] = (byte) c.getGreen();
            colorTable[2][i] = (byte) c.getBlue();
            colorTable[3][i] = (byte) (alpha * 0xff);
        }

        LookupTable lookupTable = new ByteLookupTable(0, colorTable);

        return lookupTable;
    }

    public static BufferedImage createEvenlyDistributedGradientImage(
            Dimension size, Color... colors) {
        BufferedImage im = new BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = im.createGraphics();

        float[] fractions = new float[colors.length];
        float step = 1f / colors.length;

        for (int i = 0; i < colors.length; ++i) {
            fractions[i] = i * step;
        }

        LinearGradientPaint gradient = new LinearGradientPaint(
                0, 0, size.width, 1, fractions, colors,
                MultipleGradientPaint.CycleMethod.REPEAT);

        g.setPaint(gradient);
        g.fillRect(0, 0, size.width, size.height);

        g.dispose();

        return im;
    }

    public BufferedImage colorize(LookupOp colorOp) {
        return colorOp.filter(monochromeImage, null);
    }

    public BufferedImage colorize(LookupTable colorTable) {
        return colorize(new LookupOp(colorTable, null));
    }

    public static BufferedImage createFadedCircleImage(int size) {
        BufferedImage im = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        float radius = size / 2f;

        RadialGradientPaint gradient = new RadialGradientPaint(
                radius, radius, radius, new float[]{0f, 1f}, new Color[]{
                    Color.BLACK, new Color(0xffffffff, true)});

        Graphics2D g = (Graphics2D) im.getGraphics();

        g.setPaint(gradient);
        g.fillRect(0, 0, size, size);

        g.dispose();

        return im;
    }
}
