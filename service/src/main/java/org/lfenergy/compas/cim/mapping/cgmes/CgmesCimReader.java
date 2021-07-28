// SPDX-FileCopyrightText: 2021 Alliander N.V.
//
// SPDX-License-Identifier: Apache-2.0
package org.lfenergy.compas.cim.mapping.cgmes;

import com.powsybl.cgmes.conversion.Conversion;
import com.powsybl.cgmes.model.CgmesModelFactory;
import com.powsybl.commons.datasource.ReadOnlyMemDataSource;
import com.powsybl.triplestore.api.TripleStoreFactory;
import org.lfenergy.compas.cim.mapping.model.CimData;
import org.lfenergy.compas.core.commons.ElementConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Class to read the CIM Model into a Java Object Model to be used further for converting it to IEC 61850.
 */
@ApplicationScoped
public class CgmesCimReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(CgmesCimReader.class);

    private final CgmesDataValidator cgmesDataValidator;
    private final ElementConverter converter;

    @Inject
    public CgmesCimReader(CgmesDataValidator cgmesDataValidator, ElementConverter converter) {
        this.cgmesDataValidator = cgmesDataValidator;
        this.converter = converter;
    }

    /**
     * Use PowSyBl to convert a CIM XML InputStream to the PowSyBl IIDM Model.
     * Multiple InputStream Objects can be passed if needed.
     *
     * @param cimData The different InputStream Objects that combined define the CIM Model.
     * @return The IIDM Network model that can be used to convert further to IEC 61850.
     */
    public CgmesCimReaderResult readModel(List<CimData> cimData) {
        LOGGER.info("Check the data passed, PowSyBl is quite sensitive about naming.");
        cgmesDataValidator.validateData(cimData);
        var cimContents = convertCimDataToMap(cimData);

        LOGGER.debug("Create a ReadOnlyDataSource from the input data.");
        var source = new ReadOnlyMemDataSource();
        cimContents.forEach(source::putData);

        LOGGER.debug("First create a CgmesModel from the InputStream (RDF File).");
        var tripStoreImpl = TripleStoreFactory.defaultImplementation();
        var cgmesModel = CgmesModelFactory.create(source, tripStoreImpl);

        LOGGER.debug("Next create a Network Model (IIDM) from the CgmesModel.");
        var config = new Conversion.Config();
        config.setConvertSvInjections(true);
        config.setProfileUsedForInitialStateValues(Conversion.Config.StateProfile.SSH.name());
        var conversion = new Conversion(cgmesModel, config);
        var network = conversion.convert();
        return new CgmesCimReaderResult(cgmesModel, network);
    }

    Map<String, InputStream> convertCimDataToMap(List<CimData> cimData) {
        return cimData.stream().collect(
                Collectors.toMap(
                        CimData::getName,
                        cimRecord -> {
                            var xml = converter.convertToString(cimRecord.getRdf(), false);
                            return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
                        })
        );
    }
}
