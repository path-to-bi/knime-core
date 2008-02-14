/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright, 2003 - 2008
 * University of Konstanz, Germany.
 * Chair for Bioinformatics and Information Mining
 * Prof. Dr. Michael R. Berthold
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any quesions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * --------------------------------------------------------------------- *
 * 
 * History
 *   15.03.2007 (berthold): created
 */
package org.knime.core.node.workflow;

/**
 * 
 * @author berthold, University of Konstanz
 */
abstract class ScopeObject {

    private NodeID m_originatingNode;
    
    void setOriginatingNode(final NodeID on) {
        m_originatingNode = on;
    }
    
    public NodeID getOriginatingNode() {
        return m_originatingNode;
    }
    
}
