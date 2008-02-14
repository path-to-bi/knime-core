/* 
 * ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 * 
 * History
 *   18.06.2007 (gabriel): created
 */
package org.knime.base.node.preproc.rowsplit;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * 
 * @author Thomas Gabriel, University of Konstanz
 */
public class NumericRowSplitterNodeModel extends NodeModel {
    
    private final SettingsModelString m_columnSelection =
        NumericRowSplitterNodeDialogPane.createColumnSelectionModel();
    
    private final SettingsModelBoolean m_lowerBoundCheck = 
        NumericRowSplitterNodeDialogPane.createLowerBoundCheckBoxModel();
    private final SettingsModelDouble m_lowerBoundValue = 
        NumericRowSplitterNodeDialogPane.createLowerBoundTextfieldModel();
    private final SettingsModelString m_lowerBound = 
        NumericRowSplitterNodeDialogPane.createLowerBoundModel();
    
    private final SettingsModelBoolean m_upperBoundCheck = 
        NumericRowSplitterNodeDialogPane.createUpperBoundCheckBoxModel();
    private final SettingsModelDouble m_upperBoundValue = 
        NumericRowSplitterNodeDialogPane.createUpperBoundTextfieldModel();
    private final SettingsModelString m_upperBound = 
        NumericRowSplitterNodeDialogPane.createUpperBoundModel();

    /**
     *
     */
    public NumericRowSplitterNodeModel() {
        super(1, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
        if (!inSpecs[0].containsName(m_columnSelection.getStringValue())) {
            throw new InvalidSettingsException("Column '" 
                    + m_columnSelection.getStringValue());
        }
        DataTableSpec outSpec = inSpecs[0];
        return new DataTableSpec[]{outSpec, outSpec};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
            final ExecutionContext exec) throws Exception {
        DataTableSpec inSpec = inData[0].getDataTableSpec();
        BufferedDataContainer buf1 = exec.createDataContainer(inSpec);
        BufferedDataContainer buf2 = exec.createDataContainer(inSpec);
        final int columnIndex = 
            inSpec.findColumnIndex(m_columnSelection.getStringValue()); 
        int count = 0;
        final int nrRows = inData[0].getRowCount();
        for (DataRow row : inData[0]) {
            if (matches(row.getCell(columnIndex))) {
                buf1.addRowToTable(row);
            } else {
                buf2.addRowToTable(row);
            }
            exec.checkCanceled();
            exec.setProgress(count / (double) nrRows, 
                    "Added row " + (++count) + "/" + nrRows + " (\"" 
                    + row.getKey() + "\")");
        }
        buf1.close();
        buf2.close();
        BufferedDataTable outData1 = buf1.getTable();
        BufferedDataTable outData2 = buf2.getTable();
        return new BufferedDataTable[]{outData1, outData2};
    }
    
    private boolean matches(final DataCell cell) {
        if (cell.isMissing()) {
            return false;
        }
        double value = ((DoubleValue) cell).getDoubleValue();
        if (m_lowerBoundCheck.getBooleanValue()) {
            double lower = m_lowerBoundValue.getDoubleValue();
            if (value < lower) {
                return false;
            }
            boolean inclLower = NumericRowSplitterNodeDialogPane.
                includeLowerBound(m_lowerBound);
            if ((lower == value) && !inclLower) {
                return false;
            }
        }
        if (m_upperBoundCheck.getBooleanValue()) {
            double upper = m_upperBoundValue.getDoubleValue();
            if (value > upper) {
                return false;
            }
            boolean inclUpper = NumericRowSplitterNodeDialogPane.
                includeUpperBound(m_upperBound);
            if ((upper == value) && !inclUpper) {
                return false;
            }
        }   
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columnSelection.loadSettingsFrom(settings);
        m_lowerBound.loadSettingsFrom(settings);
        m_lowerBoundCheck.loadSettingsFrom(settings);
        m_lowerBoundValue.loadSettingsFrom(settings);
        m_upperBound.loadSettingsFrom(settings);
        m_upperBoundCheck.loadSettingsFrom(settings);
        m_upperBoundValue.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, 
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_columnSelection.saveSettingsTo(settings);
        m_lowerBound.saveSettingsTo(settings);
        m_lowerBoundCheck.saveSettingsTo(settings);
        m_lowerBoundValue.saveSettingsTo(settings);
        m_upperBound.saveSettingsTo(settings);
        m_upperBoundCheck.saveSettingsTo(settings);
        m_upperBoundValue.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columnSelection.validateSettings(settings);
        m_lowerBound.validateSettings(settings);
        m_lowerBoundCheck.validateSettings(settings);
        m_lowerBoundValue.validateSettings(settings);
        m_upperBound.validateSettings(settings);
        m_upperBoundCheck.validateSettings(settings);
        m_upperBoundValue.validateSettings(settings);
        
        SettingsModelBoolean lowerBoundCheck = 
            m_lowerBoundCheck.createCloneWithValidatedValue(settings);
        SettingsModelBoolean upperBoundCheck = 
            m_upperBoundCheck.createCloneWithValidatedValue(settings);
        if (lowerBoundCheck.getBooleanValue() 
                && upperBoundCheck.getBooleanValue()) {
            SettingsModelDouble lowerBoundValue =
                m_lowerBoundValue.createCloneWithValidatedValue(settings);
            double low = lowerBoundValue.getDoubleValue();
            SettingsModelDouble upperBoundValue =
                m_upperBoundValue.createCloneWithValidatedValue(settings);
            double upp = upperBoundValue.getDoubleValue();
            if (low > upp) {
                throw new InvalidSettingsException("Check lower and upper "
                        + "bound values: " + low + " > " + upp);
            }
            if (low == upp) {
                SettingsModelString lowerBound = 
                    m_lowerBound.createCloneWithValidatedValue(settings);
                boolean inclLow = NumericRowSplitterNodeDialogPane.
                        includeLowerBound(lowerBound);
                SettingsModelString upperBound = 
                    m_upperBound.createCloneWithValidatedValue(settings);
                boolean inclUpp = NumericRowSplitterNodeDialogPane.
                        includeUpperBound(upperBound);
                if ((inclLow ^ inclUpp) || (!inclLow & !inclUpp)) {
                    throw new InvalidSettingsException("Lower and upper bounds "
                            + "are inconsistent with borders!");
                }
            }
        }
    }

}
