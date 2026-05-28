package id.rockierocker.imagetools.util;

import org.apache.batik.parser.AWTPathProducer;
import org.apache.batik.parser.PathParser;

import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class SvgPointExtractor {

    public static List<Point2D> extractPoints(
            String pathData
    ) {

        List<Point2D> points =
                new ArrayList<>();

        try {

            AWTPathProducer producer =
                    new AWTPathProducer();

            PathParser parser =
                    new PathParser();

            parser.setPathHandler(producer);

            parser.parse(pathData);

            Shape shape =
                    producer.getShape();

            PathIterator iterator =
                    shape.getPathIterator(null);

            double[] coords =
                    new double[6];

            while (!iterator.isDone()) {

                int segmentType =
                        iterator.currentSegment(coords);

                switch (segmentType) {

                    case PathIterator.SEG_MOVETO:

                        points.add(
                                new Point2D.Double(
                                        coords[0],
                                        coords[1]
                                )
                        );

                        break;

                    case PathIterator.SEG_LINETO:

                        points.add(
                                new Point2D.Double(
                                        coords[0],
                                        coords[1]
                                )
                        );

                        break;

                    case PathIterator.SEG_QUADTO:

                        points.add(
                                new Point2D.Double(
                                        coords[2],
                                        coords[3]
                                )
                        );

                        break;

                    case PathIterator.SEG_CUBICTO:

                        points.add(
                                new Point2D.Double(
                                        coords[4],
                                        coords[5]
                                )
                        );

                        break;

                    case PathIterator.SEG_CLOSE:

                        break;
                }

                iterator.next();
            }

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed parse SVG path",
                    e
            );
        }

        return points;
    }
}