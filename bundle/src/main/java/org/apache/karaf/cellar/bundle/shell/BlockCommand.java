/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.cellar.bundle.shell;

import org.apache.karaf.cellar.bundle.Constants;
import org.apache.karaf.cellar.core.CellarSupport;
import org.apache.karaf.cellar.core.Configurations;
import org.apache.karaf.cellar.core.Group;
import org.apache.karaf.cellar.core.event.EventType;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;

import java.util.*;

@Command(scope = "cluster", name = "bundle-block", description = "Change the blocking policy for a bundle")
public class BlockCommand extends BundleCommandSupport {

    @Option(name = "-in", description = "Update the inbound direction", required = false, multiValued = false)
    boolean in = false;

    @Option(name = "-out", description = "Update the outbound direction", required = false, multiValued = false)
    boolean out = false;

    @Option(name = "-whitelist", description = "Allow the feature by updating the whitelist (false by default)", required = false, multiValued = false)
    boolean whitelist = false;

    @Option(name = "-blacklist", description = "Block the feature by updating the blacklist (true by default)", required = false, multiValued = false)
    boolean blacklist = false;

    public Object doExecute() throws Exception {

        Group group = groupManager.findGroupByName(groupName);
        if (group == null) {
            System.err.println("Cluster group " + groupName + " doesn't exist");
            return null;
        }

        List<String> patterns = new ArrayList<String>();

        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());

        try {
            Map<String, ExtendedBundleState> bundles = gatherBundles();
            List<String> selectedBundles = selector(bundles);
            for (String selectedBundle : selectedBundles) {
                patterns.add(bundles.get(selectedBundle).getLocation());
            }

            if (patterns.isEmpty() && ids != null) {
                for (String id : ids) {
                    patterns.add(id);
                }
            }

            CellarSupport support = new CellarSupport();
            support.setClusterManager(clusterManager);
            support.setGroupManager(groupManager);
            support.setConfigurationAdmin(configurationAdmin);

            if (!in && !out) {
                in = true;
                out = true;
            }
            if (!whitelist && !blacklist) {
                whitelist = true;
                blacklist = true;
            }

            if (patterns.isEmpty()) {
                // display mode
                if (in) {
                    System.out.println("INBOUND:");
                    if (whitelist) {
                        System.out.print("\twhitelist: ");
                        Set<String> list = support.getListEntries(Configurations.WHITELIST, groupName, Constants.CATEGORY, EventType.INBOUND);
                        System.out.println(list.toString());
                    }
                    if (blacklist) {
                        System.out.print("\tblacklist: ");
                        Set<String> list = support.getListEntries(Configurations.BLACKLIST, groupName, Constants.CATEGORY, EventType.INBOUND);
                        System.out.println(list.toString());
                    }
                }
                if (out) {
                    System.out.println("OUTBOUND:");
                    if (whitelist) {
                        System.out.print("\twhitelist: ");
                        Set<String> list = support.getListEntries(Configurations.WHITELIST, groupName, Constants.CATEGORY, EventType.OUTBOUND);
                        System.out.println(list.toString());
                    }
                    if (blacklist) {
                        System.out.print("\tblacklist: ");
                        Set<String> list = support.getListEntries(Configurations.BLACKLIST, groupName, Constants.CATEGORY, EventType.OUTBOUND);
                        System.out.println(list.toString());
                    }
                }
            } else {
                // edit mode
                for (String pattern : patterns) {
                    System.out.println("Updating blocking policy for " + pattern);
                    if (in) {
                        if (whitelist) {
                            System.out.println("\tinbound whitelist ...");
                            support.switchListEntry(Configurations.WHITELIST, groupName, Constants.CATEGORY, EventType.INBOUND, pattern);
                        }
                        if (blacklist) {
                            System.out.println("\tinbound blacklist ...");
                            support.switchListEntry(Configurations.BLACKLIST, groupName, Constants.CATEGORY, EventType.INBOUND, pattern);
                        }
                    }
                    if (out) {
                        if (whitelist) {
                            System.out.println("\toutbound whitelist ...");
                            support.switchListEntry(Configurations.WHITELIST, groupName, Constants.CATEGORY, EventType.OUTBOUND, pattern);
                        }
                        if (blacklist) {
                            System.out.println("\toutbound blacklist ...");
                            support.switchListEntry(Configurations.BLACKLIST, groupName, Constants.CATEGORY, EventType.OUTBOUND, pattern);
                        }
                    }
                }
            }
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        return null;
    }

}
