package fr.shom.ping.utils;

import fr.shom.ping.exception.ServiceException;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.commons.io.FileUtils;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.ImmutableMap.of;
import static fr.shom.ping.utils.ZipUtil.generateZipFileAndAttachToServletResponse;
import static java.lang.Boolean.TRUE;
import static java.nio.file.Files.createDirectories;
import static java.util.stream.Collectors.toList;

public abstract class ShpUtil {

    private static final Logger log = LoggerFactory.getLogger(ShpUtil.class);

    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    protected static void generateShp(String generatedShapefileFolder, UUID objectId, List<Geometry> geometries, HttpServletResponse response) throws ServiceException {
        String shapefilesDirectoryPathname = Path.of(generatedShapefileFolder, objectId.toString()).toString();
        deleteCurrentShapefilesDirectory(shapefilesDirectoryPathname);
        createNewShapefilesDirectory(shapefilesDirectoryPathname);

        buildShapefileByGeometryType(geometries, Point.class, shapefilesDirectoryPathname);
        buildShapefileByGeometryType(geometries, LineString.class, shapefilesDirectoryPathname);
        buildShapefileByGeometryType(geometries, Polygon.class, shapefilesDirectoryPathname);

        generateZipFileAndAttachToServletResponse(shapefilesDirectoryPathname, response);
    }

    private static void createNewShapefilesDirectory(String shapefilesDirectoryPathname) throws ServiceException {
        try {
            createDirectories(Path.of(shapefilesDirectoryPathname));
        } catch (IOException exception) {
            throw new ServiceException(String.format("Failed to create directory %s : %s", shapefilesDirectoryPathname, exception.getMessage()));
        }
    }

    private static void deleteCurrentShapefilesDirectory(String shapefilesDirectoryPathname) throws ServiceException {
        try {
            File currentDirectory = new File(shapefilesDirectoryPathname);
            if (currentDirectory.exists()) {
                FileUtils.deleteDirectory(currentDirectory);
            }
        } catch (IOException exception) {
            throw new ServiceException("Failed to delete current directory : " + exception.getMessage());
        }
    }

    private static void buildShapefileByGeometryType(List<? extends Geometry> geometries, Class type, String shapefilesDirectoryPathname) throws ServiceException {
        List<? extends Geometry> filteredGeometries = geometries.stream()
            .filter(geometry -> geometry.getGeometryType().equalsIgnoreCase(type.getSimpleName()))
            .collect(toList());

        if (filteredGeometries.isEmpty()) {
            log.debug("No shape files found for geometry type {}", type.getSimpleName());
            return;
        }

        SimpleFeatureType simpleFeatureType = buildFeatureType(type);
        List<SimpleFeature> features = buildFeaturesFromGeometries(filteredGeometries, simpleFeatureType);
        createShapefileDataStore(type, shapefilesDirectoryPathname, simpleFeatureType, features);
    }

    private static SimpleFeatureType buildFeatureType(Class type) throws ServiceException {
        SimpleFeatureType simpleFeatureType;
        try {
            simpleFeatureType = DataUtilities.createType("Location", "the_geom:" + type.getSimpleName() + ":srid=3857");
        } catch (SchemaException exception) {
            throw new ServiceException("Failed to build simple feature type : " + exception.getMessage());
        }
        return simpleFeatureType;
    }

    private static List<SimpleFeature> buildFeaturesFromGeometries(List<? extends Geometry> geometries,
        SimpleFeatureType simpleFeatureType) {
        List<SimpleFeature> features = new ArrayList<>();
        geometries.forEach(geometry -> {
            SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(simpleFeatureType);
            featureBuilder.add(geometry);
            features.add(featureBuilder.buildFeature(null));
        });
        return features;
    }

    private static void createShapefileDataStore(Class type, String shapefilesDirectoryPathname, SimpleFeatureType simpleFeatureType,
        List<SimpleFeature> features) throws ServiceException {
        File file;
        try {
            file = Files.createFile(Path.of(shapefilesDirectoryPathname, type.getSimpleName().toLowerCase() + ".shp")).toFile();
        } catch (IOException exception) {
            throw new ServiceException("Failed to create shapefile : " + exception.getMessage());
        }
        ShapefileDataStoreFactory factory = new ShapefileDataStoreFactory();
        ShapefileDataStore datastore;
        SimpleFeatureSource featureSource;
        Transaction transaction;
        try {
            datastore = (ShapefileDataStore) factory.createNewDataStore(of(
                "url", file.toURI().toURL(),
                "create spatial index", TRUE
            ));
            datastore.createSchema(simpleFeatureType);
            transaction = new DefaultTransaction("create");
            featureSource = datastore.getFeatureSource(datastore.getTypeNames()[0]);
        } catch (IOException exception) {
            throw new ServiceException("Failed to initialize datastore : " + exception.getMessage());
        }

        if (!(featureSource instanceof SimpleFeatureStore)) {
            return;
        }

        SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
        SimpleFeatureCollection collection = new ListFeatureCollection(simpleFeatureType, features);
        featureStore.setTransaction(transaction);
        try {
            featureStore.addFeatures(collection);
            transaction.commit();
        } catch (Exception exception) {
            rollbackTransaction(transaction);
            throw new ServiceException("Failed to commit features : " + exception.getMessage());
        } finally {
            closeTransaction(transaction);
        }
    }

    private static void rollbackTransaction(Transaction transaction) throws ServiceException {
        try {
            transaction.rollback();
        } catch (IOException exception) {
            throw new ServiceException("Failed to rollback transaction : " + exception.getMessage());
        }
    }

    private static void closeTransaction(Transaction transaction) throws ServiceException {
        try {
            transaction.close();
        } catch (IOException exception) {
            throw new ServiceException("Failed to close transaction : " + exception.getMessage());
        }
    }
}
