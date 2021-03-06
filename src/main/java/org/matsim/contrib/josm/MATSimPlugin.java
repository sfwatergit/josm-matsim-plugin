package org.matsim.contrib.josm;

import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.osm.visitor.paint.MapRendererFactory;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.TaggingPresetMenu;
import org.openstreetmap.josm.gui.tagging.TaggingPresetReader;
import org.openstreetmap.josm.gui.tagging.TaggingPresetSeparator;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.xml.sax.SAXException;

/**
 * This is the main class for the MATSim plugin.
 * 
 * @see Plugin
 * 
 * @author Nico
 * 
 */
public class MATSimPlugin extends Plugin implements PreferenceChangedListener {

    private static Collection<WeakReference<PreferenceChangedListener>> preferenceChangeListeners = new ArrayList<>();
    private PTToggleDialog ptToggleDialog = new PTToggleDialog();
    private LinksToggleDialog linksToggleDialog = new LinksToggleDialog();

    public MATSimPlugin(PluginInformation info) {
		super(info);

		// add xml exporter for matsim data
		ExtensionFileFilter.exporters.add(0, new MATSimNetworkFileExporter());

		// add commands to tools list
        Main.main.menu.toolsMenu.add(new ImportAction());
        Main.main.menu.toolsMenu.add(new NewNetworkAction());
        Main.main.menu.toolsMenu.add(new ConvertAction());
        TransitScheduleExportAction transitScheduleExportAction = new TransitScheduleExportAction();
        Main.main.menu.toolsMenu.add(transitScheduleExportAction);
        Main.pref.addPreferenceChangeListener(transitScheduleExportAction);

		// read tagging preset
		Reader reader = new InputStreamReader(getClass().getResourceAsStream(
				"matsimPreset.xml"));
		Collection<TaggingPreset> tps;
		try {
			tps = TaggingPresetReader.readAll(reader, true);
		} catch (SAXException e) {
			throw new RuntimeException(e);
		}
		for (TaggingPreset tp : tps) {
			if (!(tp instanceof TaggingPresetSeparator)) {
				Main.toolbar.register(tp);
			}
		}
		AutoCompletionManager.cachePresets(tps);
		HashMap<TaggingPresetMenu, JMenu> submenus = new HashMap<>();
		for (final TaggingPreset p : tps) {
			JMenu m = p.group != null ? submenus.get(p.group)
					: Main.main.menu.presetsMenu;
			if (p instanceof TaggingPresetSeparator) {
				m.add(new JSeparator());
			} else if (p instanceof TaggingPresetMenu) {
				JMenu submenu = new JMenu(p);
				submenu.setText(p.getLocaleName());
				((TaggingPresetMenu) p).menu = submenu;
				submenus.put((TaggingPresetMenu) p, submenu);
				m.add(submenu);
			} else {
				JMenuItem mi = new JMenuItem(p);
				mi.setText(p.getLocaleName());
				m.add(mi);
			}
		}

		// register map renderer
		if (Main.pref.getBoolean("matsim_renderer", false)) {
			MapRendererFactory factory = MapRendererFactory.getInstance();
			factory.register(MapRenderer.class, "MATSim Renderer",
					"This is the MATSim map renderer");
			factory.activate(MapRenderer.class);
		}

		// register for preference changed events
		Main.pref.addPreferenceChangeListener(this);
		Main.pref.addPreferenceChangeListener(MapRenderer.Properties.getInstance());

		// load default converting parameters
		OsmConvertDefaults.load();
	}

    static void addPreferenceChangedListener(PreferenceChangedListener listener) {
        preferenceChangeListeners.add(new WeakReference<>(listener));
    }

	/**
	 * Called when the JOSM map frame is created or destroyed.
	 * 
	 * @param oldFrame
	 *            The old MapFrame. Null if a new one is created.
	 * @param newFrame
	 *            The new MapFrame. Null if the current is destroyed.
	 */
	@Override
	public void mapFrameInitialized(MapFrame oldFrame, MapFrame newFrame) {
		if (oldFrame == null && newFrame != null) { // map frame added
            Main.map.addToggleDialog(linksToggleDialog);
            Main.map.addToggleDialog(ptToggleDialog);
		}
	}

	@Override
	public PreferenceSetting getPreferenceSetting() {
		return new Preferences.Factory().createPreferenceSetting();
	}

	@Override
	public void preferenceChanged(PreferenceChangeEvent e) {
		if (e.getKey().equalsIgnoreCase("matsim_renderer")) {
			MapRendererFactory factory = MapRendererFactory.getInstance();
			if (Main.pref.getBoolean("matsim_renderer")) {
				factory.register(MapRenderer.class, "MATSim Renderer",
						"This is the MATSim map renderer");
				factory.activate(MapRenderer.class);
			} else {
				factory.activateDefault();
				factory.unregister(MapRenderer.class);
			}
		} else if (e.getKey().equalsIgnoreCase("matsim_supportTransit")) {
            boolean supportTransit = Main.pref.getBoolean("matsim_supportTransit");
            ptToggleDialog.setEnabled(supportTransit);
            if (!supportTransit) {
                ptToggleDialog.hideDialog();
            }
        }
        Iterator<WeakReference<PreferenceChangedListener>> iterator = preferenceChangeListeners.iterator();
        while (iterator.hasNext()) {
            WeakReference<PreferenceChangedListener> next = iterator.next();
            PreferenceChangedListener preferenceChangedListener = next.get();
            if (preferenceChangedListener != null) {
                preferenceChangedListener.preferenceChanged(e);
            } else {
                iterator.remove();
            }
        }
    }
}
