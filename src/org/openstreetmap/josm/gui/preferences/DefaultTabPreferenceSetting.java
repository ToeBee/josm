// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.openstreetmap.josm.tools.GBC;

public abstract class DefaultTabPreferenceSetting extends DefaultPreferenceSetting implements TabPreferenceSetting {

    private final String iconName;
    private final String description;
    private final String title;
    private final JTabbedPane tabpane;
    private final Map<SubPreferenceSetting, Component> subSettingMap;
    
    public DefaultTabPreferenceSetting() {
        this(null, null, null);
    }

    public DefaultTabPreferenceSetting(String iconName, String title, String description) {
        this(iconName, title, description, false);
    }

    public DefaultTabPreferenceSetting(String iconName, String title, String description, boolean isExpert) {
        this(iconName, title, description, isExpert, null);
    }

    public DefaultTabPreferenceSetting(String iconName, String title, String description, boolean isExpert, JTabbedPane tabpane) {
        super(isExpert);
        this.iconName = iconName;
        this.description = description;
        this.title = title;
        this.tabpane = tabpane;
        this.subSettingMap = tabpane != null ? new HashMap<SubPreferenceSetting, Component>() : null;
    }

    @Override
    public String getIconName() {
        return iconName;
    }

    @Override
    public String getTooltip() {
        if (getDescription() != null) {
            return "<html>"+getDescription()+"</html>";
        } else {
            return null;
        }
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getTitle() {
        return title;
    }

    /**
     * Get the inner tab pane, if any.
     * @return The JTabbedPane contained in this tab preference settings, or null if none is set.
     * @since 5631
     */
    public final JTabbedPane getTabPane() {
        return tabpane;
    }
    
    protected final void createPreferenceTabWithScrollPane(PreferenceTabbedPane gui, JPanel panel) {
        GBC a = GBC.eol().insets(-5,0,0,0);
        a.anchor = GBC.EAST;
        
        JScrollPane scrollPane = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);

        JPanel tab = gui.createPreferenceTab(this);
        tab.add(scrollPane, GBC.eol().fill(GBC.BOTH));
        tab.add(GBC.glue(0,10), a);
    }

    @Override
    public boolean selectSubTab(SubPreferenceSetting subPref) {
        if (tabpane != null && subPref != null) {
            Component tab = getSubTab(subPref);
            if (tab != null) {
                try {
                    tabpane.setSelectedComponent(tab);
                    return true;
                } catch (IllegalArgumentException e) {
                    // Ignore exception and return false below
                    e.printStackTrace();
                    System.out.println(tabpane.getTabCount());
                    for (int i = 0; i < tabpane.getTabCount() ; i++) {
                        System.out.println(tabpane.getComponentAt(i));
                    }
                }
            }
        }
        return false;
    }
    
    @Override
    public final void addSubTab(SubPreferenceSetting sub, String title, Component component) {
        addSubTab(sub, title, component, null);
    }
    
    @Override
    public final void addSubTab(SubPreferenceSetting sub, String title, Component component, String tip) {
        if (tabpane != null && component != null) {
            tabpane.addTab(title, null, component, tip);
            registerSubTab(sub, component);
        }
    }
    
    @Override
    public final void registerSubTab(SubPreferenceSetting sub, Component component) {
        if (subSettingMap != null && sub != null && component != null) {
            subSettingMap.put(sub, component);
        }
    }
    
    @Override
    public final Component getSubTab(SubPreferenceSetting sub) {
        return subSettingMap != null ? subSettingMap.get(sub) : null;
    }
}
