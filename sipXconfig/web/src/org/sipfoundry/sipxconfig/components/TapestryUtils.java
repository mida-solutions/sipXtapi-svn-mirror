/*
 * 
 * 
 * Copyright (C) 2004 SIPfoundry Inc.
 * Licensed by SIPfoundry under the LGPL license.
 * 
 * Copyright (C) 2004 Pingtel Corp.
 * Licensed to SIPfoundry under a Contributor Agreement.
 * 
 * $
 */
package org.sipfoundry.sipxconfig.components;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.hivemind.Messages;
import org.apache.tapestry.AbstractPage;
import org.apache.tapestry.IComponent;
import org.apache.tapestry.IRequestCycle;
import org.apache.tapestry.PageRedirectException;
import org.apache.tapestry.contrib.table.model.IAdvancedTableColumn;
import org.apache.tapestry.contrib.table.model.ITableColumn;
import org.apache.tapestry.contrib.table.model.ognl.ExpressionTableColumn;
import org.apache.tapestry.services.ExpressionEvaluator;
import org.apache.tapestry.valid.IValidationDelegate;
import org.apache.tapestry.valid.ValidatorException;
import org.sipfoundry.sipxconfig.common.NamedObject;

/**
 * Utility method for tapestry pages and components
 * 
 * DEVELOPERS NOTE: Static utility methods have their place but consider adding methods to
 * TapestryContext to avoid too many static methods and the limitations they incur
 */
public final class TapestryUtils {
    /**
     * Standard name for form validation delegate
     */
    public static final String VALIDATOR = "validator";

    /**
     * restrict construction
     */
    private TapestryUtils() {
        // intentionally empty
    }

    /**
     * Tapestry4 does not provide a way of detecting if message has been provided. This method is
     * trying to guess if message has been retrieved or if Tapestry is trying to help by providing
     * a formatted key as a message.
     * 
     * @param defaultValue value that should be used if key is not found
     * @return found message or default value if key not found
     */
    public static final String getMessage(Messages messages, String key, String defaultValue) {
        String candidate = messages.getMessage(key);
        if (candidate.equalsIgnoreCase("[" + key + "]")) {
            return defaultValue;
        }
        return candidate;
    }

    /**
     * Utility method to provide more descriptive unchecked exceptions for unmarshalling object
     * from Tapestry service Parameters.
     * 
     * Please note that in most cases it is better to use listeners parameters directly.
     * 
     * @throws IllegalArgumentException if parameter is not there is wrong class type
     */
    public static final Object assertParameter(Class expectedClass, Object[] params, int index) {
        if (params == null || params.length < index) {
            throw new IllegalArgumentException("Missing parameter at position " + index);
        }

        if (params[index] != null) {
            Class actualClass = params[index].getClass();
            if (!expectedClass.isAssignableFrom(actualClass)) {
                throw new IllegalArgumentException("Object of class type "
                        + expectedClass.getName() + " was expected at position " + index
                        + " but class type was " + actualClass.getName());
            }
        }

        return params[index];
    }

    /**
     * Specialized version of assertParameter. Extracts beanId (Integer) from the first (index 0)
     * parameter of the cycle service
     * 
     * @param cycle current request cycle
     * @return bean id - exception is thrown if no id found
     */
    public static Integer getBeanId(IRequestCycle cycle) {
        return (Integer) assertParameter(Integer.class, cycle.getListenerParameters(), 0);
    }

    /**
     * Helper method to display standard "nice" stale link message
     * 
     * @param page page on which stale link is discovered
     * @param validatorName name of the validator delegate bean used to record validation errors
     */
    public static void staleLinkDetected(AbstractPage page, String validatorName) {
        IValidationDelegate validator = (IValidationDelegate) page.getBeans().getBean(
                validatorName);
        validator.setFormComponent(null);
        validator.record(new ValidatorException("The page is out of date. Please refresh."));
        throw new PageRedirectException(page);
    }

    /**
     * Helper method to display standard "nice" stale link message. Use only if standard
     * "validator" name has been used.
     * 
     * @param page page on which stale link is discovered
     */
    public static void staleLinkDetected(AbstractPage page) {
        staleLinkDetected(page, VALIDATOR);
    }

    /**
     * Check if there are any validation errors on the page. Use only if standard "validator" name
     * has been used.
     * 
     * Please note: this will only work properly if called after all components had a chance to
     * append register validation errors. Do not use in submit listeners other than form submit
     * listener.
     * 
     * @param page
     * @return true if no errors found
     */
    public static boolean isValid(AbstractPage page) {
        IValidationDelegate validator = getValidator(page);
        return !validator.getHasErrors();
    }

    /**
     * Retrieves the validator for the current page. Use only if standard "validator" name has
     * been used.
     * 
     * Use to record errors not related to any specific component.
     * 
     * @param page
     * @return validation delegate component
     */    
    public static IValidationDelegate getValidator(IComponent page) {
        IValidationDelegate validator = (IValidationDelegate) page.getBeans().getBean(VALIDATOR);
        return validator;
    }

    public static void recordSuccess(IComponent page, String msg) {
        IValidationDelegate delegate = getValidator(page);
        if (delegate instanceof SipxValidationDelegate) {
            SipxValidationDelegate validator = (SipxValidationDelegate) delegate;
            validator.recordSuccess(msg);
        }
    }

    /**
     * Convience method that will quietly localize and act graceful if resources are not set or
     * found.
     */
    public static final String localize(Messages messages, String key) {
        return localize(messages, null, key);
    }

    /**
     * @param prefix prepended to key w/o '.' (e.g. fullkey = prefix + key)
     */
    public static final String localize(Messages messages, String prefix, String key) {
        if (key == null || messages == null) {
            return key;
        }

        String fullKey = prefix != null ? prefix + key : key;
        String label = messages.getMessage(fullKey);

        return label;
    }

    /**
     * Creates column model that can be used to display dates
     * 
     * @param columnName used as internal column name, user visible name (through getMessage), and
     *        OGNL expression to calculate the date
     * @param messages used to retrieve column title
     * @return newly create column model
     */
    public static ITableColumn createDateColumn(String columnName, Messages messages,
            ExpressionEvaluator expressionEvaluator) {
        String columnTitle = messages.getMessage(columnName);
        IAdvancedTableColumn column = new ExpressionTableColumn(columnName, columnTitle,
                columnName, true, expressionEvaluator);
        column.setValueRendererSource(new DateTableRendererSource());
        return column;
    }
    
    /**
     * For auto completetion of space delimited fields. Collection is represented named
     * 
     * @param namedItems collection of objects that implement NamedItem
     * @param currentValue what user have entered so far including
     * @return collection of Strings of possible auto completed items
     */
    public static Collection getAutoCompleteCandidates(Collection namedItems, String currentValue) {
        String targetGroup;
        String prefix;
        if (StringUtils.isBlank(currentValue)) {
            targetGroup = null;
            prefix = "";
        } else if (currentValue.endsWith(" ")) {
            targetGroup = null;
            prefix = currentValue;            
        } else {
            String[] groups = currentValue.split("\\s+");
            int ignore = groups.length - 1;
            targetGroup = groups[ignore].toLowerCase();            
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < ignore; i++) {
                sb.append(groups[i]).append(' ');
            }
            prefix = sb.toString();
        }
        
        List candidates = new ArrayList();
        for (Iterator i = namedItems.iterator(); i.hasNext();) {
            NamedObject candidate = (NamedObject) i.next();
            String candidateName = candidate.getName();
            // candidates suggestions are case insensitve, doesn't mean groups are
            // though
            if (targetGroup == null || candidateName.toLowerCase().startsWith(targetGroup)) {
                // do not list items twice
                if (prefix.indexOf(candidateName + ' ') < 0) {
                    candidates.add(prefix + candidateName);
                }
            }               
        }
        
        return candidates;        
    }
}
