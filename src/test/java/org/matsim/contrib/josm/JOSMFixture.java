package org.matsim.contrib.josm;

import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Paths;
import java.text.MessageFormat;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences;
import org.openstreetmap.josm.io.OsmApi;
import org.openstreetmap.josm.tools.I18n;

/**
 *
 * COPIED and adapted from JOSM
 *
 * Fixture to define a proper and safe environment before running tests.
 */
class JOSMFixture {

    private final String josmHome;

    /**
     * Constructs a new test fixture initialized to a given josm home.
     * @param josmHome The user home where preferences are to be read/written
     */
    public JOSMFixture(String josmHome) {
        this.josmHome = josmHome;
    }

    /**
     * Initializes the test fixture, without GUI.
     */
    public void init() {
        init(false);
    }

    /**
     * Initializes the test fixture, with or without GUI.
     * @param createGui if {@code true} creates main GUI components
     */
    public void init(boolean createGui) {

        // check josm.home
        //
        if (josmHome == null) {
            fail(MessageFormat.format("property ''{0}'' not set in test environment", "josm.home"));
        } else {
            File f = new File(josmHome);
            if (! f.exists() || ! f.canRead()) {
                fail(MessageFormat.format("property ''{0}'' points to ''{1}'' which is either not existing ({2}) or not readable ({3}). Current directory is ''{4}''.",
                        "josm.home", josmHome, f.exists(), f.canRead(), Paths.get("").toAbsolutePath()));
            }
        }
        System.setProperty("josm.home", josmHome);
        Main.initApplicationPreferences();
        Main.pref.enableSaveOnPut(false);
        I18n.init();
        // initialize the plaform hook, and
        Main.determinePlatformHook();
        // call the really early hook before we anything else
        Main.platform.preStartupHook();

        Main.pref.init(false);
        Main.pref.put("osm-server.url", "http://api06.dev.openstreetmap.org/api");

        I18n.set(Main.pref.get("language", "en"));

        // init projection
        Main.setProjection(Projections.getProjectionByCode("EPSG:3857")); // Mercator

        // make sure we don't upload to or test against production
        //
        String url = OsmApi.getOsmApi().getBaseUrl().toLowerCase().trim();
        if (url.startsWith("http://www.openstreetmap.org") || url.startsWith("http://api.openstreetmap.org")
                || url.startsWith("https://www.openstreetmap.org") || url.startsWith("https://api.openstreetmap.org")) {
            fail(MessageFormat.format("configured server url ''{0}'' seems to be a productive url, aborting.", url));
        }

        if (createGui) {
            if (Main.toolbar == null) {
                Main.toolbar = new ToolbarPreferences();
            }
            if (Main.main == null) {
                new MainApplication();
            }
            if (Main.map == null) {
                Main.main.createMapFrame(null, null);
            }
        }
    }
}
