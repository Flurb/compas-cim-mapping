// SPDX-FileCopyrightText: 2021 Alliander N.V.
//
// SPDX-License-Identifier: Apache-2.0
package org.lfenergy.compas.cim.mapping.mapper;

import com.powsybl.cgmes.model.CgmesModel;
import com.powsybl.triplestore.api.PropertyBags;
import org.lfenergy.compas.cim.mapping.model.*;
import org.lfenergy.compas.scl2007b4.model.TConnectivityNode;
import org.lfenergy.compas.scl2007b4.model.TNaming;

import java.util.*;
import java.util.stream.Collectors;

public class CimToSclMapperContext {
    public static final String SUBSTATION_PROP = "Substation";
    public static final String VOLTAGE_LEVEL_PROP = "VoltageLevel";
    public static final String BUSBARSECTION_PROP = "BusbarSection";
    public static final String BAY_PROP = "Bay";
    public static final String POWER_TRANSFORMER_PROP = "PowerTransformer";
    public static final String TRANSFORMER_END_PROP = "TransformerEnd";
    public static final String RATIO_TAP_CHANGER_PROP = "RatioTapChanger";
    public static final String PHASE_TAP_CHANGER_PROP = "PhaseTapChanger";
    public static final String SWITCH_PROP = "Switch";
    public static final String TERMINAL_PROP = "Terminal";
    public static final String CONNECTIVITY_NODE_PROP = "ConnectivityNode";
    public static final String CONDUCTING_EQUIPMENT_PROP = "ConductingEquipment";
    public static final String EQUIPMENT_CONTAINER_PROP = "EquipmentContainer";
    public static final String NAME_PROP = "name";
    public static final String DESCRIPTION_PROP = "description";
    public static final String NOMINAL_VOLTAGE_PROP = "nominalVoltage";
    public static final String TYPE_PROP = "type";
    public static final String ENDNUMBER_PROP = "endNumber";
    public static final String TERMINAL_1_PROP = "Terminal1";
    public static final String TERMINAL_2_PROP = "Terminal2";

    private static final String START_QUERY = "SELECT *\nWHERE {\n GRAPH ?graph {\n";
    private static final String END_QUERY = "}}\n";

    private final CgmesModel cgmesModel;

    public CimToSclMapperContext(CgmesModel cgmesModel) {
        this.cgmesModel = cgmesModel;
    }

    /**
     * Search the CGMES Model for all Substations.
     *
     * @return The List of converted CGMES Substations that were found.
     */
    public List<CgmesSubstation> getSubstations() {
        return cgmesModel.substations()
                .stream()
                .map(propertyBag -> new CgmesSubstation(
                        propertyBag.getId(SUBSTATION_PROP),
                        propertyBag.get(NAME_PROP)))
                .collect(Collectors.toList());
    }

    /**
     * Search the CGMES Model for VoltageLevels that are coupled to a specific substation.
     *
     * @param substationId The ID of the Substation.
     * @return The List of converted CGMES VoltageLevels that were found.
     */
    public List<CgmesVoltageLevel> getVoltageLevelsBySubstation(String substationId) {
        return cgmesModel.voltageLevels()
                .stream()
                .filter(propertyBag -> substationId.equals(propertyBag.getId(SUBSTATION_PROP)))
                .map(propertyBag -> new CgmesVoltageLevel(
                        propertyBag.getId(VOLTAGE_LEVEL_PROP),
                        propertyBag.get(NAME_PROP),
                        propertyBag.asDouble(NOMINAL_VOLTAGE_PROP)))
                .collect(Collectors.toList());
    }

    /**
     * Search for busbarsections that are coupled to a specific Equipment Container.
     *
     * @param containerId The ID of the Equipment Container to filter on.
     * @return The list of converted CGMES BusbarSections that were found.
     */
    public List<CgmesBusbarSection> getBusbarSectionsByEquipmentContainer(String containerId) {
        return cgmesModel.tripleStore().query(
                        START_QUERY +
                                "    ?BusbarSection\n" +
                                "        a cim:BusbarSection ;\n" +
                                "        cim:Equipment.EquipmentContainer ?EquipmentContainer .\n" +
                                "        OPTIONAL { ?BusbarSection cim:IdentifiedObject.name ?name }\n" +
                                " FILTER (str(?EquipmentContainer) = \"http://default-cgmes-model/#" + containerId + "\") " +
                                END_QUERY).stream()
                .map(bag -> new CgmesBusbarSection(bag.getId(BUSBARSECTION_PROP), bag.get(NAME_PROP)))
                .collect(Collectors.toList());
    }

    /**
     * Search for bays that are coupled to a specific Voltage Level.
     *
     * @param voltageLevelId The ID of the Voltage Level to filter on.
     * @return The list of converted CGMES Bays that were found.
     */
    public List<CgmesBay> getBaysByVoltageLevel(String voltageLevelId) {
        return cgmesModel.tripleStore().query(
                        START_QUERY +
                                "    ?Bay\n" +
                                "        a cim:Bay ;\n" +
                                "        cim:Bay.VoltageLevel ?VoltageLevel .\n" +
                                "        OPTIONAL { ?Bay cim:IdentifiedObject.name ?name }\n" +
                                " FILTER (str(?VoltageLevel) = \"http://default-cgmes-model/#" + voltageLevelId + "\") " +
                                END_QUERY).stream()
                .map(bag -> new CgmesBay(bag.getId(BAY_PROP), bag.get(NAME_PROP)))
                .collect(Collectors.toList());
    }

    /**
     * Search the CGMES Model for Power-Transformers that are coupled to a specific container.
     *
     * @param containerId The ID of the Container.
     * @return The List of converted CGMES Power-Transformers that were found.
     */
    public List<CgmesTransformer> getTransformers(String containerId) {
        return cgmesModel.tripleStore().query(
                        START_QUERY +
                                "    ?PowerTransformer\n" +
                                "        a cim:PowerTransformer ;\n" +
                                "        cim:Equipment.EquipmentContainer ?EquipmentContainer .\n" +
                                "        OPTIONAL { ?PowerTransformer cim:IdentifiedObject.name ?name }\n" +
                                "        OPTIONAL { ?PowerTransformer cim:IdentifiedObject.description ?description } \n" +
                                " FILTER (str(?EquipmentContainer) = \"http://default-cgmes-model/#" + containerId + "\") " +
                                END_QUERY)
                .stream()
                .map(propertyBag -> new CgmesTransformer(
                        propertyBag.getId(POWER_TRANSFORMER_PROP),
                        propertyBag.get(NAME_PROP),
                        propertyBag.get(DESCRIPTION_PROP)))
                .collect(Collectors.toList());
    }

    /**
     * Search the CGMES Model for Power-Transformer Ends that are coupled to a specific Power-Transformer.
     *
     * @param powerTransformerId The ID of the Power-Transformer.
     * @return The List of converted CGMES Power-Transformer Ends that were found.
     */
    public List<CgmesTransformerEnd> getTransformerEnds(String powerTransformerId) {
        return cgmesModel.transformerEnds()
                .stream()
                .filter(propertyBag -> powerTransformerId.equals(propertyBag.getId(POWER_TRANSFORMER_PROP)))
                .map(propertyBag -> new CgmesTransformerEnd(
                        propertyBag.getId(TRANSFORMER_END_PROP),
                        propertyBag.get(NAME_PROP),
                        propertyBag.getId(TERMINAL_PROP),
                        propertyBag.get(ENDNUMBER_PROP)))
                .collect(Collectors.toList());
    }

    /**
     * Search the CGMES Model for a RatioTapChanger or PhaseTapChanger that are coupled to a Power-Transformer End.
     *
     * @param powerTransformerEndId The ID of the Power-Transformer End.
     * @return The converted CGMEs TapChanger found, or Empty Optional if non.
     */
    public Optional<CgmesTapChanger> getTapChanger(String powerTransformerEndId) {
        // Convert all the RatioTapChangers from CIM.
        var tapChanger = getTapChanger(cgmesModel.ratioTapChangers(), powerTransformerEndId, RATIO_TAP_CHANGER_PROP);
        if (!tapChanger.isPresent()) {
            // Convert all the PhaseTapChangers from CIM.
            tapChanger = getTapChanger(cgmesModel.phaseTapChangers(), powerTransformerEndId, PHASE_TAP_CHANGER_PROP);
        }
        return tapChanger;
    }

    private Optional<CgmesTapChanger> getTapChanger(PropertyBags tapChangers, String powerTransformerEndId, String idName) {
        return tapChangers
                .stream()
                .filter(propertyBag -> powerTransformerEndId.equals(propertyBag.getId(TRANSFORMER_END_PROP)))
                .map(propertyBag -> new CgmesTapChanger(
                        propertyBag.getId(idName),
                        propertyBag.get(NAME_PROP)))
                .findFirst();
    }

    /**
     * Search the CGMES Model for Connectivity Nodes that are coupled to a BusbarSection.
     *
     * @param busbarSectionId The ID of the BusbarSection.
     * @return The List of converted CGMES Connectivity Nodes that were found.
     */
    public List<CgmesConnectivityNode> getConnectivityNodeByBusbarSection(String busbarSectionId) {
        return cgmesModel.tripleStore().query(
                        START_QUERY +
                                "  ?ConnectivityNode cim:ConnectivityNode.ConnectivityNodeContainer ?ConnectivityNodeContainer \n" +
                                "      OPTIONAL { ?ConnectivityNode cim:IdentifiedObject.name ?name } \n" +
                                "          ?Terminal cim:Terminal.ConnectivityNode ?ConnectivityNode; \n" +
                                "          cim:Terminal.ConductingEquipment ?ConductingEquipment; \n" +
                                "          FILTER (str(?ConductingEquipment) = \"http://default-cgmes-model/#" + busbarSectionId + "\") \n" +
                                END_QUERY)
                .stream()
                .map(propertyBag -> new CgmesConnectivityNode(
                        propertyBag.getId(CONNECTIVITY_NODE_PROP),
                        propertyBag.get(NAME_PROP)))
                .collect(Collectors.toList());
    }

    /**
     * Search the CGMES Model for Connectivity Nodes that are coupled to a specific container.
     *
     * @param containerId The ID of the Container.
     * @return The List of converted CGMES Connectivity Nodes that were found.
     */
    public List<CgmesConnectivityNode> getConnectivityNodeByBay(String containerId) {
        return cgmesModel.tripleStore().query(
                        "SELECT DISTINCT ?ConnectivityNode ?name \n" +
                                "WHERE {\n" +
                                " GRAPH ?graph {\n" +
                                "  ?ConnectivityNode a cim:ConnectivityNode . \n" +
                                "  OPTIONAL { ?ConnectivityNode cim:IdentifiedObject.name ?name } \n" +
                                "    ?Terminal a cim:Terminal ;\n" +
                                "    cim:Terminal.ConnectivityNode ?ConnectivityNode ; \n" +
                                "    cim:Terminal.ConductingEquipment ?Switch . \n" +
                                "      ?Switch a ?type ; \n" +
                                "      cim:Equipment.EquipmentContainer ?EquipmentContainer . \n" +
                                "      VALUES ?type { cim:Switch cim:Breaker cim:Disconnector cim:LoadBreakSwitch cim:ProtectedSwitch } . \n" +
                                "  FILTER (str(?EquipmentContainer) = \"http://default-cgmes-model/#" + containerId + "\") \n" +
                                END_QUERY +
                                "ORDER BY ?name ")
                .stream()
                .map(propertyBag -> new CgmesConnectivityNode(
                        propertyBag.getId(CONNECTIVITY_NODE_PROP),
                        propertyBag.get(NAME_PROP)))
                .collect(Collectors.toList());
    }

    /**
     * Search the CGMES Model for Switches (Breakers, Disconnector and more) that are coupled to a specific container.
     *
     * @param containerId The ID of the Container.
     * @return The List of converted CGMES Switches that were found.
     */
    public List<CgmesSwitch> getSwitches(String containerId) {
        return cgmesModel.switches()
                .stream()
                .filter(propertyBag -> containerId.equals(propertyBag.getId(EQUIPMENT_CONTAINER_PROP)))
                .map(propertyBag -> new CgmesSwitch(
                        propertyBag.getId(SWITCH_PROP),
                        propertyBag.get(NAME_PROP),
                        propertyBag.getLocal(TYPE_PROP),
                        propertyBag.getId(TERMINAL_1_PROP),
                        propertyBag.getId(TERMINAL_2_PROP)))
                .collect(Collectors.toList());
    }

    /**
     * Search the CGMES Model for Terminals that are coupled to a specific Conducting Equipment.
     *
     * @param conductingEquipmentId The ID of the Conducting Equipment.
     * @return The List of converted CGMES Terminals that were found.
     */
    public List<CgmesTerminal> getTerminals(String conductingEquipmentId) {
        return cgmesModel.terminals()
                .stream()
                .filter(propertyBag -> conductingEquipmentId.equals(propertyBag.getId(CONDUCTING_EQUIPMENT_PROP)))
                .map(propertyBag -> new CgmesTerminal(
                        propertyBag.getId(TERMINAL_PROP),
                        propertyBag.get(NAME_PROP),
                        propertyBag.getId(CONNECTIVITY_NODE_PROP)))
                .collect(Collectors.toList());
    }

    /**
     * Search the CGMES Model for a Terminal with a specific ID.
     *
     * @param terminalId The ID of the Terminal.
     * @return The converted CGMES Terminal that is found.
     */
    public Optional<CgmesTerminal> getTerminal(String terminalId) {
        return cgmesModel.terminals()
                .stream()
                .filter(propertyBag -> terminalId.equals(propertyBag.getId(TERMINAL_PROP)))
                .map(propertyBag -> new CgmesTerminal(
                        propertyBag.getId(TERMINAL_PROP),
                        propertyBag.get(NAME_PROP),
                        propertyBag.getId(CONNECTIVITY_NODE_PROP)))
                .findFirst();
    }

    /*
     * Below part contains methods to keep track of all the naming elements passed.
     * At the end this list is used to create a PathName for the ConnectivityNode.
     */
    // List holding all passed TNaming Elements
    private LinkedList<TNaming> namingLevels = new LinkedList<>();

    /**
     * Adds the parameter to the stack. Called on the way down (BeforeMapping).
     *
     * @param tNaming The naming element to add.
     */
    public void addLast(TNaming tNaming) {
        namingLevels.addLast(tNaming);
    }

    /**
     * Remove the last added element from the stack. Called on the way back (AfterMapping).
     */
    public TNaming removeLast() {
        return namingLevels.removeLast();
    }

    /**
     * Uses the Stack to create a PathName for the ConnectivityNode. All names are joined separated by a slash.
     *
     * @return The created PathName from the Stack.
     */
    public String createPathName() {
        return namingLevels.stream()
                .map(TNaming::getName)
                .collect(Collectors.joining("/"));
    }

    /*
     * Because there needs to be a link between the terminal and the Connectivity Node, we need to store
     * the TConnectivityNode with his ID. The ID is not copied to the IEC SCL, so this way the link can be
     * made between those two elements.
     */
    // Map with Connectivity Nodes with the ID as Key.
    private Map<String, TConnectivityNode> connectivityNodeIdMap = new HashMap<>();

    public void resetTConnectivityNodeMap() {
        connectivityNodeIdMap = new HashMap<>();
    }

    public void saveTConnectivityNode(String id, TConnectivityNode tConnectivityNode) {
        connectivityNodeIdMap.put(id, tConnectivityNode);
    }

    public boolean containsTConnectivityNode(String id) {
        return connectivityNodeIdMap.containsKey(id);
    }

    public Optional<String> getPathnameFromConnectivityNode(String id) {
        var tConnectivityNode = connectivityNodeIdMap.get(id);
        if (tConnectivityNode != null) {
            return Optional.ofNullable(tConnectivityNode.getPathName());
        }
        return Optional.empty();
    }

    public Optional<String> getNameFromConnectivityNode(String id) {
        var tConnectivityNode = connectivityNodeIdMap.get(id);
        if (tConnectivityNode != null) {
            return Optional.ofNullable(tConnectivityNode.getName());
        }
        return Optional.empty();
    }
}
