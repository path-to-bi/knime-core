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
 * -------------------------------------------------------------------
 * 
 * History
 *   18.07.2005 (ohl): created
 */
package org.knime.base.node.preproc.filter.row;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Vector;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.base.node.preproc.filter.row.rowfilter.ColValRowFilter;
import org.knime.base.node.preproc.filter.row.rowfilter.RowFilter;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnSelectionComboxBox;

/**
 * 
 * @author Peter Ohl, University of Konstanz
 */
public class ColumnRowFilterPanel extends RowFilterPanel {

    /** object version for serialization. */
    static final long serialVersionUID = 1;

    private ColumnSelectionComboxBox m_colCombo;

    private JRadioButton m_useRange;

    private JRadioButton m_useRegExpr;

    private ButtonGroup m_radios;

    private JLabel m_lowerLabel;

    private JTextField m_lowerBound;

    private JLabel m_upperLabel;

    private JTextField m_upperBound;

    private JLabel m_regLabel;

    private JTextField m_regExpr;

    private JCheckBox m_caseSensitive;

    private JLabel m_errText;

    private DataTableSpec m_tSpec;

    private JRadioButton m_useMissValue;

    /**
     * Craetes a new panel for column content filter settings.
     * 
     * @param tSpec table spec containing column specs to select from
     * @throws NotConfigurableException it tspec is <code>null</code> or emtpy
     */
    public ColumnRowFilterPanel(final DataTableSpec tSpec)
            throws NotConfigurableException {

        super(400, 350);

        if ((tSpec == null) || (tSpec.getNumColumns() <= 0)) {
            throw new IllegalArgumentException("Don't instantiate with "
                    + "useless table spec");
        }
        m_tSpec = tSpec;

        instantiateComponents(m_tSpec);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "Column value matching"));

        /* stuff for column selection */
        panel.add(Box.createVerticalStrut(10));
        Box textBox = Box.createHorizontalBox();
        textBox.add(new JLabel("select the column to test:"));
        textBox.add(Box.createHorizontalGlue());
        panel.add(textBox);
        Box idxBox = Box.createHorizontalBox();
        idxBox.add(Box.createHorizontalGlue());
        idxBox.add(m_colCombo);
        idxBox.add(Box.createHorizontalGlue());
        panel.add(idxBox);

        /* the panel for range/regExpr matching */
        JPanel matchPanel = new JPanel();
        matchPanel.setLayout(new BoxLayout(matchPanel, BoxLayout.Y_AXIS));
        matchPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory
                .createEtchedBorder(), "matching criteria:"));
        Box regBox = Box.createHorizontalBox(); // regExpr radio
        regBox.add(m_useRegExpr);
        regBox.add(Box.createHorizontalGlue());
        matchPanel.add(regBox);
        Box exprBox = Box.createHorizontalBox(); // reg expr edit field
        exprBox.add(Box.createHorizontalGlue());
        exprBox.add(m_regLabel);
        exprBox.add(Box.createHorizontalStrut(3));
        exprBox.add(m_regExpr);
        m_regExpr.setPreferredSize(new Dimension(100, 20));
        m_regExpr.setMaximumSize(new Dimension(100, 20));
        m_regExpr.setPreferredSize(new Dimension(100, 20));
        matchPanel.add(exprBox);
        Box caseBox = Box.createHorizontalBox(); // case sensitive checkbox
        caseBox.add(Box.createHorizontalGlue());
        caseBox.add(m_caseSensitive);
        matchPanel.add(caseBox);
        Box rrBox = Box.createHorizontalBox(); // range radio
        rrBox.add(m_useRange);
        rrBox.add(Box.createHorizontalGlue());
        matchPanel.add(rrBox);
        Box lbBox = Box.createHorizontalBox(); // lower bound
        lbBox.add(Box.createHorizontalGlue());
        lbBox.add(m_lowerLabel);
        lbBox.add(Box.createHorizontalStrut(3));
        lbBox.add(m_lowerBound);
        m_lowerBound.setPreferredSize(new Dimension(75, 20));
        m_lowerBound.setMaximumSize(new Dimension(75, 20));
        m_lowerBound.setPreferredSize(new Dimension(75, 20));
        matchPanel.add(lbBox);
        Box ubBox = Box.createHorizontalBox(); // upper bound
        ubBox.add(Box.createHorizontalGlue());
        ubBox.add(m_upperLabel);
        ubBox.add(Box.createHorizontalStrut(3));
        ubBox.add(m_upperBound);
        m_upperBound.setPreferredSize(new Dimension(75, 20));
        m_upperBound.setMaximumSize(new Dimension(75, 20));
        m_upperBound.setPreferredSize(new Dimension(75, 20));
        matchPanel.add(ubBox);
        Box mvBox = Box.createHorizontalBox(); // missing value matching
        mvBox.add(m_useMissValue);
        mvBox.add(Box.createHorizontalGlue());
        matchPanel.add(mvBox);
        
        panel.add(Box.createVerticalStrut(7));
        panel.add(matchPanel);

        /* error display */
        Box errBox = Box.createHorizontalBox();
        Box errLblBox = Box.createVerticalBox();
        errLblBox.add(m_errText);
        m_errText.setMaximumSize(new Dimension(350, 30));
        m_errText.setMinimumSize(new Dimension(350, 30));
        m_errText.setPreferredSize(new Dimension(350, 30));
        errBox.add(Box.createHorizontalGlue());
        errBox.add(errLblBox);
        errBox.add(Box.createHorizontalGlue());
        // errBox.add(Box.createHorizontalGlue());
        panel.add(Box.createHorizontalStrut(300));
        panel.add(errBox);
        panel.add(Box.createVerticalStrut(7));

        panel.add(Box.createVerticalGlue()); // do we need some glue here?!?
        panel.invalidate();
        this.add(panel);
    }

    @SuppressWarnings("unchecked")
    private void instantiateComponents(final DataTableSpec tSpec)
            throws NotConfigurableException {

        /* instantiate the col idx selector, depending on the table spec */
        assert ((tSpec != null) && (tSpec.getNumColumns() > 0));

        Vector<String> colNames = new Vector<String>();
        for (int c = 0; c < tSpec.getNumColumns(); c++) {
            colNames.add(tSpec.getColumnSpec(c).getName());
        }
        m_colCombo = new ColumnSelectionComboxBox(
                (Border)null, DataValue.class);
        m_colCombo.update(tSpec, null);
        m_colCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                selectedColChanged();
            }
        });

        /* the selectors for what kind of checking will be done */
        m_useRange = new JRadioButton("use range checking");
        m_useRegExpr = new JRadioButton("use regular expr. pattern matching");
        m_useMissValue = new JRadioButton("only missing values match");
        m_useRange.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                radiosChanged();
            }
        });
        m_useRegExpr.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                radiosChanged();
            }
        });
        m_useMissValue.addItemListener(new ItemListener() {
            public void itemStateChanged(final ItemEvent e) {
                radiosChanged();
            }
        });

        m_radios = new ButtonGroup();
        m_radios.add(m_useRange);
        m_radios.add(m_useRegExpr);
        m_radios.add(m_useMissValue);
        /* the bound edit fields */
        m_lowerLabel = new JLabel("lower bound:");
        m_lowerBound = new JTextField();
        m_upperLabel = new JLabel("upper bound:");
        m_upperBound = new JTextField();
        m_lowerBound.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(final DocumentEvent e) {
                boundsChanged();
            }

            public void removeUpdate(final DocumentEvent e) {
                boundsChanged();
            }

            public void changedUpdate(final DocumentEvent e) {
                boundsChanged();
            }
        });
        m_upperBound.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(final DocumentEvent e) {
                boundsChanged();
            }

            public void removeUpdate(final DocumentEvent e) {
                boundsChanged();
            }

            public void changedUpdate(final DocumentEvent e) {
                boundsChanged();
            }
        });
        /* the regular expression stuff */
        m_regLabel = new JLabel("regular expression:");
        m_regExpr = new JTextField();
        m_regExpr.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(final DocumentEvent e) {
                regExprChanged();
            }

            public void removeUpdate(final DocumentEvent e) {
                regExprChanged();
            }

            public void changedUpdate(final DocumentEvent e) {
                regExprChanged();
            }
        });
        m_caseSensitive = new JCheckBox("case sensitive match");

        /* and a label to display errors/warnings */
        m_errText = new JLabel("");
        setErrMsg("");
        m_errText.setForeground(Color.RED);

        /* set the default values */
        m_useRegExpr.setSelected(true);
        if (tSpec == null) {
            // no table spec no range. Sorry.
            m_useRange.setEnabled(false);
            m_upperBound.setEnabled(false);
            m_lowerBound.setEnabled(false);
            setErrMsg("configure (or execute) predecessor node"
                    + " to enable range checking");
        }
    }

    /**
     * Called when user pushes the buttons.
     */
    protected void radiosChanged() {

        m_upperLabel.setEnabled(m_useRange.isSelected());
        m_upperBound.setEnabled(m_useRange.isSelected());
        m_lowerLabel.setEnabled(m_useRange.isSelected());
        m_lowerBound.setEnabled(m_useRange.isSelected());

        m_regLabel.setEnabled(m_useRegExpr.isSelected());
        m_regExpr.setEnabled(m_useRegExpr.isSelected());
        m_caseSensitive.setEnabled(m_useRegExpr.isSelected());

        // have the err text updated
        if (m_useMissValue.isSelected()) {
            setErrMsg("");
            validate();
        } else if (m_useRange.isSelected()) {
            boundsChanged();
        } else {
            regExprChanged();
        }

    }

    /**
     * Called when user changes the values for the lower or upper bounds.
     */
    protected void boundsChanged() {
        // check if the entered value somehow goes along with the selected col.
        setErrMsg("");
        validate();
        if (m_tSpec == null) {
            return;
        }
        if (getSelectedColumnName() == null) {
            return;
        }
        if (!m_useRange.isSelected()) {
            return;
        }
        DataCell lowBound = null;
        DataCell hiBound = null;
        try {
            lowBound = getLowerBoundCell();
            hiBound = getUpperBoundCell();
        } catch (InvalidSettingsException ise) {
            setErrMsg(ise.getMessage());
            validate();
            return;
        }
        if ((lowBound == null) && (hiBound == null)) {
            setErrMsg("Specify at least one range boundary");
            validate();
            return;
        }
        if ((lowBound != null) && (hiBound != null)) {
            DataValueComparator comp;
            comp = DataType.getCommonSuperType(lowBound.getType(),
                    hiBound.getType()).getComparator();
            if (comp.compare(hiBound, lowBound) == -1) {
                setErrMsg("The lower bound must be smaller than the"
                        + " upper bound");
                validate();
                return;
            }
        }

        if (((lowBound != null) && (lowBound instanceof StringCell))
                || ((hiBound != null) && (hiBound instanceof StringCell))) {
            setErrMsg("Warning: String comparison is used for "
                    + "range checking. May not work as expected!");
            validate();
        }
    }

    /**
     * Called when the user selects a new column.
     */
    protected void selectedColChanged() {
        // we trigger 'boundsChanged' to get bounds checked against the new
        // column type
        boundsChanged();
    }

    /**
     * Called when the user changes the regular expression.
     */
    protected void regExprChanged() {
        setErrMsg("");
        if (m_regExpr.getText().length() <= 0) {
            setErrMsg("Enter valid regular expression");
            validate();
            return;
        }
        try {
            Pattern.compile(m_regExpr.getText());
        } catch (PatternSyntaxException pse) {
            setErrMsg("Error in regular expression. ('" + pse.getMessage()
                    + "')");
            validate();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void loadSettingsFromFilter(final RowFilter filter)
            throws InvalidSettingsException {
        if (!(filter instanceof ColValRowFilter)) {
            throw new InvalidSettingsException("ColVal filter panel can only "
                    + "load settings from a ColValRowFilter");
        }

        ColValRowFilter colFilter = (ColValRowFilter)filter;
        String colName = colFilter.getColumnName();
        if (colName != null) {
            m_colCombo.setSelectedColumn(colName);
        }
        if (colFilter.getFilterMissingValues()) {
            m_useMissValue.setSelected(true);
        } else if (colFilter.rangeSet()) {
            String upper = "";
            String lower = "";
            if (colFilter.getUpperBound() != null) {
                upper = colFilter.getUpperBound().toString();
            }
            if (colFilter.getLowerBound() != null) {
                lower = colFilter.getLowerBound().toString();
            }
            m_upperBound.setText(upper);
            m_lowerBound.setText(lower);
            if (m_useRange.isEnabled()) {
                m_useRange.setSelected(true);
            }
        } else {
            m_useRegExpr.setSelected(true);
            m_regExpr.setText(colFilter.getRegExpr());
            m_caseSensitive.setSelected(colFilter.caseSensitiveMatch());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RowFilter createFilter(final boolean include)
            throws InvalidSettingsException {
        if (hasErrors()) {
            throw new InvalidSettingsException(getErrMsg());
        }

        String colName = getSelectedColumnName();
        if ((colName == null) || (colName.length() == 0)) {
            setErrMsg("Select a valid column");
            validate();
            throw new InvalidSettingsException(getErrMsg());
        }

        if (m_useRange.isSelected()) {
            DataCell loBound = getLowerBoundCell();
            DataCell hiBound = getUpperBoundCell();
            if ((loBound == null) && (hiBound == null)) {
                setErrMsg("Enter at least one valid range boundary");
                validate();
                throw new InvalidSettingsException(getErrMsg());
            }

            return new ColValRowFilter(m_tSpec.getColumnSpec(colName).getType()
                    .getComparator(), loBound, hiBound, colName, include);
        }

        if (m_useRegExpr.isSelected()) {
            if (m_regExpr.getText().length() <= 0) {
                setErrMsg("Enter a valid regular expression");
                validate();
                throw new InvalidSettingsException(getErrMsg());
            }
            return new ColValRowFilter(m_regExpr.getText(), colName, include,
                    m_caseSensitive.isSelected(), false);

        }

        if (m_useMissValue.isSelected()) {
            return new ColValRowFilter(colName, include);
        }
        
        throw new InvalidSettingsException("Internal Error. "
                + "Please change some setting and try again. Sorry");
    }

    /**
     * @return the selected name of the column to test.
     */
    private String getSelectedColumnName() {

        return m_colCombo.getSelectedColumn();
    }

    /*
     * returns a DataCell of the entered value in the upper bound field. Will
     * return null if no text is entered in the corresponding edit field, if no
     * valid column index is set, if the entered value is not valid (with
     * respect to the selected column and its type - if a tablespec is set). In
     * the latter case the errText field is updated. If no table spec is set it
     * will create the most specific cell that can hold the value (IntCell,
     * DoubleCell, or finally StringCell)
     * 
     * @return a DataCell of the entered value in the upper bound field or null
     * if a problem occurred.
     */
    private DataCell getUpperBoundCell() throws InvalidSettingsException {
        return getBoundCell(m_upperBound, "upper");
    }

    /*
     * returns a DataCell of the entered value in the lower bound field. Will
     * return null if no text is entered in the corresponding edit field. Will
     * throw an exception if no valid column index is set, if the entered value
     * is not valid (with respect to the selected column and its type - if a
     * tablespec is set). It will not update the errText field. If no table spec
     * is set it will create the most specific cell that can hold the value
     * (IntCell, DoubleCell, or finally StringCell)
     * 
     * @return a DataCell of the entered value in the lower bound field or no
     * text was entered.
     */
    private DataCell getLowerBoundCell() throws InvalidSettingsException {
        return getBoundCell(m_lowerBound, "lower");
    }

    /* method used from the above */
    private DataCell getBoundCell(final JTextField editField, final String name)
            throws InvalidSettingsException {
        if (editField.getText().length() <= 0) {
            return null;
        }
        String colName = getSelectedColumnName();
        if ((colName == null) || (colName.length() == 0)) {
            throw new InvalidSettingsException("Invalid columns selection");
        }

        if (m_tSpec != null) {
            DataColumnSpec cSpec = m_tSpec.getColumnSpec(colName);
            DataType cType = cSpec.getType();

            if (cType.isCompatible(IntValue.class)) {

                // first try making of an IntCell
                try {
                    int lb = Integer.parseInt(editField.getText());
                    return new IntCell(lb);
                } catch (NumberFormatException nfe) {
                    throw new InvalidSettingsException(
                            "Number format error in " + name
                                    + " bound number: Enter valid integer.");
                }
            } else if (cType.isCompatible(DoubleValue.class)) {
                try {
                    double lb = Double.parseDouble(editField.getText());
                    return new DoubleCell(lb);
                } catch (NumberFormatException nfe) {
                    throw new InvalidSettingsException(
                            "Number format error in " + name
                                    + " bound number: enter a valid "
                                    + "float number");
                }
            } else {
                return new StringCell(editField.getText());
            }
        } else {
            // if we got no column type
            return new StringCell(editField.getText());
        }

    }

    /**
     * @return true if the settings in the panel are invalid, false if they are
     *         consistent and usable.
     */
    public boolean hasErrors() {
        if (m_errText.getText().length() <= 0) {
            return false;
        }
        if (m_errText.getText().substring(0, 7).equalsIgnoreCase("WARNING")) {
            return false;
        }
        return true;
    }

    /**
     * @return a message to the user if hasErrors returns true
     */
    public String getErrMsg() {
        return m_errText.getText();
    }

    private void setErrMsg(final String msg) {
        m_errText.setText(msg);
        m_errText.setToolTipText(msg);
    }
}
